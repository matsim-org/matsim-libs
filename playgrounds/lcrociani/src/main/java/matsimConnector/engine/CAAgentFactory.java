package matsimConnector.engine;

import java.util.HashMap;
import java.util.Map;

import matsimConnector.agents.Pedestrian;
import matsimConnector.environment.TransitionArea;
import matsimConnector.scenario.CAEnvironment;
import matsimConnector.utility.IdUtility;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import pedCA.agents.Agent;
import pedCA.engine.AgentsGenerator;
import pedCA.environment.grid.GridPoint;

public class CAAgentFactory {

	//private Scenario scenario;
	private Map<Id<CAEnvironment>, AgentsGenerator> generators;
	
	public CAAgentFactory() {
		this.generators = new HashMap<Id<CAEnvironment>,AgentsGenerator>();
	}
	
	public Pedestrian buildPedestrian(Id<CAEnvironment> environmentId, QVehicle vehicle, TransitionArea transitionArea){
		GridPoint gp = transitionArea.calculateEnterPosition();
		int destinationId = extractDestinationId(vehicle);
		Agent agent = generators.get(environmentId).generatePedestrian(gp, destinationId);
		return new Pedestrian(agent,vehicle,transitionArea);
	}

	private int extractDestinationId(QVehicle vehicle) {
		Id<Link> linkId = vehicle.getDriver().chooseNextLinkId();
		return IdUtility.linkIdToDestinationId(linkId);
	}
	
	protected void addAgentsGenerator(Id<CAEnvironment> environmentId,AgentsGenerator agentGenerator){
		this.generators.put(environmentId,agentGenerator);
	}
	
}
