# DRT Operations

Provides functionality to realistically simulate operational aspects,
designed for, bot not limited to, non-autonomous services.

Initially developed for MOIA GmbH

If used, please cite:

Felix Zwick, Nico Kuehnel, Sebastian Hörl.
Shifts in perspective: Operational aspects in (non-)autonomous 
ride-pooling simulations.
Transportation Research Part A: Policy and Practice,
Volume 165, 2022, Pages 300-320.
https://doi.org/10.1016/j.tra.2022.09.001.


## Core features:

- Operation facilities
- (Driver) shifts


The entry point for setting up a simulation are the specific control(l)er creators:
- DrtOperationsControlerCreator
  - or
- EDrtOperationsControlerCreator
  - in the electric vehicles case

## Operation Facilities
Operation facilities are meant to represent hubs and in-field break locations.
The facilities have a capacity that cannot be exceeded and may be linked to
existing chargers via the id.

(Driver) shifts may only start or end at operation facilities in the default setup.
Vehicles will route to operation facilities to end a shift or for scheduling a break.

Operational facilities may be described with an xml file like this:
```
<facilities>
    <facility id="1" linkId="274110" x="4478595" y="5304631" chargerId="1" capacity="20" type="hub">
        <chargers>
            <charger id="1"/>
            <charger id="2"/>
        </chargers>
    </facility>
</facilities>
```

## Shifts
Shifts define periods in which vehicles may be active serving passengers.
Shifts are dynamically assigned to vehicles.

In autonomous settings, shifts may be used to model up- and down-time and/or cleaning
cycles.

Shifts have a start and end time and can optionally have a break which is defined
by earliest start and latest end as well as a duration. Optionally, as operation 
facility id may be defined to control the location of the start/end of the shift.

Shifts may be described in an xml file likes this:
```
<shifts>
    <shift id="0" start="14400" end="45000" operationFacilityId="1">
        <break earliestStart="28800.0" latestEnd="32400.0" duration="1800.0"/>
    </shift>
    <shift id="1" start="14400" end="45000" operationFacilityId="1">
        <break earliestStart="28800.0" latestEnd="32400.0" duration="1800.0"/>
    </shift>
</shifts>
```

