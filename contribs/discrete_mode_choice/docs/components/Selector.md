# Selector

A `Selector` component has the task to make a decision between a number of alternatives, either for a trip or for a tour. In some cases, this may be utility-based, in some cases this could be purely random or dependent on other factors.

The DMC extension contains a number of predefined `Selector` implementations, but it is also possible to write custom ones. How to do that is explained in [Customizing the model](docs/Customizing.md).

In the following the existing built-in selectors are described. All of them exist in a trip-based and tour-based version. While some of them have additional configuration options that can be defined in a `parameterset`, some don't. In any case, the selector can be chosen in the main config group:

```xml
<module name="DiscreteModeChoice">
	<!-- Defines which Selector component to use. Built-in choices: ... -->
	<param name="selector" value="Maximum" />
</module>
```

## Maximum

*Description:* The `Maximum` selector choose the alternative with the highest estimated utility. This makes the selector also memory-efficient: At any point in time only the alternative with the highest utility that is has seen is kept.

*Configuration:*
No specific configuration available.

## MultinomialLogit

*Description:* The `MultinomialLogit` selector performs a probability-based selection of all the alternatives it has seen. Given the utilities `u_i` of each alternative `i`, a choice probability is calculated according to:

```
P(i) = exp(u_i) / [ exp(u_1) + exp(u_2) + ... + exp(u_N) ] 
```

Once the probabilities are calculated, one alternative is sampled based on the obtained probability density.

There is a number of things that need to be taken into account:
- Sometimes, utilities can exceed a certain large number, leading the exponential term to become infinity. In those cases, it is common practice to truncate the utility. Here, the `maximumUtility` option is used to define a number. Whenever it is exceeded, the utility of the given alternative will be cropped and a warning will be shown.
- In some cases, very low utilities can occur. In those cases it may be desirable not to consider them altogether, because they generally introduce bias into the selection problem. For that purpose it is optionally (`considerMinimumUtility`) possible to filter out those alternatives that have very low utilities. The threshold is defined as `minimumUtility`.

*Configuration:*

```xml
<parameterset type="selector:MultinomialLogit" >
	<!-- Defines whether candidates with a utility lower than the minimum utility should be filtered out. -->
	<param name="considerMinimumUtility" value="false" />
	<!-- Candidates with a utility above that threshold will be cut off to this value. -->
	<param name="maximumUtility" value="700.0" />
	<!-- Candidates with a utility lower than that threshold will not be considered by default. -->
	<param name="minimumUtility" value="-700.0" />
</parameterset>
```

## Random

*Description:* The random selector collects all possible alternatives for a choice and then selects one at random.

*Configuration:*
No specific configuration available.
