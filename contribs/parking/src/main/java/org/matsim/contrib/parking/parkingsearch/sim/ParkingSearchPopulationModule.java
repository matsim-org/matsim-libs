package org.matsim.contrib.parking.parkingsearch.sim;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;

public class ParkingSearchPopulationModule extends AbstractQSimModule {
	public final static String PARKING_SEARCH_AGENT_SOURCE_NAME = "ParkingSearchAgentSource";

	@Override
	protected void configureQSim() {
		if (getConfig().transit().isUseTransit()) {
			throw new RuntimeException("parking search together with transit is not implemented (should not be difficult)") ;
		} 
		
		bind(AgentFactory.class).to(ParkingAgentFactory.class).asEagerSingleton(); // (**)
		bind(ParkingPopulationAgentSource.class).asEagerSingleton();
		
		bindAgentSource(PARKING_SEARCH_AGENT_SOURCE_NAME).to(ParkingPopulationAgentSource.class);
	}

}
