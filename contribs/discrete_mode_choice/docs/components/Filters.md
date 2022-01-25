# Constraints

A `TripFilter` or `TourFilter` is used to select which trips or tours should even be considered from mode choice. Note that this is different than restricting the choice set for a certain trip or tour in terms of available modes through a [constraint](Constraint.md). The filter decides if mode choice is performed *at all*. 

Typically use cases would be to restrict mode choice to tours of a certain length, or to ignore trips and tours that cross some kind of analysis region.

In the configuration file, filters can be selected on a trip- and tour level:

```xml
<module name="DiscreteModeChoice">
	<!-- Defines a number of TripFilter components that should be activated. Built-in choices: ... -->
	<param name="tripFilters" value="" />
	
	<!-- Defines a number of TourFilter components that should be activated. Built-in choices: ... -->
	<param name="tourFilters" value="" />
</module>
```

Current, only one built-in filter is available. It can be defined through the respective `tourFilter:*` paramter sets.

## TourLength

*Description:* The `TourLength` tour filter excludes tours which exceed a certain length.

*Level:* Tour

*Configuration:*

```xml
<parameterset type="tourFilter:TourLength" >
	<!-- Defines the maximum allowed length of a tour. -->
	<param name="maximumLength" value="10" />
</parameterset>
```

