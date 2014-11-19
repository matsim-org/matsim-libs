package playground.anhorni.rc.microwdr;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilterFactory;


public class StuckAgentsFilterFactory implements AgentFilterFactory {
	
	protected WithinDayControlerListener withinDayControlerListener;
	private static final Logger log = Logger.getLogger(StuckAgentsFilterFactory.class);
	private Network network;
	
	public StuckAgentsFilterFactory(WithinDayControlerListener withinDayControlerListener, Network network) {
		this.network = network;
		this.withinDayControlerListener = withinDayControlerListener;
		
	}

	@Override
	public AgentFilter createAgentFilter() {
		log.info("creating stuck agents filter ...");		
		return new StuckAgentsFilter(withinDayControlerListener.getMobsimDataProvider().getAgents(), 
				this.withinDayControlerListener.getTravelTimeCollector(),
				this.network);
	}
}
