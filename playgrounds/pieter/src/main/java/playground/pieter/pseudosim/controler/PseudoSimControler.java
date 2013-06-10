package playground.pieter.pseudosim.controler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PlanStrategyFactoryRegister;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.pieter.pseudosim.controler.listeners.BeforePSimSelectedPlanScoreRecorder;
import playground.pieter.pseudosim.controler.listeners.ExpensiveSimScoreWriter;
import playground.pieter.pseudosim.controler.listeners.IterationEndsSelectedPlanScoreRestoreListener;
import playground.pieter.pseudosim.controler.listeners.MobSimSwitcher;
import playground.pieter.pseudosim.controler.listeners.PseudoSimPlanMarkerModuleAppender;
import playground.pieter.pseudosim.controler.listeners.SimpleAnnealer;
import playground.pieter.pseudosim.replanning.PSimPlanStrategyRegistrar;
import playground.pieter.pseudosim.trafficinfo.PSimTravelTimeCalculator;
import playground.pieter.pseudosim.trafficinfo.PSimStopStopTimeCalculator;
import playground.pieter.pseudosim.trafficinfo.PSimWaitTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.StopStopTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.WaitTimeStuckCalculator;

/**
 * @author fouriep
 * 
 */
public class PseudoSimControler extends Controler{
	
	
	private LinkedHashSet<Plan> plansForPseudoSimulation = new LinkedHashSet<Plan>();
	private LinkedHashSet<IdImpl> agentsForPseudoSimulation = new LinkedHashSet<IdImpl>();
	private HashMap<IdImpl,Double> nonSimulatedAgentSelectedPlanScores = new HashMap<IdImpl, Double>(); 
	public static String AGENT_ATT = "PseudoSimAgent";
	private WaitTimeStuckCalculator waitTimeCalculator;
	private StopStopTimeCalculator stopStopTimeCalculator;
	private PSimTravelTimeCalculator carTravelTimeCalculator;
	private PSimPlanStrategyRegistrar psimStrategies;



	
	public PseudoSimControler(String[] args) {
		super(ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0])));
		this.psimStrategies = new PSimPlanStrategyRegistrar(this);
		this.substituteStrategies();
		this.addControlerListener(new SimpleAnnealer());
		this.addControlerListener(new MobSimSwitcher(this));
//		this.addControlerListener(new PseudoSimPlanMarkerModuleAppender(this));
		this.addControlerListener(new ExpensiveSimScoreWriter(this));
		this.addControlerListener(new BeforePSimSelectedPlanScoreRecorder(this));
		this.addControlerListener(new IterationEndsSelectedPlanScoreRestoreListener(this));
		this.carTravelTimeCalculator = new PSimTravelTimeCalculator(getNetwork(),
				getConfig().travelTimeCalculator());
		this.getEvents().addHandler(carTravelTimeCalculator);
		if (this.getConfig().scenario().isUseTransit()) {
			this.waitTimeCalculator = new PSimWaitTimeCalculator(
					this.getPopulation(),
					this.getScenario().getTransitSchedule(),
					this.getConfig().travelTimeCalculator()
							.getTraveltimeBinSize(),
					(int) (this.getConfig().getQSimConfigGroup().getEndTime() - this
							.getConfig().getQSimConfigGroup().getStartTime()));
			this.getEvents().addHandler(waitTimeCalculator);
			this.stopStopTimeCalculator = new PSimStopStopTimeCalculator(
					this.getScenario().getTransitSchedule(),
					((ScenarioImpl) this.getScenario()).getVehicles(), this
							.getConfig().travelTimeCalculator()
							.getTraveltimeBinSize(), (int) (this.getConfig()
							.getQSimConfigGroup().getEndTime() - this
							.getConfig().getQSimConfigGroup().getStartTime()));
			this.getEvents().addHandler(stopStopTimeCalculator);
		}
	}


	/**
	 * Goes through the list of plan strategies and substitutes qualifying strategies with their PSim equivalents
	 */
	private void substituteStrategies() {
		String[] nonMutatingStrategyModules = this.getConfig()
				.getParam("PseudoSim", "nonMutatingStrategies").split(",");
		ArrayList<String> nonMutatingStrategies = new ArrayList<String>();
		for (String strat : nonMutatingStrategyModules) {
			nonMutatingStrategies.add(this.getConfig().strategy().getParams()
					.get(strat.trim()));
		}
		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {

			String classname = settings.getModuleName();
			
			if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
				classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
				settings.setModuleName(classname);
			}
			if(nonMutatingStrategies.contains(classname))
				continue;
			if(!psimStrategies.getCompatibleStrategies().contains(classname)){
				throw new RuntimeException("Strategy "+classname+"not known to be compatible with PseudoSim. Exiting.");
			}else{
				settings.setModuleName(classname+"PSim");
			}
			Logger.getLogger(this.getClass()).info("Mutating plan strategies prepared for PSim");
		}
		
	}


	public void addPlanForPseudoSimulation(Plan p){
		plansForPseudoSimulation.add(p);
		agentsForPseudoSimulation.add((IdImpl) p.getPerson().getId());
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



	public PSimTravelTimeCalculator getCarTravelTimeCalculator() {
		return carTravelTimeCalculator;
	}




}
