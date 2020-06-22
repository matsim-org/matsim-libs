# Estimator

An `Estimator` component has the purpose to analyze a certain propose mode (in a trip-based model) or a chain of modes (in a tour-based model) and assign a certain utility to it. Later on, this utility can be used to make an educated guess about which alternative should be chosen.

The DMC extension contains a number of predefined `Estimator` implementations, but it is also possible to write custom ones. How to do that is explained in [Customizing the model](../Customizing.md).

In the following the existing built-in estimators are described. Some of them exist in a trip-based and tour-based version, some only in one. While some of them have additional configuration options that can be defined in a `parameterset`, some don't. In any case, the estimator can be chosen in the main config group. For a trip-based model, the estimator can be defined as `tripEstimator`, while for a tour-based model it needs to be set in the `tourEstimator` parameter. Depending on which [model](Model.md) type is used, the respective field is active:

```xml
<module name="DiscreteModeChoice">
	<!-- Defines which TourEstimator component to use. Built-in choices: ... -->
	<param name="tourEstimator" value="Cumulative" />
	<!-- Defines which TripEstimator component to use. Built-in choices: ... -->
	<param name="tripEstimator" value="Uniform" />
	<!-- Trips tested with the modes listed here will be cached for each combination of trip and agent during one replanning pass. -->
	<param name="cachedModes" value="car, pt, ..." />
</module>
```

The `cachedModes` parameter defines which trip estimates should be cached. This is especially useful in a tour-based set-up. If estimates are not cached, trips are re-routed for each possible tour. If they are cached, one and the same trip (with the same mode) reuses one estimate in any tour that it is part of.

## Cumulative

*Description:* The `Cumulative` tour estimator is a special estimator that does not perform any estimation on its own. Instead if look up the estimator given in `tripEstimator` and applies it to each trip in a tour independently. Finally, the utilities of the single trips are summed up.

*Level:* Tour

*Configuration:*
No specific configuration available. The `tripEstimator` field from the main DMC configuration is used.

## MATSimTripScoring

*Description:* The `MATSimTripScoring` trip estimator approximates the MATSim scoring function. Internally, the considered trip is routed using MATSim's `TripRouter` component. Afterwards, the scoring parameters defined in the respective `calcScore` config group are applied. Since the at the time of replanning the exact resulting departure and travel time is not known completely, this is only an approximation. 

*Level:* Trip

*Configuration:*
No specific configuration is available. The parameters from `calcScore` are used.

## MATSimDayScoring

*Description:* The `MATSimDayScoring` tour estimator approximates the MATSim scoring function, similar to the `MATSimTripScoring` esimator, on which it builds. Two improvement are included that make use of the tour-based character: Using the initial departure time of the tour and the estimated travel times, delays can be predicted and considered to certain extent. Also, day-based scoring parameters such as daily costs are considered in this estimator. Again, it only approximates the score accumulated by trips throughout the considered day, but *not* the score resulting from activities. *In principle, this would be possible, so here we consider it as future work*. Note that day-based scoring parameters are considered. Therefore, the estimator should mainly be used in a plan-based context (except those parameters can be expected to be zero).

*Level:* Tour (preferrable plan-based setup)

*Configuration:*
No specific configuration is available. The parameters from `calcScore` are used.

## Uniform

*Description:* The `Uniform` selector returns `1.0` for any alternative that it encounters. Therefore, each altenratives obtaines the same valuation.

*Level:* Tour or Trip

*Configuration:*
No specific configuration is available.