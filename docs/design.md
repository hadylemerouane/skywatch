# SkyWatch – Design

This document records how SkyWatch is built and why. Each decision answers a requirement from the [specification](specification.md).

## 1. Architecture

```
          OpenSky / adsb.lol (real ADS-B data)
                       │ HTTPS, every 5 minutes
                       ▼
   ┌─────────────┐  POST   ┌──────────────────────┐  SQL   ┌────────────┐
   │ collector   │ ──────▶ │ api                  │ ─────▶ │ PostgreSQL │
   │ Python      │         │ Java / Spring Boot   │        └────────────┘
   └─────────────┘         │ detects conflicts    │
                           │ every 10 seconds     │
                           └──────────────────────┘
                                      ▲ GET
   Browser ──HTTP──▶ ┌────────────────┐
                     │ web (nginx)    │
                     │ map + proxy    │
                     └────────────────┘
```

| Service | Responsibility |
| --- | --- |
| collector | Fetches aircraft positions from the data source, converts them to aviation units and sends them to the API. |
| api | The only service that accesses the database. Stores positions, computes conflicts every 10 seconds and exposes both. |
| web | Serves the map to the browser and forwards its `/api/` requests to the API. |

## 2. Design decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Split | 3 services: collector, api, web | Each has a single responsibility and can evolve, be deployed and restart independently. |
| Database access | Only through the API | A single owner of the data: the schema can change without breaking other services. |
| Collector language | Python | Calling an API and transforming JSON takes little code and reads clearly. |
| API language | Java with Spring Boot | Strong typing for the business logic, standard in industry, excellent testing tools. |
| Conflict computation | Every 10 s in the background, result cached (NFR-01) | Reading conflicts is instant, and the cost does not depend on the number of users. |
| Storage | PostgreSQL, one row per aircraft (FR-02) | Detection only needs the current state of each aircraft. |

Not done, on purpose:
- **No trajectory history**: not required by the specification.
- **No message queue between collector and API**: unnecessary at this volume, but the natural next step to decouple them further.

## 3. API contract

| Method and path | Purpose | Response |
| --- | --- | --- |
| `POST /api/positions` | Receive a batch of positions (from the collector) | `202 Accepted`, or `400 Bad Request` if a position is invalid |
| `GET /api/positions` | Aircraft seen in the last 10 minutes (FR-02) | `200 OK` with a JSON list |
| `GET /api/conflicts` | Latest detected conflicts (FR-07) | `200 OK` with a JSON list |
| `GET :9090/actuator/health` | Health check, internal port only | `200 OK` |
| `GET :9090/actuator/prometheus` | Metrics, internal port only | `200 OK` |

A position:

```json
{
  "icao24": "3c6444",
  "callsign": "AFR1234",
  "latitude": 43.63,
  "longitude": 1.37,
  "altitudeFt": 35000,
  "groundSpeedKt": 450,
  "trackDeg": 90,
  "verticalRateFpm": 0
}
```

A conflict:

```json
{
  "icao24A": "3c6444",
  "callsignA": "AFR1234",
  "icao24B": "39856a",
  "callsignB": "EZY56AB",
  "horizontalNm": 2.2,
  "verticalFt": 500,
  "type": "PREDICTED",
  "secondsToCpa": 95
}
```

`type` is `CURRENT` or `PREDICTED`. `secondsToCpa` is the time until the closest point of approach (0 for a current conflict).

## 4. Data model

```sql
CREATE TABLE positions (
  icao24            VARCHAR(6) PRIMARY KEY,
  callsign          VARCHAR(10),
  latitude          DOUBLE PRECISION NOT NULL,
  longitude         DOUBLE PRECISION NOT NULL,
  altitude_ft       DOUBLE PRECISION NOT NULL,
  ground_speed_kt   DOUBLE PRECISION NOT NULL,
  track_deg         DOUBLE PRECISION NOT NULL,
  vertical_rate_fpm DOUBLE PRECISION NOT NULL,
  updated_at        TIMESTAMPTZ NOT NULL
);
```

Each new position replaces the previous one for the same aircraft ("upsert"), so the table holds at most one row per aircraft.

## 5. Java packages

```
fr.skywatch.api
├── model/        Position, Conflict, ConflictType     (plain data records)
├── detection/    GeoUtils, ConflictDetector           (the algorithm, no Spring)
├── repository/   PositionRepository                   (SQL access)
├── service/      ConflictService                      (periodic computation, cache, metrics)
└── web/          PositionController, ConflictController (REST API)
```

The key point: **the algorithm depends neither on Spring nor on the database.** It takes a list of positions and returns a list of conflicts, so it is easy to test (NFR-03) and to reuse.

## 6. Detection algorithm

Every pair of aircraft is checked once.

**Current conflict (FR-03).** Compute the great-circle distance between the two aircraft (haversine formula) and their altitude difference. It is a conflict if the distance is below 5 NM **and** the altitude difference is below 1,000 ft.

**Predicted conflict (FR-04): closest point of approach (CPA).** Work on a flat local plane centred on aircraft A, in nautical miles. The position of B relative to A (x east, y north):

```math
x = \Delta\lambda \cdot 60 \cdot \cos\bar{\varphi}, \qquad y = \Delta\varphi \cdot 60
```

where Δλ and Δφ are the longitude and latitude differences in degrees and φ̄ the mean latitude (one degree of latitude is 60 NM). The velocity of each aircraft, in NM per second, from its ground speed V in knots and its track θ:

```math
\vec{v} = \frac{V}{3600}\,(\sin\theta,\ \cos\theta)
```

With the relative position r and the relative velocity v (B's minus A's), the distance at time t is |r + v·t|. It is minimal at:

```math
t^{*} = -\frac{\vec{r}\cdot\vec{v}}{\lVert\vec{v}\rVert^{2}}
```

t* is bounded between 0 and the horizon (300 s). If the distance at t* is below 5 NM and the altitude difference at t* (extrapolating vertical rates) is below 1,000 ft, it is a predicted conflict, in t* seconds.

## 7. Known limitations

- **Straight-line trajectories**: a turning or climbing aircraft is not well predicted.
- **Vertical check only at t\***: two aircraft could be vertically close before or after. Improvement: check the whole time interval during which they are horizontally too close.
- **Flat local plane**: very accurate over tens of NM, less over long distances; acceptable since only nearby aircraft matter.
- **Quadratic complexity**: n(n−1)/2 comparisons, about 500,000 for 1,000 aircraft, still fast (NFR-02). Improvement: a spatial grid that only compares aircraft in neighbouring cells.
