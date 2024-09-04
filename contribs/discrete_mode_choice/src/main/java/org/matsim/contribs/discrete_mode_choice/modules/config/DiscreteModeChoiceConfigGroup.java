package org.matsim.contribs.discrete_mode_choice.modules.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.constraints.Positive;
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
	@Positive
	private int writeUtilitiesInterval = 1;
	public static final String GROUP_NAME = "DiscreteModeChoice";

	public static final String PERFORM_REROUTE = "performReroute";
	private static final String PERFORM_REROUTE_CMT = "Defines whether the " + DiscreteModeChoiceModule.STRATEGY_NAME
							   + " strategy should be followed by a rerouting of all trips. If the estimator returns alternatives with routes attached this is not necessary.";

	public static final String ENFORCE_SINGLE_PLAN = "enforceSinglePlan";
	private static final String ENFORCE_SINGLE_PLAN_CMT = "Defines whether to run a runtime check that verifies that everything is set up correctl for a 'mode-choice-in-the-loop' setup.";

	public static final String FALLBACK_BEHAVIOUR = "fallbackBehaviour";
	private static final String FALLBACK_BEHAVIOR_CMT = "Defines what happens if there is no feasible choice alternative for an agent: ";

	public static final String ACCUMULATE_ESTIMATION_DELAYS = "accumulateEstimationDelays";

	public static final String MODEL_TYPE = "modelType";
	private static final String MODEL_TYPE_CMT = "Main model type: ";

	public static final String MODE_AVAILABILITY = "modeAvailability";
	private static final String MODE_AVAILABILITY_CMT = "Defines which ModeAvailability component to use. Built-in choices: ";

	public static final String TOUR_FINDER = "tourFinder";
	private static final String TOUR_FINDER_CMT = "Defines which TourFinder component to use. Built-in choices: ";

	public static final String HOME_FINDER = "homeFinder";
	private static final String HOME_FINDER_CMT = "Defines how home activities are identified. Built-in choices: " ;

	public static final String SELECTOR = "selector";
	private static final String SELECTOR_CMT = "Defines which Selector component to use. Built-in choices: ";

	public static final String TOUR_CONSTRAINTS = "tourConstraints";
	private static final String TOUR_CONSTRAINTS_CMT = "Defines a number of TourConstraint components that should be activated. Built-in choices: ";

	public static final String TRIP_CONSTRAINTS = "tripConstraints";
	private static final String TRIP_CONSTRAINTS_CMT = "Defines a number of TripConstraint components that should be activated. Built-in choices: ";

	public static final String TOUR_CONSTRAINT = "tourConstraint";
	public static final String TRIP_CONSTRAINT = "tripConstraint";

	public static final String TOUR_ESTIMATOR = "tourEstimator";
	private static final String TOUR_ESTIMATOR_CMT = "Defines which TourEstimator component to use. Built-in choices: ";

	public static final String TRIP_ESTIMATOR = "tripEstimator";
	private static final String TRIP_ESTIMATOR_CMT = "Defines which TripEstimator component to use. Built-in choices: ";

	public static final String TOUR_FILTERS = "tourFilters";
	private static final String TOUR_FILTERS_CMT = "Defines a number of TourFilter components that should be activated. Built-in choices: ";

	public static final String TRIP_FILTERS = "tripFilters";
	private static final String TRIP_FILTERS_CMT = "Defines a number of TripFilter components that should be activated. Built-in choices: ";

	public static final String TOUR_FILTER = "tourFilter";
	public static final String TRIP_FILTER = "tripFilter";

	public static final String CACHED_MODES = "cachedModes";
	public static final String CACHED_MODES_CMT = "Trips tested with the modes listed here will be cached for each combination of trip and agent during one replanning pass.";

	public static final String WRITE_UTILITIES_INTERVAL = "writeUtilitiesInterval";
	public static final String WRITE_UTILITIES_INTERVAL_CMT = "Specifies the interval, in iterations, at which the dmc_utilities.csv file is written. If set to 0, the file is written only at the end of the simulation";

	public DiscreteModeChoiceConfigGroup() {
		super(GROUP_NAME);
	}

	/**
	 * @param performReroute -- {@value #PERFORM_REROUTE_CMT}
	 */
	@StringSetter(PERFORM_REROUTE)
	public void setPerformReroute(boolean performReroute) {
		this.performReroute = performReroute;
	}

	/**
	 * @return -- {@value #PERFORM_REROUTE_CMT}
	 */
	@StringGetter(PERFORM_REROUTE)
	public boolean getPerformReroute() {
		return performReroute;
	}

	/**
	 * @param enforceSinglePlan -- {@value #ENFORCE_SINGLE_PLAN_CMT}
	 */
	@StringSetter(ENFORCE_SINGLE_PLAN)
	public void setEnforceSinglePlan(boolean enforceSinglePlan) {
		this.enforceSinglePlan = enforceSinglePlan;
	}

	/**
	 * @return -- {@value #ENFORCE_SINGLE_PLAN_CMT}
	 */
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

	/**
	 * @param fallbackBehaviour -- {@value #FALLBACK_BEHAVIOR_CMT}
	 */
	@StringSetter(FALLBACK_BEHAVIOUR)
	public void setFallbackBehaviour(DiscreteModeChoiceModel.FallbackBehaviour fallbackBehaviour) {
		this.fallbackBehaviour = fallbackBehaviour;
	}

	/**
	 * @return -- {@value #FALLBACK_BEHAVIOR_CMT}
	 */
	@StringGetter(FALLBACK_BEHAVIOUR)
	public DiscreteModeChoiceModel.FallbackBehaviour getFallbackBehaviour() {
		return fallbackBehaviour;
	}

	/**
	 * @param modelType -- {@value #MODEL_TYPE_CMT}
	 */
	@StringSetter(MODEL_TYPE)
	public void setModelType(ModelModule.ModelType modelType) {
		this.modelType = modelType;
	}

	/**
	 * @return -- {@value #MODEL_TYPE_CMT}
	 */
	@StringGetter(MODEL_TYPE)
	public ModelModule.ModelType getModelType() {
		return modelType;
	}

	/**
	 * @param modeAvailability -- {{@value #MODE_AVAILABILITY_CMT}}
	 */
	@StringSetter(MODE_AVAILABILITY)
	public void setModeAvailability(String modeAvailability) {
		this.modeAvailability = modeAvailability;
	}

	/**
	 * @return -- {@value #MODE_AVAILABILITY_CMT}
	 */
	@StringGetter(MODE_AVAILABILITY)
	public String getModeAvailability() {
		return modeAvailability;
	}

	/**
	 * @param tourFinder -- {@value #TOUR_FINDER_CMT}
	 */
	@StringSetter(TOUR_FINDER)
	public void setTourFinder(String tourFinder) {
		this.tourFinder = tourFinder;
	}

	/**
	 * @return -- {@value #TOUR_FINDER_CMT}
	 */
	@StringGetter(TOUR_FINDER)
	public String getTourFinder() {
		return tourFinder;
	}

	/**
	 * @param homeFinder -- {@value #HOME_FINDER_CMT}
	 */
	@StringSetter(HOME_FINDER)
	public void setHomeFinder(String homeFinder) {
		this.homeFinder = homeFinder;
	}

	/**
	 * @return -- {@value #HOME_FINDER_CMT}
	 */
	@StringGetter(HOME_FINDER)
	public String getHomeFinder() {
		return homeFinder;
	}

	/**
	 * @param selector -- {@value #SELECTOR_CMT}
	 */
	@StringSetter(SELECTOR)
	public void setSelector(String selector) {
		this.selector = selector;
	}

	/**
	 * @return -- {@value #SELECTOR_CMT}
	 */
	@StringGetter(SELECTOR)
	public String getSelector() {
		return selector;
	}

	/**
	 * @param tourFilters  -- {@value #TOUR_FILTERS_CMT}
	 */
	public void setTourFilters(Collection<String> tourFilters) {
		this.tourFilters = new HashSet<>(tourFilters);
	}

	/**
	 * @return  -- {@value #TOUR_FILTERS_CMT}
	 */
	public Collection<String> getTourFilters() {
		return tourFilters;
	}

	/**
	 * @param tourFilters -- {@value #TOUR_FILTERS_CMT}
	 */
	@StringSetter(TOUR_FILTERS)
	public void setTourFiltersAsString(String tourFilters) {
		this.tourFilters = Arrays.asList(tourFilters.split(",")).stream().map(String::trim).collect(Collectors.toSet());
	}

	/**
	 * @return  -- {@value #TOUR_FILTERS_CMT}
	 */
	@StringGetter(TOUR_FILTERS)
	public String getTourFiltersAsString() {
		return String.join(", ", tourFilters);
	}

	/**
	 * @param tripFilters  -- {@value #TRIP_FILTERS_CMT}
	 */
	public void setTripFilters(Collection<String> tripFilters) {
		this.tripFilters = new HashSet<>(tripFilters);
	}

	/**
	 * @return  -- {@value #TRIP_FILTERS_CMT}
	 */
	public Collection<String> getTripFilters() {
		return tripFilters;
	}

	/**
	 * @param tripFilters -- {@value #TRIP_FILTERS_CMT}
	 */
	@StringSetter(TRIP_FILTERS)
	public void setTripFiltersAsString(String tripFilters) {
		this.tripFilters = Arrays.asList(tripFilters.split(",")).stream().map(String::trim).collect(Collectors.toSet());
	}

	/**
	 * @return  -- {@value #TRIP_FILTERS_CMT}
	 */
	@StringGetter(TRIP_FILTERS)
	public String getTripFiltersAsString() {
		return String.join(", ", tripFilters);
	}

	/**
	 * @param tourEstimator -- {@value #TOUR_ESTIMATOR_CMT}
	 */
	@StringSetter(TOUR_ESTIMATOR)
	public void setTourEstimator(String tourEstimator) {
		this.tourEstimator = tourEstimator;
	}

	/**
	 * @return  -- {@value #TOUR_ESTIMATOR_CMT}
	 */
	@StringGetter(TOUR_ESTIMATOR)
	public String getTourEstimator() {
		return tourEstimator;
	}

	/**
	 * @param tripEstimator -- {@value #TRIP_ESTIMATOR_CMT}
	 */
	@StringSetter(TRIP_ESTIMATOR)
	public void setTripEstimator(String tripEstimator) {
		this.tripEstimator = tripEstimator;
	}

	/**
	 * @return  -- {@value #TRIP_ESTIMATOR_CMT}
	 */
	@StringGetter(TRIP_ESTIMATOR)
	public String getTripEstimator() {
		return tripEstimator;
	}

	/**
	 * @param tourConstraints -- {@value #TOUR_CONSTRAINTS_CMT}
	 */
	public void setTourConstraints(Collection<String> tourConstraints) {
		this.tourConstraints = new HashSet<>(tourConstraints);
	}

	/**
	 * @return -- {@value #TOUR_CONSTRAINTS_CMT}
	 */
	public Collection<String> getTourConstraints() {
		return tourConstraints;
	}

	/**
	 * @param tourConstraints -- {@value #TOUR_CONSTRAINTS_CMT}
	 */
	@StringSetter(TOUR_CONSTRAINTS)
	public void setTourConstraintsAsString(String tourConstraints) {
		this.tourConstraints = Arrays.asList(tourConstraints.split(",")).stream().map(String::trim)
				.collect(Collectors.toSet());
	}

	/**
	 * @return -- {@value #TOUR_CONSTRAINTS_CMT}
	 */
	@StringGetter(TOUR_CONSTRAINTS)
	public String getTourConstraintsAsString() {
		return String.join(", ", tourConstraints);
	}

	/**
	 * @param tripConstraints  -- {@value #TRIP_CONSTRAINTS_CMT}
	 */
	public void setTripConstraints(Collection<String> tripConstraints) {
		this.tripConstraints = new HashSet<>(tripConstraints);
	}

	/**
	 * @return  -- {@value #TRIP_CONSTRAINTS_CMT}
	 */
	public Collection<String> getTripConstraints() {
		return tripConstraints;
	}

	/**
	 * @param tripConstraints -- {@value #TRIP_CONSTRAINTS_CMT}
	 */
	@StringSetter(TRIP_CONSTRAINTS)
	public void setTripConstraintsAsString(String tripConstraints) {
		this.tripConstraints = Arrays.asList(tripConstraints.split(",")).stream().map(String::trim)
				.collect(Collectors.toSet());
	}

	/**
	 * @return  -- {@value #TRIP_CONSTRAINTS_CMT}
	 */
	@StringGetter(TRIP_CONSTRAINTS)
	public String getTripConstraintsAsString() {
		return String.join(", ", tripConstraints);
	}

	/**
	 * @param cachedModes  -- {@value #CACHED_MODES_CMT}
	 */
	public void setCachedModes(Collection<String> cachedModes) {
		this.cachedModes = new HashSet<>(cachedModes);
	}

	/**
	 * @return  -- {@value #CACHED_MODES_CMT}
	 */
	public Collection<String> getCachedModes() {
		return cachedModes;
	}

	/**
	 * @param cachedModes -- {@value #CACHED_MODES_CMT}
	 */
	@StringSetter(CACHED_MODES)
	public void setCachedModesAsString(String cachedModes) {
		this.cachedModes = Arrays.asList(cachedModes.split(",")).stream().map(String::trim).collect(Collectors.toSet());
	}

	/**
	 * @return  -- {@value #CACHED_MODES_CMT}
	 */
	@StringGetter(CACHED_MODES)
	public String getCachedModesAsString() {
		return String.join(", ", cachedModes);
	}

	/**
	 * @param writeUtilitiesInterval -- {@value #WRITE_UTILITIES_INTERVAL_CMT}
	 */
	@StringSetter(WRITE_UTILITIES_INTERVAL)
	public void setWriteUtilitiesInterval(int writeUtilitiesInterval) {
		this.writeUtilitiesInterval = writeUtilitiesInterval;
	}

	/**
	 * @return -- {@value #WRITE_UTILITIES_INTERVAL_CMT}
	 */
	@StringGetter(WRITE_UTILITIES_INTERVAL)
	public int getWriteUtilitiesInterval() {
		return this.writeUtilitiesInterval;
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

	/**
	 * See {@link ActivityTourFinderConfigGroup}
	 */
	public ActivityTourFinderConfigGroup getActivityTourFinderConfigGroup() {
		return (ActivityTourFinderConfigGroup) getComponentConfig(TOUR_FINDER, TourFinderModule.ACTIVITY_BASED);
	}

	/**
	 * See {@link ActivityHomeFinderConfigGroup}
	 */
	public ActivityHomeFinderConfigGroup getActivityHomeFinderConfigGroup() {
		return (ActivityHomeFinderConfigGroup) getComponentConfig(HOME_FINDER, HomeFinderModule.ACTIVITY_BASED);
	}

	/**
	 * See {@link ModeAvailabilityConfigGroup}
	 */
	public ModeAvailabilityConfigGroup getDefaultModeAvailabilityConfig() {
		return (ModeAvailabilityConfigGroup) getComponentConfig(MODE_AVAILABILITY, ModeAvailabilityModule.DEFAULT);
	}

	/**
	 * See {@link ModeAvailabilityConfigGroup}
	 */
	public ModeAvailabilityConfigGroup getCarModeAvailabilityConfig() {
		return (ModeAvailabilityConfigGroup) getComponentConfig(MODE_AVAILABILITY, ModeAvailabilityModule.CAR);
	}

	/**
	 * See {@link MultinomialLogitSelectorConfigGroup}
	 */
	public MultinomialLogitSelectorConfigGroup getMultinomialLogitSelectorConfig() {
		return (MultinomialLogitSelectorConfigGroup) getComponentConfig(SELECTOR, SelectorModule.MULTINOMIAL_LOGIT);
	}

	/**
	 * See {@link LinkAttributeConstraintConfigGroup}
	 */
	public LinkAttributeConstraintConfigGroup getLinkAttributeConstraintConfigGroup() {
		return (LinkAttributeConstraintConfigGroup) getComponentConfig(TRIP_CONSTRAINT,
				ConstraintModule.LINK_ATTRIBUTE);
	}

	/**
	 * See {@link ShapeFileConstraintConfigGroup}
	 */
	public ShapeFileConstraintConfigGroup getShapeFileConstraintConfigGroup() {
		return (ShapeFileConstraintConfigGroup) getComponentConfig(TRIP_CONSTRAINT, ConstraintModule.SHAPE_FILE);
	}

	/**
	 * See {@link VehicleTripConstraintConfigGroup}
	 */
	public VehicleTripConstraintConfigGroup getVehicleTripConstraintConfig() {
		return (VehicleTripConstraintConfigGroup) getComponentConfig(TRIP_CONSTRAINT,
				ConstraintModule.VEHICLE_CONTINUITY);
	}

	/**
	 * See {@link VehicleTourConstraintConfigGroup}
	 */
	public VehicleTourConstraintConfigGroup getVehicleTourConstraintConfig() {
		return (VehicleTourConstraintConfigGroup) getComponentConfig(TOUR_CONSTRAINT,
				ConstraintModule.VEHICLE_CONTINUITY);
	}

	/**
	 * See {@link SubtourModeConstraintConfigGroup}
	 */
	public SubtourModeConstraintConfigGroup getSubtourConstraintConfig() {
		return (SubtourModeConstraintConfigGroup) getComponentConfig(TOUR_CONSTRAINT, ConstraintModule.SUBTOUR_MODE);
	}

	/**
	 * See {@link MATSimTripScoringConfigGroup}
	 */
	public MATSimTripScoringConfigGroup getMATSimTripScoringConfigGroup() {
		return (MATSimTripScoringConfigGroup) getComponentConfig(TRIP_ESTIMATOR, EstimatorModule.MATSIM_TRIP_SCORING);
	}

	/**
	 * See {@link TourLengthFilterConfigGroup}
	 */
	public TourLengthFilterConfigGroup getTourLengthFilterConfigGroup() {
		return (TourLengthFilterConfigGroup) getComponentConfig(TOUR_FILTER, FilterModule.TOUR_LENGTH);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = new HashMap<>();
		comments.put(MODEL_TYPE, MODEL_TYPE_CMT  + Arrays.stream(ModelType.values() ).map(String::valueOf ).collect(Collectors.joining(", " ) ) );
		comments.put(PERFORM_REROUTE, PERFORM_REROUTE_CMT );
		comments.put(ENFORCE_SINGLE_PLAN, ENFORCE_SINGLE_PLAN_CMT );
		comments.put(FALLBACK_BEHAVIOUR, FALLBACK_BEHAVIOR_CMT  + Arrays.asList(FallbackBehaviour.values() ).stream().map(String::valueOf ).collect(Collectors.joining(", " ) ) );
		comments.put(MODE_AVAILABILITY, MODE_AVAILABILITY_CMT  + String.join( ", ", ModeAvailabilityModule.COMPONENTS ) );
		comments.put(TOUR_FINDER, TOUR_FINDER_CMT  + String.join( ", ", TourFinderModule.COMPONENTS ) );
		comments.put(HOME_FINDER, HOME_FINDER_CMT + String.join( ", ", HomeFinderModule.COMPONENTS ) );
		comments.put(SELECTOR, SELECTOR_CMT  + String.join( ", ", SelectorModule.COMPONENTS ) );
		comments.put(TOUR_CONSTRAINTS, TOUR_CONSTRAINTS_CMT + String.join( ", ", ConstraintModule.TOUR_COMPONENTS ) );
		comments.put(TRIP_CONSTRAINTS, TRIP_CONSTRAINTS_CMT + String.join( ", ", ConstraintModule.TRIP_COMPONENTS ) );
		comments.put(TOUR_ESTIMATOR, TOUR_ESTIMATOR_CMT  + String.join( ", ", EstimatorModule.TOUR_COMPONENTS ) );
		comments.put(TRIP_ESTIMATOR, TRIP_ESTIMATOR_CMT + String.join( ", ", EstimatorModule.TRIP_COMPONENTS ) );
		comments.put(TOUR_FILTERS, TOUR_FILTERS_CMT  + String.join( ", ", FilterModule.TOUR_COMPONENTS ) );
		comments.put(TRIP_FILTERS, TRIP_FILTERS_CMT + String.join( ", ", FilterModule.TRIP_COMPONENTS ) );
		comments.put(CACHED_MODES, CACHED_MODES_CMT );

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
