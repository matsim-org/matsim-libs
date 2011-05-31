/**
 * 
 */
package kid;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.network.NetworkImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mrieser.core.mobsim.api.AgentSource;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.impl.DefaultPlanAgent;


/**
 * @author stefan
 *
 */
public class KiDPlanAgentCreator implements AgentSource{
	
	private static Logger logger = Logger.getLogger(KiDPlanAgentCreator.class);
	
	private ScheduledVehicles scheduledVehicles;
	
	private NetworkImpl network;
	
	public void setNetwork(NetworkImpl network) {
		this.network = network;
	}

	public void setRouter(PlanAlgorithm router) {
		this.router = router;
	}

	private PlanAlgorithm router;

	private List<PlanAgent> agents;
	
	private double weight = 1;
	
	public KiDPlanAgentCreator(ScheduledVehicles scheduledVehicles) {
		super();
		this.scheduledVehicles = scheduledVehicles;
	}
	
	public void createPlanAgents(){
		logger.info("create plan agents for " + scheduledVehicles.getScheduledVehicles().values().size() + " agents");
		agents = new ArrayList<PlanAgent>();
		for(ScheduledVehicle scheduledVehicle : scheduledVehicles.getScheduledVehicles().values()){
			ScheduledVehicleAgent vehicleAgent = new ScheduledVehicleAgent(scheduledVehicle);
			vehicleAgent.setNetwork(network);
			vehicleAgent.setRouter(router);
			Plan plan = vehicleAgent.createPlan();
			PlanAgent planAgent = new DefaultPlanAgent(plan, weight);
			agents.add(planAgent);
		}
	}

	public List<PlanAgent> getAgents() {
		return agents;
	}
	

}
