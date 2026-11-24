# Accessibility

This package provides a tool for accessibility calculation.

The code in this contribution relates mainly to the _ER Africa_ project titled _MAXess: Measuring accessibility in policy evaluation_. The project is a joint research project between the following entities:

* Prof Johan W. Joubert, University of Pretoria, South Africa (coordinator);
* Prof Kay W. Axhausen, Swiss Federal Institute of Technology (ETH Zurich);
* Prof Kai Nagel, Technical University Berlin; and
* Prof John Kiema and Dr David Siriba, University of Nairobi, Kenya

with the involvement of the following PhD students:

* Dominik Ziemke, Technical University Berlin;
* Thibaut Duberbet, Swiss Federal Institute of Technology;
* Sammy Matara, University of Nairobi; and
* Gerhard Hitge, University of Pretoria.

# Person-Based Accessibility Thesis Extension

This section documents the implementation and analysis used for the thesis:

**An Income Dependent Person-Based Accessibility Analysis of a Car-Free Zone in Berlin using MATSim**

**Author:** Ardias A. Tumiwa  
**Institution:** Technische Universität Berlin  
**Thesis:** Bachelor’s thesis  
**Year:** 2026

The workflow extends the MATSim accessibility calculation to support person-based accessibility at agents' home coordinates, includes an income-dependent car cost component, and compares a base scenario with a car-free policy scenario in Berlin.

---

## 1. Overview

The analysis consists of two main stages:

```text
MATSim Berlin scenario outputs
        |
        v
RunPersonBasedAccBerlin.java
        |
        |-- person-based accessibility by mode
        |-- base / policy scenario selection
        |-- rawSum / logSum selection
        |
        v
Mode-specific accessibility CSV files
        |
        v
analysis.R
        |
        |-- merge results by personId
        |-- calculate mode-specific logsums
        |-- calculate multimodal accessibility
        |-- calculate base-policy changes
        |-- assign Berlin districts
        |-- produce summary tables and plots
        |
        v
Final analysis outputs
```

For the thesis analysis, `rawSum` outputs from MATSim are used for walking, public transport and car. These are combined in R before applying the logarithm to calculate multimodal accessibility.

---

## 2. Main Java runner

The main run file is:

```text
contribs/accessibility/src/main/java/org/matsim/contrib/accessibility/run/RunPersonBasedAccBerlin.java
```

The main run settings are defined at the beginning of `main()`:

```java
boolean personBased = true;
RunScenario runScenario = RunScenario.policy;
Modes4Accessibility targetMode = Modes4Accessibility.pt;
AccessibilityConfigGroup.AccessibilityMeasureType measureType =
    AccessibilityConfigGroup.AccessibilityMeasureType.rawSum;
```

### Run options

| Setting | Options | Description |
|---|---|---|
| `personBased` | `true`, `false` | `true` calculates accessibility at individual home coordinates; `false` runs the location/grid-based calculation |
| `runScenario` | `base`, `policy` | Selects the MATSim base or car-free policy scenario |
| `targetMode` | `car`, `pt`, `teleportedWalk` | Selects the transport mode |
| `measureType` | `rawSum`, `logSum` | Selects the MATSim accessibility measure |

Pass the MATSim simulation output folder with `--scenario-dir`. The runner
discovers the run prefix from the events file and uses it to find the other
simulation outputs in that folder. For `--opportunity facilities`, it loads the
facilities from the same run folder. The `--scenario` flag labels the results
as `base` or `policy`.

Accessibility CSVs go to `analysis/accessibility` inside the simulation output
folder by default. Use `--output-dir` to write to a different location.

The accessibility computation area is also selected automatically:

- `personBased = true` -> `fromPopulation`
- `personBased = false` -> `fromBoundingBox`

The location-based calculation uses a tile size of 1,000 m.

---

## 3. Required MATSim inputs

The Java runner currently expects the Berlin 1% scenario outputs under:

```text
../public-svn/matsim/scenarios/countries/de/berlin/projects/fabilut/output-1pct/
```

with the following directory structure:

```text
output-1pct/
├── base/
└── policy/
```

For the selected scenario, the following files are used:

```text
berlin-v6.3.output_events.xml.gz
berlin-v6.3.output_network.xml.gz
berlin-v6.3.output_transitSchedule.xml.gz
```

The population/plans file is always loaded from the base scenario:

```text
base/berlin-v6.3.output_plans.xml.gz
```

The coordinate reference system used by the analysis is:

```text
EPSG:25832
```

---

## 4. Accessibility opportunity locations

The accessibility calculation uses a manually defined set of spa facilities.

| Facility | Area |
|---|---|
| Vabali | Mitte |
| Liquidrom | Friedrichshain-Kreuzberg |
| Meridian | Spandau |
| Aspria | Charlottenburg-Wilmersdorf |
| Hamam | Friedrichshain-Kreuzberg |
| Saunabad | Pankow |
| Kristall | Ludwigsfelde |

The coordinates are defined directly in `RunPersonBasedAccBerlin.java`.

---

## 5. Modified accessibility classes

The following accessibility classes were changed for the person-based implementation.

### `RunPersonBasedAccBerlin.java`

Main runner for the Berlin case study.

Main functions:

- selects person-based or location-based accessibility;
- selects base or policy scenario;
- selects transport mode;
- selects `rawSum` or `logSum`;
- defines the opportunity facilities;
- extracts each person's home coordinates;
- assigns `homeX` and `homeY` as person attributes;
- receives person-level accessibility results;
- exports person-level accessibility to CSV.

For the person-based calculation, the first home activity in the selected plan is used as the person's accessibility location. Persons without a selected plan or valid home coordinate are excluded.

### `NetworkModeAccessibilityExpContributionCalculator.java`

Used for the **car** accessibility calculation.

This class was adjusted for the person-based accessibility implementation and contains the income-dependent monetary distance component used in the thesis.

The relevant car monetary utility term is:

```java
double utilityDistCarMonetary =
    distCar * betaCarDistMonetary_m * marginalUtilityOfMoney * incomeFactor;
```

The `incomeFactor` changes the weight of monetary car travel costs according to the person's income.

#### Car accessibility without income integration

The R analysis also requires:

```text
pbAcc_car_no_income.csv
```

This file is generated through a separate base-scenario car run.

To reproduce it, manually remove/comment out `incomeFactor` from the monetary car distance calculation in `NetworkModeAccessibilityExpContributionCalculator.java`.

Change:

```java
double utilityDistCarMonetary =
    distCar * betaCarDistMonetary_m * marginalUtilityOfMoney * incomeFactor;
```

to:

```java
double utilityDistCarMonetary =
    distCar * betaCarDistMonetary_m * marginalUtilityOfMoney;
```

Then run the **base car accessibility calculation** and save/rename the resulting output as:

```text
pbAcc_car_no_income.csv
```

Restore `incomeFactor` afterwards for the normal income-dependent accessibility runs.

### `TeleportedModeContributionCalculator.java`

Used for the **teleportedWalk** accessibility calculation.

Slight adjustments were made to:

- enable person-based accessibility calculations using individual home coordinates; and
- optimise the accessibility computation process.

### `TripRouterAccessibilityContributionCalculator.java`

Used for the **public transport (`pt`)** accessibility calculation.

Slight adjustments were made to:

- enable person-based accessibility calculations using individual home coordinates; and
- optimise the accessibility computation process.

---

## 6. Java accessibility outputs

For person-based runs, `RunPersonBasedAccBerlin.java` exports files using the naming convention:

```text
pbAcc_<mode>_<scenario>_<measureType>.csv
```

Examples:

```text
pbAcc_car_base_rawSum.csv
pbAcc_car_policy_rawSum.csv
pbAcc_pt_base_rawSum.csv
pbAcc_teleportedWalk_base_rawSum.csv
```

The person-level output contains:

```text
personId
age
sex
income
carAvail
restricted_mobility
homeX
homeY
accessibility
```

For the thesis multimodal analysis, the main required Java runs are:

```text
personBased = true
runScenario = base
targetMode = teleportedWalk
measureType = rawSum
```

```text
personBased = true
runScenario = base
targetMode = pt
measureType = rawSum
```

```text
personBased = true
runScenario = base
targetMode = car
measureType = rawSum
```

```text
personBased = true
runScenario = policy
targetMode = car
measureType = rawSum
```

Walking and public transport are unchanged between the base and policy scenarios, so only the car accessibility calculation is repeated for the policy scenario.

A separate base car run without `incomeFactor` is required for the income-integration comparison, as described above.

---

## 7. R analysis

The thesis analysis script is located at:

```text
contribs/accessibility/src/main/R/THESIS/analysis.R
```

Required packages:

```r
dplyr
readr
sf
tidyr
ggplot2
```

The script uses paths relative to the `THESIS` directory:

```r
base_dir   <- "."
input_dir  <- file.path(base_dir, "input")
output_dir <- file.path(base_dir, "output")
```

This avoids machine-specific absolute paths. Run `analysis.R` with the working directory set to:

```text
contribs/accessibility/src/main/R/THESIS/
```

The script will then read inputs from `input/` and write generated tables and plots to `output/`.

---

## 8. R input files

The following accessibility CSV files must be placed in:

```text
contribs/accessibility/src/main/R/THESIS/input/
```

Required files:

```text
pbAcc_teleportedWalk_base_rawSum.csv
pbAcc_pt_base_rawSum.csv
pbAcc_car_base_rawSum.csv
pbAcc_car_policy_rawSum.csv
pbAcc_car_no_income.csv
```

The Berlin district shapefiles are already included in the repository under:

```text
contribs/accessibility/src/main/R/THESIS/input/Berlin shapefiles/
```

The analysis uses:

```text
Berlin_Bezirke.shp
```

and the district-name field:

```text
Gemeinde_n
```

The shapefile is handled in `EPSG:25832`.

---

## 9. R processing workflow

The five accessibility CSV files are read and merged using:

```text
personId
```

The R script then calculates the single-mode logsums:

```text
twalk_logSum
pt_logSum
car_logSum_base
car_logSum_policy
```

Car accessibility change is calculated as:

```text
car_delta = car_logSum_policy - car_logSum_base
```

### Multimodal accessibility

Multimodal accessibility is calculated from the combined mode-specific raw sums:

```r
mm_logSum_base =
    log(twalk_rawSum + pt_rawSum + car_base_rawSum) / beta

mm_logSum_policy =
    log(twalk_rawSum + pt_rawSum + car_policy_rawSum) / beta
```

with:

```r
beta <- 1
```

The policy change in multimodal accessibility is:

```text
mm_delta = mm_logSum_policy - mm_logSum_base
```

### Income-integration comparison

The effect of income integration is calculated as:

```text
base_car_delta_incomeFactor =
    car_logSum_base - car_base_logSum_no_income
```

### Winners and losers

For the multimodal policy analysis:

```text
mm_delta > 0  -> Winner
mm_delta < 0  -> Loser
mm_delta == 0 -> No change
```

### Berlin districts

Individual home coordinates are spatially joined to the Berlin district shapefile. The resulting district variable is included in the final analysis dataset.

---

## 10. Main processed dataset

Running `analysis.R` creates:

```text
output/acc.csv
```

This is the main combined person-level analysis dataset and contains:

```text
personId
age
sex
income
homeX
homeY
district

twalk_rawSum
pt_rawSum
car_base_rawSum
car_policy_rawSum

twalk_logSum
pt_logSum
car_logSum_base
car_logSum_policy

car_delta

car_base_logSum_no_income
base_car_delta_incomeFactor

mm_logSum_base
mm_logSum_policy
mm_delta

outcome
```

---

## 11. Tables produced by `analysis.R`

The current analysis script exports the following CSV tables to:

```text
contribs/accessibility/src/main/R/THESIS/output/
```

### Combined person-level data

```text
acc.csv
```

### Income integration

```text
table_summary_income_integration.csv
```

Contains accessibility with and without income integration by income group, including mean, median and change statistics.

### Overall accessibility

```text
summary_accessibility.csv
```

Contains base/policy mean and median accessibility, accessibility changes, standard deviation of change and shares improved/worsened/no change for the relevant modes.

### Accessibility by income group

```text
summary_accessibility_by_income.csv
```

Contains multimodal base accessibility, policy accessibility and accessibility change by income group.

### Median accessibility by mode, income and scenario

```text
median_accessibility_by_income_mode_scenario.csv
```

Contains median accessibility by:

- income group;
- transport mode; and
- base/policy scenario.

Walking and public transport values are duplicated for the policy scenario in this table because those modes are unchanged by the car-free policy.

### Overall winners / losers

```text
summary_outcome_overall.csv
```

Contains the overall counts and percentages of:

- Winner;
- Loser;
- No change.

### Winners / losers by income

```text
summary_outcome_by_income.csv
```

Contains counts and percentages of improved, worsened and unchanged individuals for each income group.

---

## 12. Plots produced by `analysis.R`

The current script exports the following plots to the `output/` directory.

### Income integration

```text
scatter_car_acc_income.png
```

Person-level comparison of base car accessibility with and without income integration.

```text
boxplot_base_car_deltaacc_by_income.png
```

Distribution of the effect of income integration on base car accessibility by income group.

### Car vs multimodal accessibility

```text
scatter_car_mm_acc_base.png
```

Person-level comparison of base car accessibility and base multimodal accessibility.

### Accessibility by mode, income and scenario

```text
bar_medianacc_by_income_mode_scenario.png
```

Median accessibility by income group and mode, shown separately for the base and policy scenarios.

### Policy accessibility change by income

```text
boxplot_car_deltaacc_by_income.png
```

Distribution of car accessibility change by income group.

```text
boxplot_mm_deltaacc_by_income.png
```

Distribution of multimodal accessibility change by income group.

### Policy accessibility change by district

```text
boxplot_car_acc_by_district.png
```

Distribution of car accessibility change by Berlin district.

```text
boxplot_mm_acc_by_district.png
```

Distribution of multimodal accessibility change by Berlin district.

---

## 13. Reproducing the thesis analysis

### Step 1: Prepare the MATSim scenario outputs

Ensure the Berlin base and policy scenario output directories contain the required:

```text
events
network
transit schedule
plans
```

files listed above.

### Step 2: Generate person-based walking accessibility

Run `RunPersonBasedAccBerlin.java` with:

```java
personBased = true;
runScenario = RunScenario.base;
targetMode = Modes4Accessibility.teleportedWalk;
measureType = AccessibilityConfigGroup.AccessibilityMeasureType.rawSum;
```

Expected output:

```text
pbAcc_teleportedWalk_base_rawSum.csv
```

### Step 3: Generate person-based public transport accessibility

Use:

```java
personBased = true;
runScenario = RunScenario.base;
targetMode = Modes4Accessibility.pt;
measureType = AccessibilityConfigGroup.AccessibilityMeasureType.rawSum;
```

Expected output:

```text
pbAcc_pt_base_rawSum.csv
```

### Step 4: Generate base car accessibility

Use:

```java
personBased = true;
runScenario = RunScenario.base;
targetMode = Modes4Accessibility.car;
measureType = AccessibilityConfigGroup.AccessibilityMeasureType.rawSum;
```

Expected output:

```text
pbAcc_car_base_rawSum.csv
```

### Step 5: Generate policy car accessibility

Use:

```java
personBased = true;
runScenario = RunScenario.policy;
targetMode = Modes4Accessibility.car;
measureType = AccessibilityConfigGroup.AccessibilityMeasureType.rawSum;
```

Expected output:

```text
pbAcc_car_policy_rawSum.csv
```

### Step 6: Generate base car accessibility without income integration

In:

```text
NetworkModeAccessibilityExpContributionCalculator.java
```

temporarily remove/comment out `incomeFactor` from:

```java
double utilityDistCarMonetary =
    distCar * betaCarDistMonetary_m * marginalUtilityOfMoney * incomeFactor;
```

so that it becomes:

```java
double utilityDistCarMonetary =
    distCar * betaCarDistMonetary_m * marginalUtilityOfMoney;
```

Run the base car calculation again and save the result as:

```text
pbAcc_car_no_income.csv
```

Restore the income factor afterwards.

### Step 7: Copy accessibility outputs to the R input folder

Place the five CSV files in:

```text
contribs/accessibility/src/main/R/THESIS/input/
```

The Berlin shapefiles are already included in:

```text
input/Berlin shapefiles/
```

### Step 8: Open the `THESIS` directory

Use the following directory as the R working directory:

```text
contribs/accessibility/src/main/R/THESIS/
```

Because `analysis.R` uses:

```r
base_dir <- "."
```

no machine-specific path adjustment is required.

### Step 9: Run `analysis.R`

Running the complete script:

1. reads and cleans the accessibility outputs;
2. merges agents by `personId`;
3. assigns Berlin districts;
4. calculates mode-specific accessibility;
5. calculates multimodal accessibility;
6. calculates policy changes;
7. performs the income-integration comparison;
8. creates the summary tables; and
9. recreates the thesis plots.

All generated files are written to:

```text
contribs/accessibility/src/main/R/THESIS/output/
```

---

## 14. Notes on base and policy scenarios

The policy scenario changes car travel conditions inside the Berlin car-free zone.

Walking and public transport accessibility inputs are therefore taken from the base scenario for both the base and multimodal policy calculations.

The multimodal policy calculation consequently differs from the base calculation through the policy car accessibility term:

```text
Base:
walk rawSum + PT rawSum + base car rawSum

Policy:
walk rawSum + PT rawSum + policy car rawSum
```

---

## 15. Repository components required for reproduction

The relevant components are:

```text
contribs/accessibility/src/main/java/
    ...
    RunPersonBasedAccBerlin.java
    NetworkModeAccessibilityExpContributionCalculator.java
    TeleportedModeContributionCalculator.java
    TripRouterAccessibilityContributionCalculator.java

contribs/accessibility/src/main/R/THESIS/
├── analysis.R
├── input/
│   ├── pbAcc_teleportedWalk_base_rawSum.csv
│   ├── pbAcc_pt_base_rawSum.csv
│   ├── pbAcc_car_base_rawSum.csv
│   ├── pbAcc_car_policy_rawSum.csv
│   ├── pbAcc_car_no_income.csv
│   └── Berlin shapefiles/
│       └── Berlin_Bezirke.*
└── output/
    ├── acc.csv
    ├── summary tables
    └── thesis plots
```

Together, these files provide the Java accessibility calculation, the intermediate person-level accessibility outputs, and the R analysis required to recreate the thesis results and plots.

