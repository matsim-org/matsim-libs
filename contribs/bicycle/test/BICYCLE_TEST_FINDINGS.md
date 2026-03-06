# Bicycle Contrib Test Findings (cycleway -> bicycle_infra)

Datum: 2026-03-06
Branch: `bicycle-motorized-interaction`

## 1) Stabiler Zwischenstand

Aktueller Stand ist ein stabiler Zwischenzustand:

- Infrastruktur-/Konfigurationsfehler in `BicycleTest` sind aufgeloest.
- `BicycleTest` laeuft deterministisch durch und zeigt jetzt nur noch fachliche Regressionen.
- Aktueller Lauf: `Tests run: 11, Failures: 9, Errors: 0, Skipped: 0`.

Referenzlauf:

- `mvn -pl contribs/bicycle clean -DskipITs "-Dtest=org.matsim.contrib.bicycle.run.BicycleTest" test`

## 2) Entscheidungen (Runner vs. Tests)

### 2.1 Runner-Entscheidung

Ziel war: Runner-Semantik moeglichst nicht veraendern.

- `RunBicycleContribExample` wurde nur minimal-invasiv angepasst, damit `BicycleTest` ihn aufrufen kann.
- Keine weitere testgetriebene Fachlogik in den Runner verlagern.
- `RunBicycleExample` bleibt Legacy und ist als deprecated markiert.

Betroffene Dateien:

- `contribs/bicycle/src/main/java/org/matsim/contrib/bicycle/run/RunBicycleContribExample.java`
- `contribs/bicycle/src/main/java/org/matsim/contrib/bicycle/run/RunBicycleExample.java`

### 2.2 Test-Entscheidung

Fehlende Test-Config wird testseitig gefixt (nicht im Runner):

- In `BicycleTest` wurde ein Helper `ensureBicycleModeParams(config)` eingefuehrt.
- Der Helper stellt sicher:
  - Replanning-Strategien vorhanden (`ChangeExpBeta`, `ReRoute`)
  - ActivityParams vorhanden (`home`, `work`)
  - `ModeParams("bicycle")` vorhanden

Warum noetig:

- Default-Config enthaelt typischerweise `bike`, aber nicht automatisch `bicycle`.
- Ohne `ModeParams("bicycle")` entstanden vorher harte Laufzeitfehler (`ProvisionException`/`Mode bicycle is not part ...`).

Betroffene Datei:

- `contribs/bicycle/src/test/java/org/matsim/contrib/bicycle/run/BicycleTest.java`

## 3) Bereits erledigte API-Migrationen

- `contribs/bicycle/src/test/java/org/matsim/contrib/bicycle/BicycleLinkSpeedCalculatorTest.java`
  - `BicycleUtils.CYCLEWAY` -> `BicycleUtils.BICYCLE_INFRA`
- `contribs/bicycle/src/test/java/org/matsim/contrib/bicycle/BicycleParamsDefaultImplTest.java`
  - Entfernt: `BicycleUtils.CYCLEWAY`, `BicycleUtils.getCyclewaytype(...)`
  - Infrastruktur-Assertions auf neue `bicycle_infra`-Semantik umgestellt

Status dieser beiden Klassen:

- `BicycleLinkSpeedCalculatorTest`: PASS
- `BicycleParamsDefaultImplTest`: PASS

## 4) Aktuelles Fehlerbild in `BicycleTest`

Quelle:

- `contribs/bicycle/target/surefire-reports/TEST-org.matsim.contrib.bicycle.run.BicycleTest.xml`

Status je Test:

| Test | Status | Hauptgrund |
|---|---|---|
| `testInfrastructureSpeedFactorDistanceMoreRelevantThanTravelTime` | PASS | - |
| `testInfrastructureSpeedFactor` | PASS | - |
| `testNormal` | FAIL | Events differ (`MISSING_EVENT`) |
| `testNormal10It` | FAIL | Events differ (`MISSING_EVENT`) |
| `testLane` | FAIL | Events differ (`MISSING_EVENT`) |
| `testGradient` | FAIL | Events differ (`MISSING_EVENT`) |
| `testGradientLane` | FAIL | Events differ (`MISSING_EVENT`) |
| `testCobblestone` | FAIL | Population differs |
| `testPedestrian` | FAIL | Events differ (`MISSING_EVENT`) |
| `testLinkBasedScoring` | FAIL | Score differs |
| `testLinkVsLegMotorizedScoring` | FAIL | Score differs |

Interpretation:

- Keine Infrastruktur-/Injector-Blocker mehr.
- Es bleiben reine Referenz-/Regressionsthemen.

## 5) Naechste TODOs fuer die 9 Fails

### TODO A: Referenz-Outputs neu erzeugen (ja, sehr wahrscheinlich noetig)

Fuer die failenden Szenarien sind die erwarteten Referenzdateien mit hoher Wahrscheinlichkeit veraltet und muessen an die aktuelle `bicycle`-Semantik angepasst werden:

- `output_events.xml.gz`
- `output_plans.xml.gz`

Zielpfade:

- Unter `contribs/bicycle/test/input/org/matsim/contrib/bicycle/run/BicycleTest/` und den relevanten Unterordnern.

### TODO B: Score-basierte Tests separat bewerten

Fuer:

- `testLinkBasedScoring`
- `testLinkVsLegMotorizedScoring`

vor Baseline-Uebernahme kurz plausibilisieren, ob die Score-Differenz fachlich erwartbar ist (Mode-Umstellung, Parameter, Routing-Unterschiede).

### TODO C: Danach Regression erneut laufen lassen

- `mvn -pl contribs/bicycle clean -DskipITs "-Dtest=org.matsim.contrib.bicycle.run.BicycleTest" test`
- Optional danach kompletter Modul-Lauf:
  - `mvn -pl contribs/bicycle -DskipITs test`

## 6) Offenes Nachthema (separat)

Semantische Input-Migration in XMLs:

- Alte Attribute `cycleway`/`cyclewaytype` in Testinputs auf `bicycle_infra` migrieren.
- Werte auf gueltige neue Kategorien mappen.

Das ist fachlich getrennt von der aktuellen Stabilisierung der `BicycleTest`-Regression.

