package org.matsim.modechoice;

import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.matsim.core.controler.AbstractModule;
import org.matsim.modechoice.constraints.TripConstraint;
import org.matsim.modechoice.estimators.FixedCostEstimator;
import org.matsim.modechoice.estimators.LegEstimator;
import org.matsim.modechoice.estimators.PlanBasedLegEstimator;

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

	/**
	 * Special entry that can match any of the available modes.
	 */
	public static final String ANY_MODE = "any_mode";

	private final Builder builder;

	private InformedModeChoiceModule(Builder builder) {
		this.builder = builder;
	}

	@Override
	public void install() {

		bindAllModes(builder.fixedCosts, new TypeLiteral<>() {
		});
		bindAllModes(builder.estimators, new TypeLiteral<>() {
		});
		bindAllModes(builder.planEstimators, new TypeLiteral<>() {
		});
		bindAllModes(builder.options, new TypeLiteral<>() {
		});

		Multibinder<TripConstraint<?>> tcBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<>() {
		});

		for (Class<? extends TripConstraint<?>> c : builder.constraints) {
			tcBinder.addBinding().to(c).in(Singleton.class);
		}
	}


	/**
	 * Binds all entries in map to their modes.
	 */
	private <T> void bindAllModes(Map<String, Class<? extends T>> map, TypeLiteral<T> value) {

		MapBinder<String, T> mapBinder = MapBinder.newMapBinder(binder(), new TypeLiteral<>() {
		}, value);
		for (Map.Entry<String, Class<? extends T>> e : map.entrySet()) {
			mapBinder.addBinding(e.getKey()).to(e.getValue()).in(Singleton.class);
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Builder to configure the module.
	 */
	public static final class Builder {

		private final Map<String, Class<? extends FixedCostEstimator<?>>> fixedCosts = new HashMap<>();
		private final Map<String, Class<? extends LegEstimator<?>>> estimators = new HashMap<>();
		private final Map<String, Class<? extends PlanBasedLegEstimator<?>>> planEstimators = new HashMap<>();
		private final Map<String, Class<? extends ModeOptions<?>>> options = new HashMap<>();

		private final Set<Class<? extends TripConstraint<?>>> constraints = new LinkedHashSet<>();

		/**
		 * Adds a fixed cost to one or more modes.
		 */
		public <T extends Enum<?>> Builder withFixedCosts(Class<? extends FixedCostEstimator<T>> estimator, String... modes) {

			for (String mode : modes) {
				fixedCosts.put(mode, estimator);
			}

			return this;
		}

		/**
		 * Adds a {@link LegEstimator} to one or more modes.
		 */
		public <T extends Enum<?>> Builder withEstimator(Class<? extends LegEstimator<T>> estimator, Class<? extends ModeOptions<T>> option,
		                                                 String... modes) {

			for (String mode : modes) {

				if (planEstimators.containsKey(mode))
					throw new IllegalArgumentException(String.format("Mode %s already has a plan estimator. Only one of either can be used", mode));


				estimators.put(mode, estimator);
				options.put(mode, option);
			}

			return this;
		}

		/**
		 * Adds a plan based estimator to one or more nodes. Only one of {@link LegEstimator} or {@link PlanBasedLegEstimator} cam be present.
		 */
		public <T extends Enum<?>> Builder withPlanEstimator(Class<? extends PlanBasedLegEstimator<T>> estimator, Class<? extends ModeOptions<T>> option,
		                                                     String... modes) {

			for (String mode : modes) {
				if (estimators.containsKey(mode))
					throw new IllegalArgumentException(String.format("Mode %s already has an leg estimator. Only one of either leg or plan-based can be used", mode));


				planEstimators.put(mode, estimator);
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
