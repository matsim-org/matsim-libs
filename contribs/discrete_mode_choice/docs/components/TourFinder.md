# Tour Finders

If a tour-based [model](Model.md) is used, the purpose of a `TourFinder` is to divide a given agent plan into separate tours, for which mode choice can be performed independently. Compared to a full plan choice this significantly can decrease the size of the choice sets.

The DMC extension contains a number of predefined tour finders, but it is also possible to write custom ones. How to do that is explained in [Customizing the model](docs/Customizing.md).

In the following the existing built-in tour finders are described. While some of them have additional configuration options that can be defined in a `parameterset`, some don't. In any case, the tour finder can be chosen in the main config group:

```xml
<module name="DiscreteModeChoice">
	<!-- Defines which TourFinder component to use. Built-in choices: ... -->
	<param name="tourFinder" value="" />
</module>
```

## ActivityBased

*Description:* This tour finder divides a plan depending on specific activity types. For instance, if "home" is chosen, home-based tours will be considered, i.e. each chain of trips starting and ending at a "home" activity is a tour.

*Configuration:*

```xml
<parameterset type="tourFinder:ActivityBased" >
	<!-- Comma-separated list of activity types which should be considered as start and end of a tour. If a plan does not start or end with such an activity additional tours are added. -->
	<param name="activityTypes" value="home" />
</parameterset>
```

## PlanBased

*Description:* The `PlanBased` tour finder considers the whole plan as one tour. It virtually turns the tour-based model into a plan-based model.

*Configuration:*
No specific configuration available.

## HomeBased

*Description:* The `HomeBased` tour finder makes use of the `HomeFinder` component. Depending on which location is defined as the agent's home location by the `HomeFinder`, tours will be cut at this location.

*Configuration:*
No specific configuration available.

