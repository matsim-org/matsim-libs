# Contrib small scale traffic generation

This provides two tools to generate commercial traffic demand.
- Basic commercial traffic demand generation; package: `org.matsim.commercialDemandGenerationBasic`
- small-scale commercial traffic demand generation; package: `org.matsim.smallScaleCommercialTrafficGeneration`


## Basic commercial traffic demand generation

This tool to create carriers and related demand based on csv files, which should be provided by the user. 
E.g. the user can provide a shape file and an amount of demand and this demand will be automatically distributed to the carriers.
In consequence this tool can be used to create a specific demand for vehicle routing problems (VRPs), e.g. for a scenario for parcel delivery.

This tool is not used for the small-scale commercial traffic generation.

### Usage

- Run `BasicCommercialDemandGeneration.java` to create and solve s specific VRP.

### Example
An example is given as a test.
See test `BasicCommercialDemandGenerationTest.java` and the related input data.

## Creation of a small scale traffic model

The tool is based on :
- IVV. Kleinräumige Wirtschaftsverkehrsmodelle. Endbericht zum Forschungsprojekt FE-Nr. 70.0689/2002/ im Auftrag des Bundesministeriums für Verkehr, Bau und Wohnungswesen. 2005. Download:
[https://daten.clearingstelle-verkehr.de/194/](https://daten.clearingstelle-verkehr.de/194/).

The description of the implementation in MATSim is given in the following paper :
- R. Ewert and K. Nagel, “Agentenbasierte Modellierung des kleinräumigen Wirtschaftsverkehrs” 2024. Presented at HEUREKA Conference 2024. Download: [https://verlag.fgsv-datenbanken.de/tagungsbaende?kat=HEUREKA&p=3&tagungsband=2530&_titel=Agentenbasierte+Modellierung+des+kleinr%C3%A4umigen+Wirtschaftsverkehrs](https://verlag.fgsv-datenbanken.de/tagungsbaende?kat=HEUREKA&p=3&tagungsband=2530&_titel=Agentenbasierte+Modellierung+des+kleinr%C3%A4umigen+Wirtschaftsverkehrs).

### Model types
The tool provides two different model types of small-scale commercial traffic. Therefore, you can select between the following options:
- **commercialPersonTraffic**: This model contains the personal commercial traffic. This traffic has the objective to transport persons from one location to another. 
- **goodsTraffic**: This model contains the goods traffic. This traffic has the objective to transport goods from one location to another.
  - !! The long distance freight traffic is not included in this model. !! Therefore, see application contrib: [link](https://github.com/matsim-org/matsim-libs/tree/master/contribs/application/src/main/java/org/matsim/application/prepare/freight)  
- **completeSmallScaleCommercialTraffic**: This model contains both, the personal commercial traffic and the goods traffic.
 

### Input data
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
      - can contain the following attributes: `levels`, `area`
        - if no `levels` are given, the tool assumes a default value of 1
        - if no `area` is given, the tool calculates the area based on the geometry
      - the user can define the column of the buildings categories
  
### Usage
For generating the traffic, the following steps are necessary:
- get necessary input data
- Run `CreateDataDistributionOfStructureData` to create the distribution of the structure data to create the commercial facilities
- Run `GenerateSmallScaleCommercialTrafficDemand` by using the created facilities. Here you can set the model type.

### Example
An example is given as a test.
See tests `RunCreateDataDistributionOfStructureDataTest` and `CommercialTrafficIT.java` and the related input data.

[//]: # (yyyyyy I cannot find the second run class.)

