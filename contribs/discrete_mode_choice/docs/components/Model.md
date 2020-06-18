# Model

The Discrete Mode Choice extension defines two model types: trip-based and tour-based. They can be configured as follows:

```xml
<module name="DiscreteModeChoice">
	<!-- Main model type: Trip, Tour -->
	<param name="modelType" value="Tour" />
</module>
```

In order for the models to work properly, make sure to define the following components in the configuration:

- A [ModeAvailability](ModeAvailability.md), which defines which modes are available to an agent
- An [Estimator](Estimator.md), which assigns a utility to all possible alternatives for a trip or a tour
- A [Selector](Selector.md), which defines the process by which certain utility-based alternatives are chosen

Optionally, [Constraints](Constraint.md) can be defined that make sure that the generated choice alternatives are realistic and the [TourFinder](TourFinder.md) decided where to start and end tours if a tour-based model is selected.

For a brief overview how the trip- and the tour-based model work, please have a look at the section "A little bit of theory" in the - [Getting Start](../GettingStarted.md) guide.










