## Multi-dimensional loads and capacities for DVRP / DRT

By default, one person represents one passenger load unit in DRT and each vehicle has a passenger capacity. Vehicles can either be defined by providing a DVRP fleet file. In that case the passenger capacity is read from the `capacity` property of each vehicle. Alternatively, DVRP can read vehicles from a *vehicles file* with individual vehicle types. In that case, the passenger capacity is read from the `seats` property of each vehicle type.

### Defining complex capacities and loads

One can now define more complex capacities and loads. Capacities and loads are structured as a sequence of slots that have a specific (integer) value. For instance, a vehicle may have slots for passengers and for luggage. In that case, one can modify the `load` parameters set in the per-mode DRT config group:

```xml
<parameterset name="load">
  <param name="slots" value="passengers,luggage">
</parameterset>
```

### Defining request loads

The load for each request / person needs to be defined specifically. This can be done by providing the `dvrp:load` attribute for each trip (inside the attributes of the origin activity) in the following format:

```xml
<attribute name="dvrp:load" class="java.lang.String" value="passengers=1,luggage=2">
```

Omitted load slots are interpreted as "zero". In case a one-dimensional load is used, a single value may be provided:

```xml
<attribute name="dvrp:load" class="java.lang.Integer" value="1">
```

Alternatively, the load can be defined slot by slot using the `dvrp:load:` attribute prefix:

```xml
<attribute name="dvrp:load:passengers" class="java.lang.Integer" value="1">
<attribute name="dvrp:load:luggage" class="java.lang.Integer" value="2">
```

Either string-based or slot-based loads can be used for a single trip. If no load information can be found in the trip attributes, the logic will fall back to person attributes. If no person-level load information can be found, a unit of one is assigned to the `defaultRequestSlot` as defined in the load configuration:   

By default, one person is mapped to one slot of the capacity. This is set by default in the same parameter set:

```xml
<parameterset name="load">
  <param name="defaultRequestSlot" value="passengers">
</parameterset>
```

This example indicates that, if no other load information is provided for a trip/person, a unit load is assumed for the slot *passengers*. An exception will be thrown if the option is set to null. This way, the user can enforce that a load is defined for every request.

### Defining vehicle capacities from the fleet file

One option of loading vehicles for a DVRP mode is to use a fleet file with its respective format. In the fleet file, each *vehicle* has a *capacity* given as an integer. This value is transformed into a capacity by assigning the given integer to a slot that is configurable:

```xml
<parameterset name="load">
  <param name="mapFleetCapacity" value="passengers">
</parameterset>
```

### Defining vehicle capacities from the vehicles file

When reading the vehicles for the DVRP fleet from a vehicles file, each vehicle can have a `dvrp:capacity` attribute defining the quantities that are assigned to each capacity slot:

```xml
<attribute name="dvrp:load" class="java.lang.String" value="passengers=4,luggage=8">
```

Alternatively, the slots can be assigned individually using the `dvrp:capacity:` prefix:

```xml
<attribute name="dvrp:capacity:passengers" class="java.lang.Integer" value="4">
<attribute name="dvrp:capacity:luggage" class="java.lang.Integer" value="8">
```

Either stirng-based or slot-based configuration can be used, not both at the same time. If no information is found in the level of a *vehicle*, the *vehicle type* is examined with the same attributes as documented above. A vehicle that already defines its own capacities cannot have a vehicle type that defines capacities.

If neither the vehicle, nor the vehilce type, define capacities, the quantities given by the capacity object of the vehice type are examined. By default, MATSim vehicles provide capacity information for *seats*, *standingRoom*, *volume*, *weight*, and *other*. Via configuration, those quantities can be mapped to capacity slots of the vehicles in the configuration:

```xml
<parameterset name="load">
  <param name="mapVehicleTypeSeats" value="passengers">
  <param name="mapVehicleTypeStandingRoom" value="null">
  <param name="mapVehicleTypeVolume" value="null">
  <param name="mapVehicleTypeWeight" value="luggage">
  <param name="mapVehicleTypeOther" value="null">
</parameterset>
```

### Defining an even more complex load/capacity logic

DVRP now provides the `DvrpLoadType` and `DvrpLoad` interfaces. By creating new implementations of those interfaces one can define an arbitrary logic with respect to how capacities are handled in DVRP. In particular, the base functionality allows to define multi-dimensional loads and capacities that have *independent* slots. This means that, for instance, adding one more luggage to a vehicle does not affect the capacity of passengers. Such a logic, however, can be implemented using the cited interfaces.
