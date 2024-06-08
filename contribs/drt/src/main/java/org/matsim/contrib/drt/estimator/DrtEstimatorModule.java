package org.matsim.contrib.drt.estimator;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.modal.ModalAnnotationCreator;

import java.lang.annotation.Annotation;

/**
 * Main module that needs to be installed if any estimator is to be used.
 */
public class DrtEstimatorModule extends AbstractDvrpModeModule {

	private final DrtConfigGroup drtCfg;
	private final DrtEstimatorParams params;

	public DrtEstimatorModule(String mode, DrtConfigGroup drtCfg, DrtEstimatorParams params) {
		super(mode);
		this.drtCfg = drtCfg;
		this.params = params;
	}

	/**
	 * Create binder for estimator to a specific mode. This is a helper method to use binding without creating
	 * a separate modal module.
	 */
	public static LinkedBindingBuilder<DrtEstimator> bindEstimator(Binder binder, String mode) {
		Key<DrtEstimator> key = modalKey(DvrpModes::mode, mode);
		return binder.bind(key);
	}

	private static <T, M extends Annotation> Key<DrtEstimator> modalKey(ModalAnnotationCreator<M> f, String mode) {
		return f.key(DrtEstimator.class, mode);
	}

	@Override
	public void install() {

		// DRT Estimators will be available as Map<DvrpMode, DrtEstimator>
		MapBinder.newMapBinder(this.binder(), DvrpMode.class, DrtEstimator.class)
			.addBinding(DvrpModes.mode(getMode()))
			.to(modalKey(DrtEstimator.class));

		addControlerListenerBinding().to(modalKey(DrtEstimator.class));

		bindModal(DrtEstimatorParams.class).toInstance(params);

		// Analyze quality of estimates, this is only useful if an online estimator is used
		// TODO: updating estimation as in drt speed up is not fully implemented yet
		if (drtCfg.simulationType == DrtConfigGroup.SimulationType.fullSimulation) {
			bindModal(DrtEstimateAnalyzer.class)
				.toProvider(
					modalProvider(getter -> new DrtEstimateAnalyzer(getter.getModal(DrtEstimator.class),
						getter.getModal(DrtEventSequenceCollector.class), getMode()))
				)
				.in(Singleton.class);

			addControlerListenerBinding().to(modalKey(DrtEstimateAnalyzer.class));
		}
	}
}
