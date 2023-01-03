# Home finders

Home finders are not 100% necessary to set up a choice model, but they can be very helpful. For instance, the `VehicleContinuity` constraints make use of the home finde to determine where vehicles need to be brought back. Furthermore, the home finder can be used to segment a plan into tours.

The DMC extension contains a number of predefined home finders, but it is also possible to write custom ones. How to do that is explained in [Customizing the model](docs/Customizing.md).

In the following the existing built-in home finders are described. While some of them have additional configuration options that can be defined in a `parameterset`, some don't. In any case, the tour finder can be chosen in the main config group:

```xml
<module name="DiscreteModeChoice">
	<!-- Defines which HomeFinder component to use. Built-in choices: ... -->
	<param name="homeFinder" value="" />
</module>
```

## ActivityBased

*Description:* This home finder searches for the first occurence of an activity with one of the provided types and saves its location as the home location of the agent.

*Configuration:*

```xml
<parameterset type="homeFinder:ActivityBased" >
	<!-- Comma-separated activity types which should be considered as home. -->
	<param name="activityTypes" value="home" />
</parameterset>
```

## FirstActivity

*Description:* The `FirstActivity` home finder looks up the location of the first activity in the plan and saves it as the home location of the agent.

*Configuration:*
No specific configuration available.
