# small scale traffic generation

This provides a tool to generate small-scale commercial traffic. 

The tool is based on :
- IVV. Kleinräumige Wirtschaftsverkehrsmodelle. Endbericht zum Forschungsprojekt FE-Nr. 70.0689/2002/ im Auftrag des Bundesministeriums für Verkehr, Bau und Wohnungswesen. 2005. Download:
[https://daten.clearingstelle-verkehr.de/194/](https://daten.clearingstelle-verkehr.de/194/).

The description of the implementation in MATSim is given in the following paper :
- R. Ewert and K. Nagel, “Agentenbasierte Modellierung des kleinräumigen Wirtschaftsverkehrs,” 2024. Accepted for publication at HEUREKA Conference 2024.

## Model types
The tool provides two different model types of small-scale commercial traffic. Therefore, you can select between the following options:
- **commercialPersonTraffic**: This model contains the personal commercial traffic. This traffic has the objective to transport persons from one location to another. 
- **goodsTraffic**: This model contains the goods traffic. This traffic has the objective to transport goods from one location to another.
  - !! The long distance freight traffic is not included in this model. !! Therefore, see application contrib: [link](https://github.com/matsim-org/matsim-libs/tree/master/contribs/application/src/main/java/org/matsim/application/prepare/freight)  
- **completeSmallScaleCommercialTraffic**: This model contains both, the personal commercial traffic and the goods traffic.
 

## Input data
- The generation model uses only open data.
  - zone shape file:
    - contains the zones for each region
    - the user can define the column of the name of the zone
  - structure data:
      - contains numbers of inhabitants and employees per sector for each `region`
  - OSM data:
    - Regions
      - shape contains the regions related to the regions in the structure data
      - the user can define the column of the name of the region
    - Landuse
      - the user can define the column of landuse categories
    - Buildings
      - should contain the following columns: `levels`, `area`
      - the user can define the column of the buildings categories
  
## Usage
For generating the traffic, the following steps are necessary:
- get necessary input data
- run the tool by calling the main class `GenerateSmallScaleCommercialTrafficDemand`

## Example
An example is given as a test. See test `RunGenerateSmallScaleCommercialTrafficTest` and the mentioned example input data.

