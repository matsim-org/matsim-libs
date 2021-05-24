package org.matsim.contrib.parking.parkingsearch.sim;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;

public class ParkingSearchPopulationModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "ParkingSearch";

	@Override
	protected void configureQSim() {
		if (getConfig().transit().isUseTransit()) {
			throw new RuntimeException("parking search together with transit is not implemented (should not be difficult)") ;
		} 
		
		bind(AgentFactory.class).to(ParkingAgentFactory.class).asEagerSingleton(); // (**)
		bind(ParkingPopulationAgentSource.class).asEagerSingleton();
		
		addNamedComponent(ParkingPopulationAgentSource.class, COMPONENT_NAME);
	}

}
