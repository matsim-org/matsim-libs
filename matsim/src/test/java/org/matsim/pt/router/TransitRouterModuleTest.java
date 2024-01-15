package org.matsim.pt.router;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import com.google.inject.Injector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.pt.config.TransitConfigGroup.TransitRoutingAlgorithmType;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser / Simunto
 */
public class TransitRouterModuleTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testTransitRoutingAlgorithm_DependencyInjection_Raptor() {
		Fixture f = new Fixture();
		f.config.transit().setRoutingAlgorithmType(TransitRoutingAlgorithmType.SwissRailRaptor);
		f.config.controller().setOutputDirectory(this.utils.getOutputDirectory());
		f.config.controller().setLastIteration(0);
		f.config.controller().setDumpDataAtEnd(false);

		Controler controler = new Controler(f.scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().to(DummyMobsim.class);
			}
		});
		controler.run();
		Injector injector = controler.getInjector();

		TransitRouter router = injector.getInstance(TransitRouter.class);
		Assertions.assertEquals(SwissRailRaptor.class, router.getClass());
	}

	private static class DummyMobsim implements Mobsim {
		public DummyMobsim() {
		}

		@Override
		public void run() {
		}
	}

}
