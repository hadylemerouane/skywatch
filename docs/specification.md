# SkyWatch – Specification

## 1. Context
Aircraft equipped with ADS-B determine their own position using GPS and broadcast it by radio about twice per second, together with their identifier, altitude, speed and heading.
Unlike radar, which measures positions from the ground, ADS-B positions are self-reported, which makes them more precise, more frequent and cheaper to collect.
To keep flights safe, air traffic controllers maintain a minimum distance between aircraft, called separation. When two aircraft are too close both horizontally and vertically at the same time, a loss of separation occurs.
SkyWatch collects real ADS-B data over south-western France, detects current losses of separation and predicts upcoming ones, as a simplified version of the Short-Term Conflict Alert (STCA) tools used in air traffic control centres.
The default thresholds (5 NM and 1,000 ft) follow ICAO radar standards (Doc 4444); real minima vary with the airspace and the phase of flight, and are simplified here.

## 2. Glossary
| Term | Definition |
| --- | --- |
| ADS-B | Automatic Dependent Surveillance–Broadcast: a technology by which an aircraft broadcasts its GPS position, identity, altitude, speed and heading to ground stations and other aircraft. |
| ICAO24 | The unique 24-bit address assigned to each aircraft by its state of registry, written as 6 hexadecimal characters (e.g. `3c6444`). It identifies the aircraft, not the flight. |
| Callsign | The flight identifier used in radio communications, usually the airline's 3-letter ICAO code followed by the flight number (e.g. `AFR1234`). It changes from one flight to another. |
| Separation | The minimum distance that air traffic control keeps between two aircraft, horizontally or vertically. Two aircraft are separated if at least one of the two minima is respected. |
| Current conflict | A loss of separation that is happening now: both the horizontal and the vertical minima are infringed at the same time. |
| Predicted conflict | A loss of separation that will occur within the prediction horizon if both aircraft keep their current speed and heading. |
| NM, ft, kt | Aviation units: 1 nautical mile (NM) = 1,852 m; 1 foot (ft) = 0.3048 m; 1 knot (kt) = 1 NM per hour. |

## 3. Functional requirements
| Ref. | Requirement |
| --- | --- |
| FR-01 | The system periodically retrieves the positions of airborne aircraft within a configurable geographic area. |
| FR-02 | The system keeps the last known position of each aircraft and ignores positions older than 10 minutes. |
| FR-03 | The system detects current losses of separation between two aircraft. |
| FR-04 | The system predicts losses of separation within a configurable horizon (5 minutes by default), assuming constant speed and heading. |
| FR-05 | The horizontal and vertical separation thresholds are configurable (5 NM and 1,000 ft by default). |
| FR-06 | A map displays the aircraft and highlights current and predicted conflicts. |
| FR-07 | A REST API exposes positions and conflicts in JSON format. |

## 4. Non-functional requirements
| Ref. | Requirement |
| --- | --- |
| NFR-01 | Conflicts are recomputed at least every 10 seconds. |
| NFR-02 | Detection takes less than 1 second for 1,000 aircraft. |
| NFR-03 | The detection algorithm is covered by automated tests. |
| NFR-04 | The system respects the usage limits of its data source (OpenSky or adsb.lol). |
| NFR-05 | If the data source is unavailable, the system keeps running and logs the error. |
| NFR-06 | Communication between services is authenticated and encrypted (layer 3). |

## 5. Out of scope
SkyWatch is an educational project. It is neither certified nor intended for operational use.

## 6. References
- SKYbrary, *Automatic Dependent Surveillance – Broadcast (ADS-B)*: https://skybrary.aero/articles/automatic-dependent-surveillance-broadcast-ads-b
- SKYbrary, *24-bit Aircraft Address*: https://skybrary.aero/articles/24-bit-aircraft-address
- SKYbrary, *Separation Standards*: https://skybrary.aero/articles/separation-standards
- SKYbrary, *Loss of Separation*: https://www.skybrary.aero/index.php/Loss_of_Separation
- Wikipedia, *Short-term conflict alert*: https://en.wikipedia.org/wiki/Short-term_conflict_alert
