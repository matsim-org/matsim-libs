# Mode Availability

The mode availability component defines which mode are available to an agent, even before any trip or tour-baesd [constraints](Constraint.md) are evaluated. The classic example is an agent without a driving license, for which the `car` mode should not even be considered in any case.

The DMC extension contains a number of predefined `ModeAvailability` implementations, but it is also possible to write custom ones. How to do that is explained in [Customizing the model](../Customizing.md).

In the following the existing built-in tour finders are described. While some of them have additional configuration options that can be defined in a `parameterset`, some don't. In any case, the mode availability can be chosen in the main config group:

```xml
<module name="DiscreteModeChoice">
	<!-- Defines which ModeAvailability component to use. Built-in choices: ... -->
	<param name="modeAvailability" value="Default" />
</module>
```

## Default

*Description:* This `ModeAvailability` makes all modes available to every agent. It can be configured with a list of available modes.

*Configuration:*

```xml
<parameterset type="modeAvailability:Default" >
	<!-- Defines which modes are avialable to the agents. -->
	<param name="availableModes" value="pt, car, walk, bike" />
</parameterset>
```

## Car

*Description:* The `Car` `ModeAvailability` extends the `Default` version in that it considers who is allowed to use a car, based on agent attributes. Replicating the behaviour of `SubtourModeChoice` the "car" mode is not allowed in any of these cases:

- The `hasLicense` attribute of an agent is `no`
- The `carAvail` attribute of an agent is `never`

*Configuration:*

```xml
<parameterset type="modeAvailability:Car" >
	<!-- Defines which modes are avialable to the agents. -->
	<param name="availableModes" value="pt, car, walk, bike" />
</parameterset>
```
