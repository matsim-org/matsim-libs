# Train-Specification for railsim

## Introduction

railsim supports the simulation of specific, train-related behavior, e.g. acceleration based on the total weight of a
train. In order to simulate this detailed behavior, additional attributes must be specified per train, i.e. per vehicle
type in MATSim.

## Specification

### TransitVehicle Attributes

TODO, e.g. length and acceleration of a train.

#### maxAcceleration

The vehicle-specific acceleration. Unit: meters per square-seconds \[m/s²]

(ik, mu): Suggestion, maybe just `acceleration`? Do we always accelerate at maximum speed?

#### maxDeceleration

The vehicle-specific maximum deceleration for emergency braking ("bremsweg"). Unit: meters per square-seconds \[m/s²]

#### deceleration

The typical vehicle-specific deceleration. Unit: meters per square-seconds \[m/s²]

## Examples

```xml

<vehicleType id="fvDosto">
    <attributes>
        <attribute name="acceleration" class="java.lang.Double">0.5</attribute>
        <attribute name="deceleration" class="java.lang.Double">0.5</attribute>
        <attribute name="maxDeceleration" class="java.lang.Double">0.7</attribute>
    </attributes>
    <capacity seats="606" standingRoomInPersons="0"/>
    <length meter="200.0"/>
    <width meter="1.435"/>
    <maximumVelocity meterPerSecond="55.556"/>
    <networkMode networkMode="rail/pt/railsim?"/>
</vehicleType>
```
