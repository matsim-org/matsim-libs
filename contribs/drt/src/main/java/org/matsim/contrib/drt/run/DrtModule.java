package org.matsim.contrib.drt.run;

import org.matsim.contrib.drt.analysis.DrtModeAnalysisModule;
import org.matsim.contrib.drt.routing.DrtMainModeIdentifier;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import com.google.inject.Inject;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 */
public final class DrtModule extends AbstractModule {

	@Inject
	private DrtConfigGroup drtCfg;

	@Override
	public void install() {
		install(new DrtModeModule(drtCfg));
		installQSimModule(new DrtModeQSimModule(drtCfg));
		install(new DrtModeAnalysisModule(drtCfg));

		bind(MainModeIdentifier.class).to(DrtMainModeIdentifier.class).asEagerSingleton();

		bind(TravelDisutilityFactory.class).annotatedWith(Drt.class).toInstance(TimeAsTravelDisutility::new);
		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(), Drt.class);
			}
		});
	}
}
