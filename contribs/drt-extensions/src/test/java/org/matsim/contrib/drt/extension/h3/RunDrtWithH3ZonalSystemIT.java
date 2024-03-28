package org.matsim.contrib.drt.extension.h3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalWaitTimesAnalyzer;
import org.matsim.contrib.drt.extension.DrtTestScenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author nkuehnel / MOIA
 */
public class RunDrtWithH3ZonalSystemIT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private Controler controler;

	@BeforeEach
	public void setUp() throws Exception {

		Config config = DrtTestScenario.loadConfig(utils);
		config.controller().setLastIteration(0);

		controler = MATSimApplication.prepare(new DrtTestScenario(controller -> prepare(controller, config), RunDrtWithH3ZonalSystemIT::prepare), config);
	}

	private static void prepare(Controler controler, Config config) {

		MultiModeDrtConfigGroup drtConfigs = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		for (DrtConfigGroup drtConfig : drtConfigs.getModalElements()) {
			drtConfig.getZonalSystemParams().get().h3Resolution = 9;
			drtConfig.getZonalSystemParams().get().zonesGeneration = DrtZonalSystemParams.ZoneGeneration.H3;
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					install(new AbstractDvrpModeModule(drtConfig.mode) {
						@Override
						public void install() {
							bindModal(DrtZonalWaitTimesAnalyzer.class).toProvider(modalProvider(
								getter -> new DrtZonalWaitTimesAnalyzer(drtConfig, getter.getModal(DrtEventSequenceCollector.class),
									getter.getModal(ZoneSystem.class)))).asEagerSingleton();
							addControlerListenerBinding().to(modalKey(DrtZonalWaitTimesAnalyzer.class));
						}
					});
				}
			});

		}
	}

	private static void prepare(Config config) {
		MultiModeDrtConfigGroup drtConfigs = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		for (DrtConfigGroup drtConfig : drtConfigs.getModalElements()) {
			DrtZonalSystemParams params = drtConfig.getZonalSystemParams().orElseThrow();
			params.cellSize = 1.;
			params.zonesGeneration = DrtZonalSystemParams.ZoneGeneration.GridFromNetwork;
		}
	}

	@Test
	void run() {
		String out = utils.getOutputDirectory();
		controler.run();

		assertThat(new File(out, "kelheim-mini-drt.drt_waitStats_drt_zonal.gpkg"))
			.exists()
			.isNotEmpty();

	}
}

