
### Main features of the contrib

Allows to simulate carsharing modes: station based (one-way, round-trip) and freefloating.  

The main features of the current state are:  
- Different companies providing carsharing services
- Flexible cost structures per company and per mode
- Membership per operator and per mode
- Infrastructure for models that let people decide should they keep their vehicles during activities
- Infrastructure for models that let people decide from which operator should they rent a vehicle (in case they are members of more than 1)
- Vehicles infrastructure is flexible allowing easy implementation of special vehicles (e.g. electrical) * More convenient output 
- Input is completely separated from default MATSim files (just the config file has to be adapted now)
- (Easy) integration with other modes
 
 
 ### Input
 There is an [Integration Test](https://github.com/matsim-org/matsim-libs/blob/carsharingpatch/contribs/carsharing/src/test/java/org/matsim/contrib/carsharing/runExample/RunCarsharingIT.java) that also provides an [example setup](https://github.com/matsim-org/matsim-libs/tree/carsharingpatch/contribs/carsharing/test/input/org/matsim/contrib/carsharing/runExample/RunCarsharingIT) for carsharing.
 
 In general you need the following files:
 - Carsharing stations xml file, in this example called `CarsharingStations.xml` contains the information of the carsharing supply avaialble in the study area. Here you can define the availability of different carsharing services for multiple companies. Each company needs to have its own `company` tag. In the example case there is only one comapny called Mobility. For station based services each tag needs to ahve the following informaiton: for twoway services, you need to prvovide an id and x and y coordiantes of the station, followed by the vehicles belonging to this stations; for oneway services, you need to provide a unique id, x/y coordinates and number of additional free parking spaces, followed by the list of currently avaialble vevhicles. For freefloating services, you only need to provide information about their unique id, x/y coordiante location and type (i.e., car, transporter, mini, etc.)
 - Membership xml file, in this example called `CSMembership.xml`. For each person you can assign for each company for which services whether the person holds membership. For those individuals that do not own a carsharing memebrship you do not need to add an entry
 - Config file for your simulation needs to be updated with the additional modules. Carsharing module is used to define output writing frequency, input carsharing stations and membership files. Three additional modules FreeFloating, OneWayCarsharing, and TwoWayCarsharing are available and need to be added depending which services you want to simulate. These additional modules cna be used to easily switch on or off the usage of each carsharing mode in the simulation.


### Usage
Main method of the `RunCarsharing` class takes only the config file as an input in order to run the carsharing module.


### Models
The models infrastcture along with examples are located in models package of the carsharing contrib.

`CostsCalculator` which is later used in the scoring is located in the `manager.supply.costs` package along with an example.

In order to implement your own cost structure one needs to implement `CostCalculation` interface and to define cost structures for each operator(company) and each carsharing option.

In `CarsharingUtils` class in the runExample package one can see an example of defining cost structures for different companies and carsharing types.

### Output
Output files are located in each iteration folder and contain all the necessary information about all the carsharing rentals during the iteration.

## Main reference

The main research reference for the station based carsharing services:
> Balac, M., F. Ciari, and K. W. Axhausen (2015) Carsharing demand estimation: Zurich, Switzerland, area case study. Transportation Research Record, 2536, 10-18.

The main research reference for the free-floating carsharing service:
> Balac, M., H. Becker, F. Ciari, and K. W. Axhausen (2019) Modeling competing free-​floating carsharing operators: A case study for Zurich, Switzerland. Transportation Research Part C: Emerging Technologies, 98, 101-117.
