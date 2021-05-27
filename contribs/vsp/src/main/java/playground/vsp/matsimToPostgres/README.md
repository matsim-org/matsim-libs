## Overview

This is an extension for exporting the output data (currently trip and legs output) of matsim runs to a postgres database.
Only final run data is exported. Once added, the exporter is executed on the controler shutdown event.

## How to use

Example use cases can be found under [testRunner](testRunner).

#### Prerequisites

- You need to have a postgres database up and running on a server/ local machine
- This postgres database needs to have 'postgis' installed as extension
- Your server's permissions need to allow access from outside (if needed so)
- You need to gather all relevant information for connecting to the database in a dbParam.xml-File (for structure and example see [dbParam.xml](testRunner/dbParam.xml))
- The respective postgres user needs database reading and writing permissions


#### Step 1: Add the exporter module as a controler listener binding

The PostgresExporterModule can be added to controler via the following code snippet:

*controler.addOverridingModule(new PostgresExporterModule());*


#### Step 2: Have the PostgresExporterConfigGroup included in your config

A 'postgresExporter' config group needs to be included in your config. An example can be found [here](testRunner/postgresExporterExampleConfig.xml).

The **'dbParamFile'** parameter defines where the dbParamFile is located. This file contains all relevant data for connecting to the postgres database.

The **'sqlAnalzyerDir'** parameter defines where the sql analyzer scripts are stored that should be updated automatically after each run.

The **'overwriteRunSettings'** parameter defines what the exporter should do if a run is already included in the database.

The **'intialData'** parameter set contains relevant settings for initial data upload
(data which need to uploaded once before importing first run data, e. g. statistical data, geometries etc...).
This set is currently not in use as the initial data upload is not included in this exporter module yet.
However, it is of course possible to upload initial data to the database manually.


#### Step 3: Make sure that writer outputs are generated adequate

The current idea of the Postgres Exporter is to rely on existing matsim output writers for the afterward conversion and upload to the database.
At the moment, the exporter includes upload of legs and trips data. Hence, a requirement is that before the execution of the PostgresExporter
trip.csv.gz and legs.csv.gz are generated in the main output directory. You can steer the generation interval by:

*config.controler().setWriteTripsInterval(integer);*


## Postgres database structure

Matsim output data representation in the postgres database is currently implemented like this:

Each type of output data receives its own table in the matsim_output schema, e. g. trips are stored in the 'output_trips' table.
Every time one imports exports data to the database, new run data is appended to the respective table (with regards to the overwriteRunSettings).
This structure has the benefit of keeping data of similar structure in one place. The main disadvantage is that tables are getting quite large.
To keep up query speed, tables should be introduced an index/ should be partitioned (currently not implemented).






