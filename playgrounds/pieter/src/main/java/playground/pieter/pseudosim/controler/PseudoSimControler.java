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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.pieter.pseudosim.controler.listeners.ExpensiveSimScoreWriter;
import playground.pieter.pseudosim.controler.listeners.MobSimSwitcher;
import playground.pieter.pseudosim.controler.listeners.PseudoSimPlanMarkerModuleAppender;
import playground.pieter.pseudosim.controler.listeners.PseudoSimSubSetSimulationListener;
import playground.pieter.pseudosim.controler.listeners.SimpleAnnealer;
import playground.pieter.pseudosim.replanning.PseudoSimSubSetSimulationStrategyManager;
import playground.pieter.pseudosim.trafficinfo.PseudoSimTravelTimeCalculator;
import playground.pieter.pseudosim.trafficinfo.PseudoSimStopStopTimeCalculator;
import playground.pieter.pseudosim.trafficinfo.PseudoSimWaitTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.StopStopTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.WaitTimeStuckCalculator;

/**
 * @author fouriep
 * 
 */
public class PseudoSimControler extends Controler{
	
	private ObjectAttributes agentPlansMarkedForSubsetPseudoSim = new ObjectAttributes();
	private LinkedHashSet<Plan> plansForPseudoSimulation = new LinkedHashSet<Plan>();
	private LinkedHashSet<IdImpl> agentsForPseudoSimulation = new LinkedHashSet<IdImpl>();
	private HashMap<IdImpl,Double> nonSimulatedAgentSelectedPlanScores = new HashMap<IdImpl, Double>(); 
	public static String AGENT_ATT = "PseudoSimAgent";
	boolean simulateSubsetPersonsOnly = false;
	private WaitTimeStuckCalculator waitTimeCalculator;
	private StopStopTimeCalculator stopStopTimeCalculator;
	private PseudoSimTravelTimeCalculator carTravelTimeCalculator;
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
			return super.loadStrategyManager();
		}
		
		StrategyManagerConfigLoader.load(this, manager);
		return manager;
	}


	public ObjectAttributes getAgentPlansMarkedForSubsetPseudoSim() {
		return agentPlansMarkedForSubsetPseudoSim;
	}



	/**
	 * @param samplingProbability
	 *            Samples persons for mental simulation. Only the selected plan of the
	 *            agent is cloned. The original population is stored for later
	 *            retrieval.
	 */
	public void markSubsetAgents(
			double samplingProbability) {
		agentPlansMarkedForSubsetPseudoSim.clear();
		for (Person p : this.getPopulation().getPersons().values()) {
			PersonImpl pax = (PersonImpl) p;
			// remember the person's original plans
			if (MatsimRandom.getRandom().nextDouble() <= samplingProbability) {
				ArrayList<Plan> originalPlans = new ArrayList<Plan>();
				for(Plan plan:p.getPlans()){
					originalPlans.add(plan);
				}
				agentPlansMarkedForSubsetPseudoSim.putAttribute(p.getId().toString(), AGENT_ATT,originalPlans);
			}

		}

	}



	public PseudoSimControler(String[] args) {
		super(ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0])));
		PlanSelector pSimPlanSelector = new ExpBetaPlanSelector(new PlanCalcScoreConfigGroup());
		this.addControlerListener(new PseudoSimSubSetSimulationListener(this,pSimPlanSelector));
		this.addControlerListener(new SimpleAnnealer());
		this.addControlerListener(new MobSimSwitcher(this));
		this.addControlerListener(new PseudoSimPlanMarkerModuleAppender(this));
		this.addControlerListener(new ExpensiveSimScoreWriter(this));
		this.carTravelTimeCalculator = new PseudoSimTravelTimeCalculator(getNetwork(),
				getConfig().travelTimeCalculator());
		this.getEvents().addHandler(carTravelTimeCalculator);
		if (this.getConfig().scenario().isUseTransit()) {
			this.waitTimeCalculator = new PseudoSimWaitTimeCalculator(
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


	public void addPlanForPseudoSimulation(Plan p){
		plansForPseudoSimulation.add(p);
		agentsForPseudoSimulation.add((IdImpl) p.getPerson().getId());
	}


	/**
	 * @param planSelector
	 * <p> 
	 * checks the plans for this person against the ones stored in the objectattributes list.
	 * creates a fake person, then maps the PseudoSim plans to the fake person.
	 * performs selection according to the plan selection scheme, then passes the original set of plans back to the person, along with the selected PseudoSim plan
	 * 
	 */
	public void stripOutPseudoSimPlansExceptSelected(
			PlanSelector planSelector) {
		for (Person pax : this.getPopulation().getPersons().values()) {
			PersonImpl p = (PersonImpl) pax;
			ArrayList<Plan> originalPlans = (ArrayList<Plan>) agentPlansMarkedForSubsetPseudoSim.getAttribute(p.getId().toString(), AGENT_ATT);
			if(originalPlans==null){
				//skip this person
				continue;
			}
			
			Person fakePerson = this.getPopulation().getFactory().createPerson(new IdImpl(p.getId().toString()+"FFF"));
//			ArrayList<Plan> PseudoSimPlans = new ArrayList<Plan>();
			for(Plan plan:p.getPlans()){
				if(!originalPlans.contains(plan)){
					fakePerson.addPlan(plan);
//					PseudoSimPlans.add(plan);
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



	public LinkedHashSet<Plan> getPlansForPseudoSimulation() {
		return plansForPseudoSimulation;
	}


	public void clearPlansForPseudoSimulation(){
		plansForPseudoSimulation = new LinkedHashSet<Plan>();
		agentsForPseudoSimulation = new LinkedHashSet<IdImpl>();
		nonSimulatedAgentSelectedPlanScores = new HashMap<IdImpl, Double>();
	}



	public LinkedHashSet<IdImpl> getAgentsForPseudoSimulation() {
		return agentsForPseudoSimulation;
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



	public PseudoSimTravelTimeCalculator getCarTravelTimeCalculator() {
		return carTravelTimeCalculator;
	}




}
