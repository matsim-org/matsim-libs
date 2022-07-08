package org.matsim.modechoice;

import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.matsim.core.controler.AbstractModule;
import org.matsim.modechoice.constraints.TripConstraint;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.estimators.LegEstimator;
import org.matsim.modechoice.estimators.TripEstimator;
import org.matsim.modechoice.replanning.SelectBestKPlanModesStrategyProvider;
import org.matsim.modechoice.replanning.InformedModeChoiceStrategyProvider;
import org.matsim.modechoice.replanning.SelectSingleTripModeStrategyProvider;
import org.matsim.modechoice.search.BestChoiceGenerator;
import org.matsim.modechoice.search.SingleTripChoicesGenerator;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The main and only module needed to install to use informed mode choice functionality.
 *
 * @see Builder
 */
public final class InformedModeChoiceModule extends AbstractModule {

	public static String SELECT_BEST_K_PLAN_MODES_STRATEGY = "SelectBestKPlanModes";

	public static String SELECT_SINGLE_TRIP_MODE_STRATEGY = "SelectSingleTripMode";

	// TODO
	public static String SELECT_SUBTOUR_MODE_STRATEGY = "SelectSubtourMode";

	public static String INFORMED_MODE_CHOICE = "InformedModeChoice";

	private final Builder builder;

	private InformedModeChoiceModule(Builder builder) {
		this.builder = builder;
	}

	@Override
	public void install() {

		bindAllModes(builder.fixedCosts, new TypeLiteral<>() {
		});
		bindAllModes(builder.legEstimators, new TypeLiteral<>() {
		});
		bindAllModes(builder.tripEstimators, new TypeLiteral<>() {
		});
		bindAllModes(builder.options, new TypeLiteral<>() {
		});

		Multibinder<TripConstraint<?>> tcBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<>() {
		});

		// Not singleton, they should be able to be created per thread if necessary.
		bind(EstimateRouter.class);
		bind(TopKChoicesGenerator.class);
		bind(BestChoiceGenerator.class);
		bind(SingleTripChoicesGenerator.class);

		for (Class<? extends TripConstraint<?>> c : builder.constraints) {
			tcBinder.addBinding().to(c).in(Singleton.class);
		}

		addPlanStrategyBinding(SELECT_BEST_K_PLAN_MODES_STRATEGY).toProvider(SelectBestKPlanModesStrategyProvider.class);
		addPlanStrategyBinding(SELECT_SINGLE_TRIP_MODE_STRATEGY).toProvider(SelectSingleTripModeStrategyProvider.class);
		addPlanStrategyBinding(INFORMED_MODE_CHOICE).toProvider(InformedModeChoiceStrategyProvider.class);

		// TODO: SubTour best choice + best k selection
		// TODO: allow generators to only work on subset of plans

		// TODO: annealing for inv better

	}

	/**
	 * Binds all entries in map to their modes.
	 */
	private <T> void bindAllModes(Map<String, Class<? extends T>> map, TypeLiteral<T> value) {

		// Ensure to bind to internal strings
		MapBinder<String, T> mapBinder = MapBinder.newMapBinder(binder(), new TypeLiteral<>() {
		}, value);
		for (Map.Entry<String, Class<? extends T>> e : map.entrySet()) {
			mapBinder.addBinding(e.getKey().intern()).to(e.getValue()).in(Singleton.class);
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Builder to configure the module.
	 */
	public static final class Builder {

		private final Map<String, Class<? extends FixedCostsEstimator<?>>> fixedCosts = new HashMap<>();
		private final Map<String, Class<? extends LegEstimator<?>>> legEstimators = new HashMap<>();
		private final Map<String, Class<? extends TripEstimator<?>>> tripEstimators = new HashMap<>();

		private final Map<String, Class<? extends ModeOptions<?>>> options = new HashMap<>();

		private final Set<Class<? extends TripConstraint<?>>> constraints = new LinkedHashSet<>();

		/**
		 * Adds a fixed cost to one or more modes.
		 */
		public <T extends Enum<?>> Builder withFixedCosts(Class<? extends FixedCostsEstimator<T>> estimator, String... modes) {

			for (String mode : modes) {
				fixedCosts.put(mode, estimator);
			}

			return this;
		}

		/**
		 * Adds a {@link LegEstimator} to one or more modes.
		 */
		public <T extends Enum<?>> Builder withLegEstimator(Class<? extends LegEstimator<T>> estimator, Class<? extends ModeOptions<T>> option,
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
		public <T extends Enum<?>> Builder withTripEstimator(Class<? extends TripEstimator<T>> estimator, Class<? extends ModeOptions<T>> option,
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
		 * Builds the module, which can then be used with {@link #install()}
		 */
		public InformedModeChoiceModule build() {
			return new InformedModeChoiceModule(this);
		}

	}
}
