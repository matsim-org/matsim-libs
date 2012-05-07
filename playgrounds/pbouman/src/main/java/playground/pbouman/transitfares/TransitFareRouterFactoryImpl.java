package playground.pbouman.transitfares;

import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class TransitFareRouterFactoryImpl implements TransitRouterFactory {

	private final ScenarioImpl scenario;
	
	private final TransitSchedule schedule;
	private final TransitRouterConfig config;
	private final TransitRouterNetwork routerNetwork;
	
	public TransitFareRouterFactoryImpl(final ScenarioImpl scenario, final TransitRouterConfig config) {
		this.scenario = scenario;
		this.schedule = scenario.getTransitSchedule();
		this.config = config;
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.config.beelineWalkConnectionDistance);
	}
	
	@Override
	public TransitRouter createTransitRouter()
	{	
		TransitFareRouterNetworkTimeAndDisutilityCalc costCalc = new TransitFareRouterNetworkTimeAndDisutilityCalc(this.config, this.scenario);
		
		return new TransitRouterImpl(config, routerNetwork, costCalc, costCalc);
	}

}
