package org.matsim.contrib.drt.estimator.run;

import com.google.inject.Singleton;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.modal.ModalInjector;
import org.matsim.core.modal.ModalProviders;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Main module that needs to be installed if any estimator is to be used.
 */
public class DrtEstimatorModule extends AbstractModule {

	@Override
	public void install() {

		MultiModeDrtEstimatorConfigGroup config = ConfigUtils.addOrGetModule(getConfig(), MultiModeDrtEstimatorConfigGroup.class);

		for (DrtEstimatorConfigGroup group : config.getModalElements()) {
			install(new ModeModul(group));
		}
	}

	static final class ModeModul extends AbstractDvrpModeModule {

		private final DrtEstimatorConfigGroup group;

		public ModeModul(DrtEstimatorConfigGroup group) {
			super(group.mode);
			this.group = group;
		}

		@Override
		public void install() {


			bindModal(DrtEstimator.class).toProvider(modalProvider(
					getter -> {
						try {
							Constructor<? extends DrtEstimator> constructor = group.estimator.getDeclaredConstructor(ModalInjector.class);
							return constructor.newInstance(ModalProviders.createInjector(getter));
						} catch (NoSuchMethodException e) {
							throw new RuntimeException("Could not find constructor for DRT estimator. You need to have a public constructor with one parameter accepting ModalInjector", e);
						} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
							throw new RuntimeException("Could not instantiate DRT estimator", e);
						}
					}
			)).in(Singleton.class);

			addControlerListenerBinding().to(modalKey(DrtEstimator.class));

			bindModal(DrtEstimatorConfigGroup.class).toInstance(group);
		}

	}
}
