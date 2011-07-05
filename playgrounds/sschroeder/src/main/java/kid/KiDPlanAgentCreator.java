/**
 * 
 */
package kid;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
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
	
	private Population population;
	
	private GeotoolsTransformation transformation;
	
	/**
	 * activity coordinates from kid-activities are assigned to network-links. thus, coordinate-system of network must be equal
	 * to coordinate-system of kid-activities. this transformation transforms kid-coordinate system to network-coordinate system.
	 * by default, kid-coordinates are in WGS84.
	 * 
	 * @param transformation
	 */
	public void setTransformation(GeotoolsTransformation transformation) {
		this.transformation = transformation;
	}

	private double weight = 1;	

	public KiDPlanAgentCreator(ScheduledVehicles scheduledVehicles) {
		super();
		this.scheduledVehicles = scheduledVehicles;
		Config config = ConfigUtils.createConfig();
		config.addCoreModules();
		Scenario scen = ScenarioUtils.createScenario(config);
		population = scen.getPopulation();
	}
	public KiDPlanAgentCreator(Population population) {
		super();
		this.population = population;
		createAgents();
	}

	private void createAgents() {
		agents = new ArrayList<PlanAgent>();
		for(Person p : population.getPersons().values()){
			PlanAgent planAgent = new DefaultPlanAgent(p.getSelectedPlan(), weight);
			agents.add(planAgent);
		}
	}

	public void createPlanAgents(){
		logger.info("create plan agents for " + scheduledVehicles.getScheduledVehicles().values().size() + " agents");
		if(scheduledVehicles == null){
			return;
		}
		agents = new ArrayList<PlanAgent>();
		int counter = 1;
		int nextInfo = 2;
		for(ScheduledVehicle scheduledVehicle : scheduledVehicles.getScheduledVehicles().values()){
			if(counter == nextInfo){
				logger.info("vehicles " + counter);
				nextInfo *= 2;
			}
			ScheduledVehicleAgent vehicleAgent = new ScheduledVehicleAgent(scheduledVehicle);
			vehicleAgent.setTransformation(transformation);
			vehicleAgent.setNetwork(network);
			vehicleAgent.setRouter(router);
			Plan plan = vehicleAgent.createPlan();
			PlanAgent planAgent = new DefaultPlanAgent(plan, weight);
			Person person = new PersonImpl(scheduledVehicle.getVehicle().getId());
			person.addPlan(plan);
			population.addPerson(person);
			agents.add(planAgent);
			counter++;
		}
	}

	public List<PlanAgent> getAgents() {
		return agents;
	}
	
	public void writePlans(String filename){
		new PopulationWriter(population, network).write(filename);
	}
	
}
