# Train-Specification

## Introduction

railsim supports the simulation of specific, train-related behavior, e.g. acceleration based on the total weight of a
train. In order to simulate this detailed behavior, additional attributes must be specified per train, i.e. per vehicle
type in MATSim.

This document specifies these custom attributes.

## Specification

### TransitVehicle Attributes

Default vehicle type attributes for length, maximum velocity and capacity.
Set network mode to rail.

#### railsimAcceleration

The vehicle-specific acceleration. Unit: meters per square-seconds \[m/s²]

#### railsimDeceleration

The vehicle-specific deceleration. Unit: meters per square-seconds \[m/s²]

## Examples

```xml

<vehicleType id="fvDosto">
    <attributes>
        <attribute name="railsimAcceleration" class="java.lang.Double">0.4</attribute>
        <attribute name="railsimDeceleration" class="java.lang.Double">0.5</attribute>
    </attributes>
    <capacity seats="606" standingRoomInPersons="0"/>
    <length meter="200.0"/>
    <width meter="1.435"/>
    <maximumVelocity meterPerSecond="55.556"/>
    <networkMode networkMode="rail"/>
</vehicleType>
```
