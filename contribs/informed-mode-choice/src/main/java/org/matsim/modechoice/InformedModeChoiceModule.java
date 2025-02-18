package org.matsim.modechoice;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.modechoice.constraints.TripConstraint;
import org.matsim.modechoice.estimators.*;
import org.matsim.modechoice.pruning.CandidatePruner;
import org.matsim.modechoice.replanning.*;
import org.matsim.modechoice.search.BestChoiceGenerator;
import org.matsim.modechoice.search.SingleTripChoicesGenerator;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import java.util.*;

/**
 * The main and only module needed to install to use informed mode choice functionality.
 *
 * @see Builder
 */
public final class InformedModeChoiceModule extends AbstractModule {

	public final static String SELECT_BEST_K_PLAN_MODES_STRATEGY = "SelectBestKPlanModes";

	public final static String SELECT_SINGLE_TRIP_MODE_STRATEGY = "SelectSingleTripMode";

	public final static String SELECT_SUBTOUR_MODE_STRATEGY = "SelectSubtourMode";
	public final static String RANDOM_SUBTOUR_MODE_STRATEGY = "RandomSubtourMode";


	private final Builder builder;

	private InformedModeChoiceModule(Builder builder) {
		this.builder = builder;
	}

	/**
	 * Replaces a strategy in the config. Can be used to enable one of the strategies in this module programmatically.
	 *
	 * @param weight if not null, the weight of the strategy is set to this value.
	 */
	public static void replaceReplanningStrategy(Config config, String subpopulation,
									   String existing, String replacement, Double weight) {

		// Copy list because it is unmodifiable
		List<ReplanningConfigGroup.StrategySettings> strategies = new ArrayList<>(config.replanning().getStrategySettings());
		List<ReplanningConfigGroup.StrategySettings> found = strategies.stream()
			.filter(s -> subpopulation == null || Objects.equals(s.getSubpopulation(), subpopulation))
			.filter(s -> s.getStrategyName().equals(existing))
			.toList();

		if (found.isEmpty())
			throw new IllegalArgumentException("No strategy %s found for subpopulation %s".formatted(existing, subpopulation));

		if (subpopulation != null && found.size() > 1)
			throw new IllegalArgumentException("Multiple strategies %s found for subpopulation %s".formatted(existing, subpopulation));

		found.forEach(s -> s.setStrategyName(replacement));

		if (weight != null) {
			found.forEach(s -> s.setWeight(weight));
		}

		// reset und set new strategies
		config.replanning().clearStrategySettings();
		strategies.forEach(s -> config.replanning().addStrategySettings(s));
	}

	/**
	 * Replaces a strategy in the config.
	 */
	public static void replaceReplanningStrategy(Config config, String subpopulation,
												 String existing, String replacement) {
		replaceReplanningStrategy(config, subpopulation, existing, replacement, null);
	}

	/**
	 * Replace a strategy in the config for all subpoopulations.
	 * @see #replaceReplanningStrategy(Config, String, String, String)
	 */
	public static void replaceReplanningStrategy(Config config, String existing, String replacement) {
		replaceReplanningStrategy(config, null, existing, replacement);
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	@Override
	public void install() {

		bindAllModes(builder.fixedCosts, new TypeLiteral<>() {});
		bindAllModes(builder.legEstimators, new TypeLiteral<>() {});
		bindAllModes(builder.tripEstimators, new TypeLiteral<>() {});
		bindAllModes(builder.options, new TypeLiteral<>() {});

		bind(ActivityEstimator.class).to(builder.activityEstimator).in(Singleton.class);

		// Not singleton, they should be able to be created per thread if necessary.
		bind(EstimateRouter.class);
		bind(TopKChoicesGenerator.class);
		bind(BestChoiceGenerator.class);
		bind(SingleTripChoicesGenerator.class);
		bind(GeneratorContext.class);
		bind(EstimateCalculator.class);

		bind(PlanModelService.class).asEagerSingleton();
		addControlerListenerBinding().to(PlanModelService.class).asEagerSingleton();

		Multibinder<TripConstraint<?>> tcBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<>() {});
		for (Class<? extends TripConstraint<?>> c : builder.constraints) {
			tcBinder.addBinding().to(c).in(Singleton.class);
		}

		Multibinder<TripScoreEstimator> tripScores = Multibinder.newSetBinder(binder(), new TypeLiteral<>() {});
		for (Class<? extends TripScoreEstimator> c : builder.tripScoreEstimators) {
			tripScores.addBinding().to(c).in(Singleton.class);
		}

		MapBinder<String, CandidatePruner> pBinder = MapBinder.newMapBinder(binder(), String.class, CandidatePruner.class);
		for (Map.Entry<String, CandidatePruner> e : builder.pruner.entrySet()) {
			CandidatePruner instance = e.getValue();

			pBinder.addBinding(e.getKey()).toInstance(instance);

			if (instance instanceof ControlerListener cl)
				addControlerListenerBinding().toInstance(cl);
		}

		addPlanStrategyBinding(SELECT_BEST_K_PLAN_MODES_STRATEGY).toProvider(SelectBestKPlanModesStrategyProvider.class);
		addPlanStrategyBinding(SELECT_SINGLE_TRIP_MODE_STRATEGY).toProvider(SelectSingleTripModeStrategyProvider.class);
		addPlanStrategyBinding(SELECT_SUBTOUR_MODE_STRATEGY).toProvider(SelectSubtourModeStrategyProvider.class);
		addPlanStrategyBinding(RANDOM_SUBTOUR_MODE_STRATEGY).toProvider(RandomSubtourModeStrategyProvider.class);
		//addPlanStrategyBinding(INFORMED_MODE_CHOICE).toProvider(InformedModeChoiceStrategyProvider.class);

		// Ensure that only one instance exists
		bind(ModeChoiceWeightScheduler.class).in(Singleton.class);
		addControlerListenerBinding().to(ModeChoiceWeightScheduler.class).in(Singleton.class);
		addControlerListenerBinding().to(ModeConstraintChecker.class).in(Singleton.class);

		bind(PlanSelector.class).toProvider(MultinomialLogitSelectorProvider.class);
	}

	@Provides
	public CandidatePruner pruner(Map<String, CandidatePruner> pruners, InformedModeChoiceConfigGroup config) {

		if (config.getPruning() == null)
			return null;

		CandidatePruner pruner = pruners.get(config.getPruning());

		if (pruner == null)
			throw new IllegalStateException(String.format("Requested pruner %s in config, but it is not bound in the module", config.getPruning()));

		return pruner;
	}

	@Provides
	public PlanRouter planRouter(Provider<TripRouter> tripRouter, ActivityFacilities facilities, TimeInterpretation time) {
		return new PlanRouter(tripRouter.get(), facilities, time);
	}

	/**
	 * Binds all entries in map to their modes.
	 */
	@SuppressWarnings("unchecked")
	private <T> void bindAllModes(Map<String, Class<? extends T>> map, TypeLiteral<T> value) {

		// Ensure to bind to internal strings
		MapBinder<String, T> mapBinder = MapBinder.newMapBinder(binder(), new TypeLiteral<>() {}, value);
		for (Map.Entry<String, Class<? extends T>> e : map.entrySet()) {
			Class<? extends T> clazz = e.getValue();
			mapBinder.addBinding(e.getKey().intern()).to(clazz).in(Singleton.class);
		}
	}

	/**
	 * Builder to configure the module.
	 */
	public static final class Builder {

		private final Map<String, Class<? extends FixedCostsEstimator>> fixedCosts = new HashMap<>();
		private final Map<String, Class<? extends LegEstimator>> legEstimators = new HashMap<>();
		private final Map<String, Class<? extends TripEstimator>> tripEstimators = new HashMap<>();

		private final Map<String, Class<? extends ModeOptions>> options = new HashMap<>();

		private final Set<Class<? extends TripConstraint<?>>> constraints = new LinkedHashSet<>();
		private final Set<Class<? extends TripScoreEstimator>> tripScoreEstimators = new LinkedHashSet<>();

		private final Map<String, CandidatePruner> pruner = new HashMap<>();

		private Class<? extends ActivityEstimator> activityEstimator = ActivityEstimator.None.class;

		/**
		 * Adds a fixed cost to one or more modes.
		 */
		public Builder withFixedCosts(Class<? extends FixedCostsEstimator> estimator, String... modes) {

			for (String mode : modes) {
				fixedCosts.put(mode, estimator);
			}

			return this;
		}

		/**
		 * Adds a {@link LegEstimator} to one or more modes.
		 */
		public Builder withLegEstimator(Class<? extends LegEstimator> estimator, Class<? extends ModeOptions> option,
															String... modes) {

			for (String mode : modes) {

				if (tripEstimators.containsKey(mode))
					throw new IllegalArgumentException(String.format("Mode %s already has a plan estimator. Only one of either can be used", mode));


				legEstimators.put(mode, estimator);
				options.put(mode, option);
			}

			return this;
		}


		/**
		 * Adds a {@link TripEstimator} to one or more modes.
		 */
		public Builder withTripEstimator(Class<? extends TripEstimator> estimator, Class<? extends ModeOptions> option,
															 String... modes) {

			for (String mode : modes) {

				if (tripEstimators.containsKey(mode))
					throw new IllegalArgumentException(String.format("Mode %s already has a plan estimator. Only one of either can be used", mode));


				tripEstimators.put(mode, estimator);
				options.put(mode, option);
			}

			return this;
		}

		/**
		 * Adds a trip constraint to restrict generated options.
		 */
		public Builder withConstraint(Class<? extends TripConstraint<?>> constraint) {
			constraints.add(constraint);
			return this;
		}

		/**
		 * Add candidate pruner with specific name. Has to be set in the config in order to be activated.
		 */
		public Builder withPruner(String name, CandidatePruner pruner) {
			this.pruner.put(name, pruner);
			return this;
		}

		public Builder withActivityEstimator(Class<? extends ActivityEstimator> activityEstimator) {
			this.activityEstimator = activityEstimator;
			return this;
		}

		/**
		 * Add general score estimator that is applied to all trips.
		 */
		public Builder withTripScoreEstimator(Class<? extends TripScoreEstimator> tripScoreEstimator) {
			tripScoreEstimators.add(tripScoreEstimator);
			return this;
		}

		/**
		 * Builds the module, which can then be used with {@link #install()}
		 */
		public InformedModeChoiceModule build() {
			return new InformedModeChoiceModule(this);
		}

	}
}
