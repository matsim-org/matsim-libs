package playground.pieter.pseudosim.controler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.pieter.pseudosim.replanning.PseudoSimSubSetSimulationStrategyManager;
import playground.pieter.pseudosim.trafficinfo.PseudoSimStopStopTimeCalculator;
import playground.pieter.pseudosim.trafficinfo.PseudoSimWaitTimeCalculator2;
import playground.sergioo.singapore2012.transitRouterVariable.StopStopTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.WaitTimeStuckCalculator;

/**
 * @author fouriep
 * 
 */
public class PseudoSimControler extends Controler{
	
	private ObjectAttributes agentPlansMarkedForSubsetMentalSim = new ObjectAttributes();
	private LinkedHashSet<Plan> plansForMentalSimulation = new LinkedHashSet<Plan>();
	private LinkedHashSet<IdImpl> agentsForMentalSimulation = new LinkedHashSet<IdImpl>();
	private HashMap<IdImpl,Double> nonSimulatedAgentSelectedPlanScores = new HashMap<IdImpl, Double>(); 
	public static String AGENT_ATT = "mentalsimAgent";
	boolean simulateSubsetPersonsOnly = false;
	private WaitTimeStuckCalculator waitTimeCalculator;
	private StopStopTimeCalculator stopStopTimeCalculator;
	public boolean isSimulateSubsetPersonsOnly() {
		return simulateSubsetPersonsOnly;
	}



	public void setSimulateSubsetPersonsOnly(boolean simulateSubsetPersonsOnly) {
		this.simulateSubsetPersonsOnly = simulateSubsetPersonsOnly;
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = null;
		if(simulateSubsetPersonsOnly){
			manager = new PseudoSimSubSetSimulationStrategyManager(this);
		}else{
			manager = new StrategyManager();
		}
		
		StrategyManagerConfigLoader.load(this, manager);
		return manager;
	}


	public ObjectAttributes getAgentPlansMarkedForSubsetMentalSim() {
		return agentPlansMarkedForSubsetMentalSim;
	}



	/**
	 * @param samplingProbability
	 *            Samples persons for mental simulation. Only the selected plan of the
	 *            agent is cloned. The original population is stored for later
	 *            retrieval.
	 */
	public void markSubsetAgents(
			double samplingProbability) {
		agentPlansMarkedForSubsetMentalSim.clear();
		for (Person p : this.getPopulation().getPersons().values()) {
			PersonImpl pax = (PersonImpl) p;
			// remember the person's original plans
			if (MatsimRandom.getRandom().nextDouble() <= samplingProbability) {
				ArrayList<Plan> originalPlans = new ArrayList<Plan>();
				for(Plan plan:p.getPlans()){
					originalPlans.add(plan);
				}
				agentPlansMarkedForSubsetMentalSim.putAttribute(p.getId().toString(), AGENT_ATT,originalPlans);
			}

		}

	}



	public PseudoSimControler(String[] args) {
		super(ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0])));

		if (this.getConfig().scenario().isUseTransit()) {
			this.waitTimeCalculator = new PseudoSimWaitTimeCalculator2(
					this.getPopulation(),
					this.getScenario().getTransitSchedule(),
					this.getConfig().travelTimeCalculator()
							.getTraveltimeBinSize(),
					(int) (this.getConfig().getQSimConfigGroup().getEndTime() - this
							.getConfig().getQSimConfigGroup().getStartTime()));
			this.getEvents().addHandler(waitTimeCalculator);
			this.stopStopTimeCalculator = new PseudoSimStopStopTimeCalculator(
					this.getScenario().getTransitSchedule(),
					((ScenarioImpl) this.getScenario()).getVehicles(), this
							.getConfig().travelTimeCalculator()
							.getTraveltimeBinSize(), (int) (this.getConfig()
							.getQSimConfigGroup().getEndTime() - this
							.getConfig().getQSimConfigGroup().getStartTime()));
			this.getEvents().addHandler(stopStopTimeCalculator);
		}
	}


	public void addPlanForMentalSimulation(Plan p){
		plansForMentalSimulation.add(p);
		agentsForMentalSimulation.add((IdImpl) p.getPerson().getId());
	}


	/**
	 * @param planSelector
	 * <p> 
	 * checks the plans for this person against the ones stored in the objectattributes list.
	 * creates a fake person, then maps the mentalsim plans to the fake person.
	 * performs selection according to the plan selection scheme, then passes the original set of plans back to the person, along with the selected mentalsim plan
	 * 
	 */
	public void stripOutMentalSimPlansExceptSelected(
			PlanSelector planSelector) {
		for (Person pax : this.getPopulation().getPersons().values()) {
			PersonImpl p = (PersonImpl) pax;
			ArrayList<Plan> originalPlans = (ArrayList<Plan>) agentPlansMarkedForSubsetMentalSim.getAttribute(p.getId().toString(), AGENT_ATT);
			if(originalPlans==null){
				//skip this person
				continue;
			}
			
			Person fakePerson = this.getPopulation().getFactory().createPerson(new IdImpl(p.getId().toString()+"FFF"));
//			ArrayList<Plan> mentalSimPlans = new ArrayList<Plan>();
			for(Plan plan:p.getPlans()){
				if(!originalPlans.contains(plan)){
					fakePerson.addPlan(plan);
//					mentalSimPlans.add(plan);
				}
			}
			
			p.getPlans().clear();
			

			for(Plan originalPlan:originalPlans){
				p.addPlan(originalPlan);
			}
			Plan mentalPlan = planSelector.selectPlan(fakePerson);
			if(mentalPlan!=null){
				p.addPlan(mentalPlan);
				p.setSelectedPlan(mentalPlan);
			}else{
				Logger.getLogger(this.getClass()).warn("oooh! couldn't swop back!!");
			}
			

		}
		
	}



	public LinkedHashSet<Plan> getPlansForMentalSimulation() {
		return plansForMentalSimulation;
	}


	public void clearPlansForMentalSimulation(){
		plansForMentalSimulation = new LinkedHashSet<Plan>();
		agentsForMentalSimulation = new LinkedHashSet<IdImpl>();
		nonSimulatedAgentSelectedPlanScores = new HashMap<IdImpl, Double>();
	}



	public LinkedHashSet<IdImpl> getAgentsForMentalSimulation() {
		return agentsForMentalSimulation;
	}



	public HashMap<IdImpl,Double> getNonSimulatedAgentSelectedPlanScores() {
		return nonSimulatedAgentSelectedPlanScores;
	}



	public WaitTimeStuckCalculator getWaitTimeCalculator() {
		return waitTimeCalculator;
	}



	public StopStopTimeCalculator getStopStopTimeCalculator() {
		return stopStopTimeCalculator;
	}




}
