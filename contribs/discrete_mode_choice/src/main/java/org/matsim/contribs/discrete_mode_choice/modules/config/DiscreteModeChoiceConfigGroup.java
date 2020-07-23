package org.matsim.contribs.discrete_mode_choice.modules.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel.FallbackBehaviour;
import org.matsim.contribs.discrete_mode_choice.modules.ConstraintModule;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.contribs.discrete_mode_choice.modules.EstimatorModule;
import org.matsim.contribs.discrete_mode_choice.modules.FilterModule;
import org.matsim.contribs.discrete_mode_choice.modules.HomeFinderModule;
import org.matsim.contribs.discrete_mode_choice.modules.ModeAvailabilityModule;
import org.matsim.contribs.discrete_mode_choice.modules.ModelModule;
import org.matsim.contribs.discrete_mode_choice.modules.SelectorModule;
import org.matsim.contribs.discrete_mode_choice.modules.TourFinderModule;
import org.matsim.contribs.discrete_mode_choice.modules.ModelModule.ModelType;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.Tuple;

/**
 * Main config group for the DiscreteModeChoice extension.
 * 
 * @author sebhoerl
 */
public class DiscreteModeChoiceConfigGroup extends ReflectiveConfigGroup {
	private boolean performReroute = true;
	private boolean enforceSinglePlan = false;
	private boolean accumulateEstimationDelays = true;

	private ModelModule.ModelType modelType = ModelModule.ModelType.Tour;
	private DiscreteModeChoiceModel.FallbackBehaviour fallbackBehaviour = DiscreteModeChoiceModel.FallbackBehaviour.EXCEPTION;

	private String modeAvailability = ModeAvailabilityModule.CAR;
	private String tourFinder = TourFinderModule.ACTIVITY_BASED;
	private String homeFinder = HomeFinderModule.ACTIVITY_BASED;
	private String selector = SelectorModule.RANDOM;

	private Collection<String> tourConstraints = new HashSet<>(Arrays.asList(ConstraintModule.VEHICLE_CONTINUITY));
	private Collection<String> tripConstraints = new HashSet<>(Arrays.asList(ConstraintModule.VEHICLE_CONTINUITY));

	private String tourEstimator = EstimatorModule.UNIFORM;
	private String tripEstimator = EstimatorModule.UNIFORM;

	private Collection<String> tourFilters = new HashSet<>();
	private Collection<String> tripFilters = new HashSet<>();

	private Collection<String> cachedModes = new HashSet<>();

	public static final String GROUP_NAME = "DiscreteModeChoice";

	public static final String PERFORM_REROUTE = "performReroute";
	public static final String ENFORCE_SINGLE_PLAN = "enforceSinglePlan";
	public static final String FALLBACK_BEHAVIOUR = "fallbackBehaviour";
	public static final String ACCUMULATE_ESTIMATION_DELAYS = "accumulateEstimationDelays";

	public static final String MODEL_TYPE = "modelType";

	public static final String MODE_AVAILABILITY = "modeAvailability";
	public static final String TOUR_FINDER = "tourFinder";
	public static final String HOME_FINDER = "homeFinder";
	public static final String SELECTOR = "selector";

	public static final String TOUR_CONSTRAINTS = "tourConstraints";
	public static final String TRIP_CONSTRAINTS = "tripConstraints";

	public static final String TOUR_CONSTRAINT = "tourConstraint";
	public static final String TRIP_CONSTRAINT = "tripConstraint";

	public static final String TOUR_ESTIMATOR = "tourEstimator";
	public static final String TRIP_ESTIMATOR = "tripEstimator";

	public static final String TOUR_FILTERS = "tourFilters";
	public static final String TRIP_FILTERS = "tripFilters";

	public static final String TOUR_FILTER = "tourFilter";
	public static final String TRIP_FILTER = "tripFilter";

	public static final String CACHED_MODES = "cachedModes";

	public DiscreteModeChoiceConfigGroup() {
		super(GROUP_NAME);
	}

	@StringSetter(PERFORM_REROUTE)
	public void setPerformReroute(boolean performReroute) {
		this.performReroute = performReroute;
	}

	@StringGetter(PERFORM_REROUTE)
	public boolean getPerformReroute() {
		return performReroute;
	}

	@StringSetter(ENFORCE_SINGLE_PLAN)
	public void setEnforceSinglePlan(boolean enforceSinglePlan) {
		this.enforceSinglePlan = enforceSinglePlan;
	}

	@StringGetter(ENFORCE_SINGLE_PLAN)
	public boolean getEnforceSinglePlan() {
		return enforceSinglePlan;
	}

	@StringSetter(ACCUMULATE_ESTIMATION_DELAYS)
	public void setAccumulateEstimationDelays(boolean accumulateEstimationDelays) {
		this.accumulateEstimationDelays = accumulateEstimationDelays;
	}

	@StringGetter(ACCUMULATE_ESTIMATION_DELAYS)
	public boolean getAccumulateEstimationDelays() {
		return accumulateEstimationDelays;
	}

	@StringSetter(FALLBACK_BEHAVIOUR)
	public void setFallbackBehaviour(DiscreteModeChoiceModel.FallbackBehaviour fallbackBehaviour) {
		this.fallbackBehaviour = fallbackBehaviour;
	}

	@StringGetter(FALLBACK_BEHAVIOUR)
	public DiscreteModeChoiceModel.FallbackBehaviour getFallbackBehaviour() {
		return fallbackBehaviour;
	}

	@StringSetter(MODEL_TYPE)
	public void setModelType(ModelModule.ModelType modelType) {
		this.modelType = modelType;
	}

	@StringGetter(MODEL_TYPE)
	public ModelModule.ModelType getModelType() {
		return modelType;
	}

	@StringSetter(MODE_AVAILABILITY)
	public void setModeAvailability(String modeAvailability) {
		this.modeAvailability = modeAvailability;
	}

	@StringGetter(MODE_AVAILABILITY)
	public String getModeAvailability() {
		return modeAvailability;
	}

	@StringSetter(TOUR_FINDER)
	public void setTourFinder(String tourFinder) {
		this.tourFinder = tourFinder;
	}

	@StringGetter(TOUR_FINDER)
	public String getTourFinder() {
		return tourFinder;
	}

	@StringSetter(HOME_FINDER)
	public void setHomeFinder(String homeFinder) {
		this.homeFinder = homeFinder;
	}

	@StringGetter(HOME_FINDER)
	public String getHomeFinder() {
		return homeFinder;
	}

	@StringSetter(SELECTOR)
	public void setSelector(String selector) {
		this.selector = selector;
	}

	@StringGetter(SELECTOR)
	public String getSelector() {
		return selector;
	}

	public void setTourFilters(Collection<String> tourFilters) {
		this.tourFilters = new HashSet<>(tourFilters);
	}

	public Collection<String> getTourFilters() {
		return tourFilters;
	}

	@StringSetter(TOUR_FILTERS)
	public void setTourFiltersAsString(String tourFilters) {
		this.tourFilters = Arrays.asList(tourFilters.split(",")).stream().map(String::trim).collect(Collectors.toSet());
	}

	@StringGetter(TOUR_FILTERS)
	public String getTourFiltersAsString() {
		return String.join(", ", tourFilters);
	}

	public void setTripFilters(Collection<String> tripFilters) {
		this.tripFilters = new HashSet<>(tripFilters);
	}

	public Collection<String> getTripFilters() {
		return tripFilters;
	}

	@StringSetter(TRIP_FILTERS)
	public void setTripFiltersAsString(String tripFilters) {
		this.tripFilters = Arrays.asList(tripFilters.split(",")).stream().map(String::trim).collect(Collectors.toSet());
	}

	@StringGetter(TRIP_FILTERS)
	public String getTripFiltersAsString() {
		return String.join(", ", tripFilters);
	}

	@StringSetter(TOUR_ESTIMATOR)
	public void setTourEstimator(String tourEstimator) {
		this.tourEstimator = tourEstimator;
	}

	@StringGetter(TOUR_ESTIMATOR)
	public String getTourEstimator() {
		return tourEstimator;
	}

	@StringSetter(TRIP_ESTIMATOR)
	public void setTripEstimator(String tripEstimator) {
		this.tripEstimator = tripEstimator;
	}

	@StringGetter(TRIP_ESTIMATOR)
	public String getTripEstimator() {
		return tripEstimator;
	}

	public void setTourConstraints(Collection<String> tourConstraints) {
		this.tourConstraints = new HashSet<>(tourConstraints);
	}

	public Collection<String> getTourConstraints() {
		return tourConstraints;
	}

	@StringSetter(TOUR_CONSTRAINTS)
	public void setTourConstraintsAsString(String tourConstraints) {
		this.tourConstraints = Arrays.asList(tourConstraints.split(",")).stream().map(String::trim)
				.collect(Collectors.toSet());
	}

	@StringGetter(TOUR_CONSTRAINTS)
	public String getTourConstraintsAsString() {
		return String.join(", ", tourConstraints);
	}

	public void setTripConstraints(Collection<String> tripConstraints) {
		this.tripConstraints = new HashSet<>(tripConstraints);
	}

	public Collection<String> getTripConstraints() {
		return tripConstraints;
	}

	@StringSetter(TRIP_CONSTRAINTS)
	public void setTripConstraintsAsString(String tripConstraints) {
		this.tripConstraints = Arrays.asList(tripConstraints.split(",")).stream().map(String::trim)
				.collect(Collectors.toSet());
	}

	@StringGetter(TRIP_CONSTRAINTS)
	public String getTripConstraintsAsString() {
		return String.join(", ", tripConstraints);
	}

	public void setCachedModes(Collection<String> cachedModes) {
		this.cachedModes = new HashSet<>(cachedModes);
	}

	public Collection<String> getCachedModes() {
		return cachedModes;
	}

	@StringSetter(CACHED_MODES)
	public void setCachedModesAsString(String cachedModes) {
		this.cachedModes = Arrays.asList(cachedModes.split(",")).stream().map(String::trim).collect(Collectors.toSet());
	}

	@StringGetter(CACHED_MODES)
	public String getCachedModesAsString() {
		return String.join(", ", cachedModes);
	}

	// --- Component configuration ---

	private final Map<Tuple<String, String>, ConfigGroup> componentRegistry = createComponentRegistry(
			createComponentSupplierRegistry());

	private static interface ComponentSupplier {
		ConfigGroup create(String componentType, String componentName);
	}

	/**
	 * Here all components that have their own parameter set should be added.
	 */
	private Map<Tuple<String, String>, ComponentSupplier> createComponentSupplierRegistry() {
		Map<Tuple<String, String>, ComponentSupplier> registry = new HashMap<>();

		registry.put(new Tuple<>(TOUR_FINDER, TourFinderModule.ACTIVITY_BASED), //
				ActivityTourFinderConfigGroup::new);
		registry.put(new Tuple<>(HOME_FINDER, HomeFinderModule.ACTIVITY_BASED), //
				ActivityHomeFinderConfigGroup::new);
		registry.put(new Tuple<>(MODE_AVAILABILITY, ModeAvailabilityModule.DEFAULT), //
				ModeAvailabilityConfigGroup::new);
		registry.put(new Tuple<>(MODE_AVAILABILITY, ModeAvailabilityModule.CAR), //
				ModeAvailabilityConfigGroup::new);
		registry.put(new Tuple<>(SELECTOR, SelectorModule.MULTINOMIAL_LOGIT), //
				MultinomialLogitSelectorConfigGroup::new);
		registry.put(new Tuple<>(TRIP_CONSTRAINT, ConstraintModule.LINK_ATTRIBUTE), //
				LinkAttributeConstraintConfigGroup::new);
		registry.put(new Tuple<>(TRIP_CONSTRAINT, ConstraintModule.SHAPE_FILE), //
				ShapeFileConstraintConfigGroup::new);
		registry.put(new Tuple<>(TRIP_CONSTRAINT, ConstraintModule.VEHICLE_CONTINUITY), //
				VehicleTripConstraintConfigGroup::new);
		registry.put(new Tuple<>(TOUR_CONSTRAINT, ConstraintModule.VEHICLE_CONTINUITY), //
				VehicleTourConstraintConfigGroup::new);
		registry.put(new Tuple<>(TOUR_CONSTRAINT, ConstraintModule.SUBTOUR_MODE), //
				SubtourModeConstraintConfigGroup::new);
		registry.put(new Tuple<>(TRIP_ESTIMATOR, EstimatorModule.MATSIM_TRIP_SCORING), //
				MATSimTripScoringConfigGroup::new);
		registry.put(new Tuple<>(TOUR_FILTER, FilterModule.TOUR_LENGTH), //
				TourLengthFilterConfigGroup::new);

		return registry;
	}

	private Map<Tuple<String, String>, ConfigGroup> createComponentRegistry(
			Map<Tuple<String, String>, ComponentSupplier> componentSupplierRegistry) {
		Map<Tuple<String, String>, ConfigGroup> registry = new HashMap<>();

		for (Map.Entry<Tuple<String, String>, ComponentSupplier> entry : componentSupplierRegistry.entrySet()) {
			ConfigGroup componentConfig = entry.getValue().create(entry.getKey().getFirst(),
					entry.getKey().getSecond());
			registry.put(entry.getKey(), componentConfig);
			super.addParameterSet(componentConfig);
		}

		return registry;
	}

	@Override
	public ConfigGroup createParameterSet(String parameterSetType) {
		List<String> segments = Arrays.asList(parameterSetType.split(":")).stream().map(String::trim)
				.collect(Collectors.toList());

		if (segments.size() == 2) {
			String componentType = segments.get(0);
			String componentName = segments.get(1);

			return getComponentConfig(componentType, componentName);
		} else {
			throw new IllegalStateException(String.format(
					"Wrongly formatted component: %s (shoud be 'componentType:componentName')", parameterSetType));
		}
	}

	@Override
	public void addParameterSet(ConfigGroup set) {
		if (!componentRegistry.containsValue(set)) {
			throw new IllegalStateException("Attempt to add unknown parameter set to DiscreteModeChoiceConfigGroup");
		}
	}

	public ConfigGroup getComponentConfig(String componentType, String componentName) {
		Tuple<String, String> key = new Tuple<>(componentType, componentName);

		if (componentRegistry.containsKey(key)) {
			return componentRegistry.get(key);
		} else {
			throw new IllegalStateException(String.format("Unknown component configuration of type '%s' and name '%s'.",
					componentType, componentName));
		}
	}

	public ActivityTourFinderConfigGroup getActivityTourFinderConfigGroup() {
		return (ActivityTourFinderConfigGroup) getComponentConfig(TOUR_FINDER, TourFinderModule.ACTIVITY_BASED);
	}

	public ActivityHomeFinderConfigGroup getActivityHomeFinderConfigGroup() {
		return (ActivityHomeFinderConfigGroup) getComponentConfig(HOME_FINDER, HomeFinderModule.ACTIVITY_BASED);
	}

	public ModeAvailabilityConfigGroup getDefaultModeAvailabilityConfig() {
		return (ModeAvailabilityConfigGroup) getComponentConfig(MODE_AVAILABILITY, ModeAvailabilityModule.DEFAULT);
	}

	public ModeAvailabilityConfigGroup getCarModeAvailabilityConfig() {
		return (ModeAvailabilityConfigGroup) getComponentConfig(MODE_AVAILABILITY, ModeAvailabilityModule.CAR);
	}

	public MultinomialLogitSelectorConfigGroup getMultinomialLogitSelectorConfig() {
		return (MultinomialLogitSelectorConfigGroup) getComponentConfig(SELECTOR, SelectorModule.MULTINOMIAL_LOGIT);
	}

	public LinkAttributeConstraintConfigGroup getLinkAttributeConstraintConfigGroup() {
		return (LinkAttributeConstraintConfigGroup) getComponentConfig(TRIP_CONSTRAINT,
				ConstraintModule.LINK_ATTRIBUTE);
	}

	public ShapeFileConstraintConfigGroup getShapeFileConstraintConfigGroup() {
		return (ShapeFileConstraintConfigGroup) getComponentConfig(TRIP_CONSTRAINT, ConstraintModule.SHAPE_FILE);
	}

	public VehicleTripConstraintConfigGroup getVehicleTripConstraintConfig() {
		return (VehicleTripConstraintConfigGroup) getComponentConfig(TRIP_CONSTRAINT,
				ConstraintModule.VEHICLE_CONTINUITY);
	}

	public VehicleTourConstraintConfigGroup getVehicleTourConstraintConfig() {
		return (VehicleTourConstraintConfigGroup) getComponentConfig(TOUR_CONSTRAINT,
				ConstraintModule.VEHICLE_CONTINUITY);
	}

	public SubtourModeConstraintConfigGroup getSubtourConstraintConfig() {
		return (SubtourModeConstraintConfigGroup) getComponentConfig(TOUR_CONSTRAINT, ConstraintModule.SUBTOUR_MODE);
	}

	public MATSimTripScoringConfigGroup getMATSimTripScoringConfigGroup() {
		return (MATSimTripScoringConfigGroup) getComponentConfig(TRIP_ESTIMATOR, EstimatorModule.MATSIM_TRIP_SCORING);
	}

	public TourLengthFilterConfigGroup getTourLengthFilterConfigGroup() {
		return (TourLengthFilterConfigGroup) getComponentConfig(TOUR_FILTER, FilterModule.TOUR_LENGTH);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = new HashMap<>();

		String options = Arrays.asList(ModelType.values()).stream().map(String::valueOf)
				.collect(Collectors.joining(", "));
		comments.put(MODEL_TYPE, "Main model type: " + options);

		comments.put(PERFORM_REROUTE, "Defines whether the " + DiscreteModeChoiceModule.STRATEGY_NAME
				+ " strategy should be followed by a rerouting of all trips. If the estimator returns alternatives with routes attached this is not necessary.");
		comments.put(ENFORCE_SINGLE_PLAN,
				"Defines whether to run a runtime check that verifies that everything is set up correctl for a 'mode-choice-in-the-loop' setup.");

		options = Arrays.asList(FallbackBehaviour.values()).stream().map(String::valueOf)
				.collect(Collectors.joining(", "));
		comments.put(FALLBACK_BEHAVIOUR,
				"Defines what happens if there is no feasible choice alternative for an agent: " + options);

		comments.put(MODE_AVAILABILITY, "Defines which ModeAvailability component to use. Built-in choices: "
				+ String.join(", ", ModeAvailabilityModule.COMPONENTS));
		comments.put(TOUR_FINDER, "Defines which TourFinder component to use. Built-in choices: "
				+ String.join(", ", TourFinderModule.COMPONENTS));
		comments.put(HOME_FINDER, "Defines how home activities are identified. Built-in choices: "
				+ String.join(", ", HomeFinderModule.COMPONENTS));
		comments.put(SELECTOR, "Defines which Selector component to use. Built-in choices: "
				+ String.join(", ", SelectorModule.COMPONENTS));
		comments.put(TOUR_CONSTRAINTS,
				"Defines a number of TourConstraint components that should be activated. Built-in choices: "
						+ String.join(", ", ConstraintModule.TOUR_COMPONENTS));
		comments.put(TRIP_CONSTRAINTS,
				"Defines a number of TripConstraint components that should be activated. Built-in choices: "
						+ String.join(", ", ConstraintModule.TRIP_COMPONENTS));
		comments.put(TOUR_ESTIMATOR, "Defines which TourEstimator component to use. Built-in choices: "
				+ String.join(", ", EstimatorModule.TOUR_COMPONENTS));
		comments.put(TRIP_ESTIMATOR, "Defines which TripEstimator component to use. Built-in choices: "
				+ String.join(", ", EstimatorModule.TRIP_COMPONENTS));
		comments.put(TOUR_FILTERS,
				"Defines a number of TourFilter components that should be activated. Built-in choices: "
						+ String.join(", ", FilterModule.TOUR_COMPONENTS));
		comments.put(TRIP_FILTERS,
				"Defines a number of TripFilter components that should be activated. Built-in choices: "
						+ String.join(", ", FilterModule.TRIP_COMPONENTS));
		comments.put(CACHED_MODES,
				"Trips tested with the modes listed here will be cached for each combination of trip and agent during one replanning pass.");

		return comments;
	}

	static public DiscreteModeChoiceConfigGroup getOrCreate(Config config) {
		DiscreteModeChoiceConfigGroup configGroup = (DiscreteModeChoiceConfigGroup) config.getModules().get(GROUP_NAME);

		if (configGroup == null) {
			configGroup = new DiscreteModeChoiceConfigGroup();
			config.addModule(configGroup);
		}

		return configGroup;
	}
}
