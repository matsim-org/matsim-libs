package playground.pbouman.transitfares;

import java.util.Map;

import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.pbouman.agentproperties.AgentProperties;

public class TransitFareRouterFactoryImpl implements TransitRouterFactory {

	private final ScenarioImpl scenario;
	
	private final TransitSchedule schedule;
	private final TransitRouterConfig config;
	private final TransitRouterNetwork routerNetwork;
	private final Map<String,AgentProperties> agentProperties;
	
	public TransitFareRouterFactoryImpl(final ScenarioImpl scenario, final TransitRouterConfig config, final Map<String,AgentProperties> ap) {
		this.scenario = scenario;
		this.schedule = scenario.getTransitSchedule();
		this.config = config;
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.config.beelineWalkConnectionDistance);
		this.agentProperties = ap;
	}
	
	public TransitFareRouterFactoryImpl(final ScenarioImpl scenario, final TransitRouterConfig config) {
		this.scenario = scenario;
		this.schedule = scenario.getTransitSchedule();
		this.config = config;
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.config.beelineWalkConnectionDistance);
		this.agentProperties = null;
	}
	
	@Override
	public TransitRouter createTransitRouter()
	{	
		TransitFareRouterNetworkTimeAndDisutilityCalc costCalc;
		if (agentProperties != null)
			costCalc = new TransitFareRouterNetworkTimeAndDisutilityCalc(this.config, this.scenario, agentProperties);
		else
			costCalc = new TransitFareRouterNetworkTimeAndDisutilityCalc(this.config, this.scenario);
			
		
		return new TransitRouterImpl(config, routerNetwork, costCalc, costCalc);
	}

}
