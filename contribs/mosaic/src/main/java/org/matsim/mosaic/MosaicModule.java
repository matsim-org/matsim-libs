package org.matsim.mosaic;

import org.eclipse.mosaic.rti.api.RtiAmbassador;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtModeModule;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;

/**
 * Mosaic module that binds required components to the {@link MATSimAmbassador}.
 */
public class MosaicModule extends AbstractModule {

	private final MATSimAmbassador ambassador;
	private final Config config;

	public MosaicModule(MATSimAmbassador ambassador, Config config) {
		this.ambassador = ambassador;
		this.config = config;
	}

	@Override
	public void install() {

		addControlerListenerBinding().toInstance(ambassador);
		addMobsimListenerBinding().toInstance(ambassador);

		bind(MATSimAmbassador.class).toInstance(ambassador);
		bind(RtiAmbassador.class).toInstance(ambassador.getRti());

		MultiModeDrtConfigGroup multiModeDrtCfg = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);

		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
			install(new DrtModeModule(drtCfg));
			installQSimModule(new MosaicQsimModule(drtCfg.getMode()));

			// TODO: analysis throws errors currently
			//install(new DrtModeAnalysisModule(drtCfg));
		}
	}

	private final class MosaicQsimModule extends AbstractDvrpModeQSimModule {

		private MosaicQsimModule(String mode) {
			super(mode);
		}

		@Override
		protected void configureQSim() {
			addModalComponent(DrtOptimizer.class,
					modalProvider(g -> new MosaicDrtEngine(ambassador.getRti(), g.getModal(Fleet.class)))
			);

			bindModal(VrpOptimizer.class).to(modalKey(DrtOptimizer.class));
		}
	}

}
