# Impact Analysis
Impact overview of the MATSim run.

## Methodology and calculation methods

The analysis reads the standard MATSim `trips.csv` and `legs.csv` outputs. Values are aggregated by mode and reported for the represented simulation day and as annualized values.

### Traffic and physical effects

- **Vehicle Volume** is the number of vehicle legs from `legs.csv` whose `network_mode` is one of the configured vehicle modes (`car`, `freight`, or `truck`). The daily value is the aggregated leg count. The annual value is `daily vehicle volume × annualization factor`.
- **Vehicle Operating Times (≤ 50 km)** sums the travel time of vehicle legs with a leg distance of at most 50 km.
- **Vehicle Operating Times (> 50 km)** sums the travel time of vehicle legs with a leg distance greater than 50 km.
- **Vehicle Operating Times** is the sum of the two distance classes.
- **Travel Distance** sums vehicle-leg distances and converts metres to kilometres. Annual values are multiplied by the annualization factor and expressed in million kilometres where indicated.

Passenger trips, passenger-kilometres and passenger travel time are read from `trips.csv`. Passenger travel time is converted from seconds to hours. Freight traffic uses the same trip-level calculations but is annualized with the freight factor.

### Emissions

Emission values are read from `analysis/emissions/emissions_per_network_mode.csv` when available. Daily values are converted to kg; annual values use the same mode-specific annualization factor and are converted to tonnes.

### Scores and agent comparison

Scores are read from `persons.csv` using `executed_score`. The daily score sum and scored-person count are annualized with the passenger traffic factor for the per-year table; the mean score remains a utility per person. Agent comparisons match person identifiers between Base Case and Policy Case and compare common persons, remainers, switchers, and travel-time/score changes.

### Base Case, Policy Case and Difference

For every comparable value, **Diff = Base Case − Policy Case**. A missing Base Case value remains blank in the Base column and is not replaced by zero. Annualization factors are 334 days for passenger transport and 302 days for freight transport by default.

## Java classes and data flow

The implementation separates reading, aggregation, calculation and presentation. The classes are connected as follows:

`ImpactAnalysis` → `ImpactRunReader` → `ImpactAnalysisResult` → `ImpactCsvWriter` → `impact.csv` → `ImpactDashboardTables` → SimWrapper tables

- **`ImpactAnalysis`** is the command-line entry point. It reads the policy run and, when supplied, the Base Case directory. It resolves `trips.csv`, `legs.csv`, `persons.csv` and the optional emissions output, applies sample scaling, and starts the calculation.
- **`ImpactRunReader`** streams the MATSim CSV files. It aggregates trip metrics by main mode from `trips.csv`, vehicle-leg volume, distance and operating times from `legs.csv`, scores from `persons.csv`, and emissions by mode and pollutant. Vehicle operating times are split at the 50 km threshold. Both `network_mode` and the standard `mode` column are supported for legs.
- **`ImpactAnalysisResult`** is the in-memory aggregation model. It stores per-mode totals, pollutant totals, person-level scores and travel times, and the counters needed for agent comparisons. It does not perform any visualization.
- **`ImpactCsvWriter`** converts the aggregated results into the stable long-format `impact.csv`. It performs unit conversions, annualization, Base Case minus Policy Case differences, relative changes, score totals and score annualization. Missing reference values are kept empty rather than interpreted as zero.
- **`ImpactComparison`** compares person identifiers between the two runs and derives common persons, remainers, switchers, and score/travel-time differences.
- **`ImpactDashboardTables`** is the presentation adapter. It reads `impact.csv` and creates the small SimWrapper-ready CSV files for each mode and period (`day` and `year`), including the score tables. It only reshapes and formats data; it does not recalculate impacts.
- **`ImpactAnalysisDashboard`** defines the SimWrapper layout. It requests the impact analysis and display-table commands, places the per-day and per-year physical/emission tables side by side for each mode, and adds the score tables below.
- **`BvwpAnalysis`** is the separate, more extensive BVWP-oriented analysis for monetized benefits, present values and benefit-cost indicators. It is not required for the basic physical impact tables described above.

The resulting files therefore have distinct roles: `impact.csv` is the calculation/reporting contract, the display CSVs are dashboard-specific views, and the YAML dashboard only describes their layout.

## Java-Klassen und Datenfluss (Deutsch)

Der Aufbau entspricht grundsätzlich dem Muster der bestehenden SimWrapper-Standard-Dashboards wie `TrafficDashboard`, `TripDashboard` und `EmissionsDashboard`:

| Aspekt | Bestehende Dashboards | `ImpactAnalysisDashboard` |
| --- | --- | --- |
| Dashboard-Klasse | Zum Beispiel `TrafficDashboard`, `TripDashboard`, `EmissionsDashboard` | `ImpactAnalysisDashboard` |
| Layout | Konfiguration über `configure(Header, Layout, SimWrapperConfigGroup)` | Gleiches Muster |
| Berechnung | `data.compute(AnalysisClass.class, output, args)` | `data.compute(ImpactAnalysis.class, "impact.csv", args)` |
| Datenquellen | Standard-MATSim-Ausgabedateien | `trips.csv`, `legs.csv`, `persons.csv` und optional Emissionsdaten |
| Ausgabe | Analyseklasse erzeugt CSV-Dateien | `ImpactAnalysis` erzeugt zunächst `impact.csv` |
| Darstellung | Tabellen, Diagramme oder Karten | `ImpactDashboardTables` erzeugt zusätzliche Anzeige-CSV-Dateien |
| Standardintegration | Einbindung über `DefaultDashboardProvider` | Ebenfalls über `DefaultDashboardProvider` eingebunden |
| Vergleichsfälle | Je nach Dashboard optional | Base Case und Policy Case werden explizit verglichen |

Der Datenfluss ist:

```text
MATSim-Lauf
    ↓
Standard-Ausgabedateien
    ↓
ImpactAnalysis
    ↓
impact.csv
    ↓
ImpactDashboardTables
    ↓
Anzeige-CSV-Dateien je Modus und Zeitraum
    ↓
ImpactAnalysisDashboard
    ↓
SimWrapper-YAML und Tabellen
```

Der wesentliche Unterschied zu einfacheren Standard-Dashboards ist die zweistufige Verarbeitung:

1. `ImpactAnalysis` liest und aggregiert die Daten und berechnet Einheiten, Jahreswerte, Base-Policy-Differenzen und Prozentänderungen.
2. `ImpactDashboardTables` formatiert die Ergebnisse in kleinere CSV-Dateien für die konkrete Dashboard-Darstellung, zum Beispiel getrennt nach Modus sowie Tag und Jahr.

Architektonisch bleibt das Verfahren dennoch im üblichen MATSim-/SimWrapper-Muster: Das Dashboard berechnet die Kennzahlen nicht selbst, sondern fordert die Analyseklassen über `data.compute(...)` an. Die zusätzliche Vergleichslogik ermöglicht außerdem die Spalten Base, Policy, Diff und Prozentänderung, die in den meisten anderen Standard-Dashboards nicht in dieser Form vorhanden sind.

`BvwpAnalysis` gehört zu einem separaten `BvwpDashboard`. Es wird nicht für das aktuelle `ImpactAnalysisDashboard` benötigt, sondern stellt die weiterführenden BVWP-Auswertungen wie monetarisierte Nutzen, Barwerte und Nutzen-Kosten-Indikatoren bereit.

## Verwendung mit einem fertigen MATSim-Lauf

Für einen bereits abgeschlossenen MATSim-Lauf benötigt die Analyse mindestens:

- `config.xml`
- `trips.csv` oder `trips.csv.gz`
- `legs.csv` oder `legs.csv.gz`
- `persons.csv` oder `persons.csv.gz`

Emissionsdaten aus `analysis/emissions/emissions_per_network_mode.csv` werden verwendet, wenn sie vorhanden sind. Für einen Vergleich wird zusätzlich das Verzeichnis des Base Case benötigt.

Wenn der MATSim-Libs-Quellcode ausgecheckt ist, kann das Dashboard nachträglich zum Beispiel so erzeugt werden:

```bash
mvn -pl contribs/simwrapper exec:java \
  -Dexec.mainClass=org.matsim.simwrapper.CreateSingleSimWrapperDashboard \
  -Dexec.args="--type impactAnalysis RUN_DIRECTORY"
```

Für einen Base-Case-/Policy-Vergleich wird das Base-Case-Verzeichnis ergänzt:

```bash
mvn -pl contribs/simwrapper exec:java \
  -Dexec.mainClass=org.matsim.simwrapper.CreateSingleSimWrapperDashboard \
  -Dexec.args="--type impactAnalysis --reference-run-directory BASE_CASE_DIRECTORY POLICY_CASE_DIRECTORY"
```

Dabei ist `POLICY_CASE_DIRECTORY` das Verzeichnis, in dem `dashboard-1.yaml` und die Analyse-CSV-Dateien erzeugt werden. Dieses Verzeichnis kann anschließend in SimWrapper geöffnet werden. Die Eingabedateien des Runs werden nicht verändert; es werden nur Analyse- und Dashboard-Dateien ergänzt.

## Automatische Einbindung in zukünftige Läufe

Für den späteren regulären Ablauf wird das SimWrapper-Modul in den MATSim-Controler eingebunden:

```java
controler.addOverridingModule(new SimWrapperModule());
```

Nach Abschluss der Simulation erzeugt SimWrapper die Standard-Dashboards einschließlich `ImpactAnalysisDashboard` automatisch. Die Analyse läuft dabei über `data.compute(...)`, sobald das Dashboard erzeugt wird. Ein manueller Nachbearbeitungsschritt ist dann nicht mehr nötig. Für einen Vergleich mit einem Base Case muss der Base-Case-Pfad über die SimWrapper-Konfiguration oder den Dashboard-Konstruktor bereitgestellt werden.

## Argumente und Pflichtangaben

### `ImpactAnalysis`

Bei einem direkten Aufruf sind die Eingabedateien und das Run-Verzeichnis erforderlich. Die Pfade können automatisch aus einem Run-Verzeichnis aufgelöst werden oder explizit angegeben werden:

| Argument | Erforderlich | Bedeutung | Standard |
| --- | --- | --- | --- |
| `--run-directory PATH` | Ja | Verzeichnis des MATSim-Runs | – |
| `--input-trips PATH` | Nein | `trips.csv` beziehungsweise `trips.csv.gz`; wird aus `--run-directory` aufgelöst | automatisch |
| `--input-legs PATH` | Nein | `legs.csv` beziehungsweise `legs.csv.gz`; wird aus `--run-directory` aufgelöst | automatisch |
| `--input-persons PATH` | Nein | `persons.csv` beziehungsweise `persons.csv.gz`; wird aus `--run-directory` aufgelöst | automatisch |
| `--sample-size FRACTION` | Nein | Simulierter Stichprobenanteil, zum Beispiel `0.1` für 10 % | `1.0` |
| `--reference-run-directory PATH` | Nein | Base-Case-Verzeichnis für den Vergleich | kein Base Case |
| `--reference-sample-size FRACTION` | Nein | Stichprobenanteil des Base Case | Policy-Stichprobenanteil |
| `--input-emissions-per-network-mode PATH` | Nein | Emissionsdatei je Netzwerkmodus | automatische Suche |
| `--modes MODE1,MODE2` | Nein | Zu analysierende Personenverkehrsmodi | Modi aus `trips.csv` |
| `--vehicle-modes MODE1,MODE2` | Nein | Modi für Fahrzeugkennzahlen | `car,freight,truck` |
| `--freight-modes MODE1,MODE2` | Nein | Modi, die als Güterverkehr gelten | `freight,truck` |
| `--person-traffic-days-per-year NUMBER` | Nein | Annualisierungsfaktor Personenverkehr | `334` |
| `--freight-traffic-days-per-year NUMBER` | Nein | Annualisierungsfaktor Güterverkehr | `302` |
| `--output-impact PATH` | Nein | Zieldatei der Analyse | `impact.csv` |

Beim Aufruf über SimWrapper werden die Eingabepfade, `--sample-size` und die Ausgabepfade automatisch aus dem Run-Kontext beziehungsweise der Dashboard-Konfiguration gesetzt. Deshalb muss ein normaler Nutzer beim Dashboard-Aufruf in der Regel nur das Run-Verzeichnis angeben.

### `CreateSingleSimWrapperDashboard`

Für die nachträgliche Dashboard-Erzeugung gelten diese Argumente:

| Argument | Erforderlich | Bedeutung |
| --- | --- | --- |
| `--type impactAnalysis` | Ja | Dashboard-Typ |
| `RUN_DIRECTORY` | Ja | Policy-/Szenario-Run; mindestens ein Positionsargument |
| `--reference-run-directory BASE_CASE_DIRECTORY` | Nein | Base Case für den Vergleich |
| `--shp PATH` | Nein | Optionaler Shapefile-Pfad für Dashboard-Varianten, die Geometrien benötigen |

Beispiel ohne Vergleich:

```bash
mvn -pl contribs/simwrapper exec:java \
  -Dexec.mainClass=org.matsim.simwrapper.CreateSingleSimWrapperDashboard \
  -Dexec.args="--type impactAnalysis POLICY_CASE_DIRECTORY"
```

Beispiel mit Base Case:

```bash
mvn -pl contribs/simwrapper exec:java \
  -Dexec.mainClass=org.matsim.simwrapper.CreateSingleSimWrapperDashboard \
  -Dexec.args="--type impactAnalysis \
    --reference-run-directory BASE_CASE_DIRECTORY \
    POLICY_CASE_DIRECTORY"
```

Die Namen der Eingabe- und Ausgabeoptionen werden durch die MATSim-Optionen `InputOptions` und `OutputOptions` aus der `@CommandSpec`-Definition erzeugt. Daher können zusätzlich die allgemeinen `--help`-Optionen verwendet werden, um die für die installierte Version verfügbaren Aliasnamen anzuzeigen.
