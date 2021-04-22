package org.matsim.pt.router;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.pt.config.TransitConfigGroup.TransitRoutingAlgorithmType;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser / Simunto
 */
public class TransitRouterModuleTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testTransitRoutingAlgorithm_DependencyInjection_Dijkstra() {
		Fixture f = new Fixture();
		f.config.transit().setRoutingAlgorithmType(TransitRoutingAlgorithmType.DijkstraBased);
		f.config.controler().setOutputDirectory(this.utils.getOutputDirectory());
		f.config.controler().setLastIteration(0);
		f.config.controler().setDumpDataAtEnd(false);

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
		Assert.assertEquals(TransitRouterImpl.class, router.getClass());
	}

	@Test
	public void testTransitRoutingAlgorithm_DependencyInjection_Raptor() {
		Fixture f = new Fixture();
		f.config.transit().setRoutingAlgorithmType(TransitRoutingAlgorithmType.SwissRailRaptor);
		f.config.controler().setOutputDirectory(this.utils.getOutputDirectory());
		f.config.controler().setLastIteration(0);
		f.config.controler().setDumpDataAtEnd(false);

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
		Assert.assertEquals(SwissRailRaptor.class, router.getClass());
	}

	private static class DummyMobsim implements Mobsim {
		public DummyMobsim() {
		}

		@Override
		public void run() {
		}
	}

}