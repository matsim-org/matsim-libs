
# Informed mode choice

This contrib provides mode choice algorithms that can be used to replace the standard random based algorithms in MATSim.

The main idea is that utilities of available modes are estimated in the planning phase and higher ranked alternatives selected with higher probability.
Unpromising plan candidates can also be removed altogether, with a flexible pruning functionality.
Unlike the default mode choice implementation, it is also possibly to freely configure constraints on mode combinations.

The whole methodology is described in the paper below.

When referencing this work, please cite the following paper:
> C. Rakow and K. Nagel (2023) [An annealing based approach to informed mode choice in agent-based transport simulations](https://doi.org/10.1016/j.procs.2023.03.086), *Procedia Computer Science*, **220**, 667-673.


## Usage

This contrib provides a .
Estimators, Constraints and Pruning is configured via the `Builder` of the module.

Default estimators for standard MATSim scoring are already provided and can be used like in this example:

```java
InformedModeChoiceModule.Builder builder = InformedModeChoiceModule.newBuilder()
        // Car has a non zero daily costs, so fixed costs need to be considered
        .withFixedCosts(FixedCostsEstimator.DailyConstant.class, "car")
        // Configure modes that are available for all agents
        .withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, "ride", "bike", "walk")
        // Car can only be used if the "carAvail" attribute allows it
        .withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderIfCarAvailable.class, "car")
        // Pt estimator is not leg based but needs to know the whole trip
        .withTripEstimator(PtTripEstimator.class, ModeOptions.AlwaysAvailable.class, "pt");
        // Adds a subtour constraint
        .withConstraint(RelaxedSubtourConstraint.class)
        // Install a pruner based on score and distance
        .withPruner("name", new DistanceBasedPruner(10, 10))
```

Most importantly install the module using:

```java
controler.addOverridingModule(builder.build());
```

For information to implement your own estimators refer to `TripEstimator` and `LegEsimator`

## Plan strategies

All new plan strategies are available as constants in `InformedModeChoiceModule`.

Replanning strategies can be added as usual either in the config or via code, e.g:
```java
strategies.add(new StrategyConfigGroup.StrategySettings()
        .setStrategyName(InformedModeChoiceModule.SELECT_SUBTOUR_MODE_STRATEGY)
        .setSubpopulation("person")
        setWeight(0.1));
```

## Config

There is a dedicated config group named `InformedModeChoiceConfigGroup`, in which the annealing and used pruner can be configured.
Please refer to the comments there for more information.