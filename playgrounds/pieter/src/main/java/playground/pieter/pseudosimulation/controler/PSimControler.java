package playground.pieter.pseudosimulation.controler;

import java.util.HashMap;
import java.util.LinkedHashSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.pieter.pseudosimulation.controler.listeners.AfterScoringSelectedPlanScoreRestoreListener;
import playground.pieter.pseudosimulation.controler.listeners.BeforePSimSelectedPlanScoreRecorder;
import playground.pieter.pseudosimulation.controler.listeners.MobSimSwitcher;
import playground.pieter.pseudosimulation.controler.listeners.QSimScoreWriter;
import playground.pieter.pseudosimulation.replanning.PSimPlanStrategyRegistrar;
import playground.pieter.pseudosimulation.trafficinfo.PSimStopStopTimeCalculator;
import playground.pieter.pseudosimulation.trafficinfo.PSimTravelTimeCalculator;
import playground.pieter.pseudosimulation.trafficinfo.PSimWaitTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.*;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.*;

/**
 * @author fouriep
 *         <P>
 *         This controler registers listeners necessary for pseudo-simulation,
 *         and replaces config strategies with their psim equivalents.
 * 
 *         <P>
 *         It also keeps track of agents for psim, and stores the scores of
 *         agents not being simulated.
 */
public class PSimControler {
	
	private Controler matsimControler;
	public Controler getMATSimControler() {
		return matsimControler;
	}



	private LinkedHashSet<Plan> plansForPseudoSimulation = new LinkedHashSet<Plan>();
	private LinkedHashSet<IdImpl> agentsForPseudoSimulation = new LinkedHashSet<IdImpl>();
	private HashMap<IdImpl,Double> nonSimulatedAgentSelectedPlanScores = new HashMap<IdImpl, Double>(); 
	public static String AGENT_ATT = "PseudoSimAgent";
	private WaitTimeStuckCalculator waitTimeCalculator;
	private StopStopTimeCalculator stopStopTimeCalculator;
	private PSimTravelTimeCalculator carTravelTimeCalculator;
	private PSimPlanStrategyRegistrar psimStrategies;



	
	public PSimControler(String[] args) {
		matsimControler = new Controler(ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0])));
		
		this.psimStrategies = new PSimPlanStrategyRegistrar(this);
		//substitute qualifying plan strategies with their PSim equivalents
		this.substituteStrategies();
		matsimControler.addControlerListener(new MobSimSwitcher(this));
		matsimControler.addControlerListener(new QSimScoreWriter(this));
		matsimControler.addControlerListener(new BeforePSimSelectedPlanScoreRecorder(this));
		matsimControler.addControlerListener(new AfterScoringSelectedPlanScoreRestoreListener(this));
		this.carTravelTimeCalculator = new PSimTravelTimeCalculator(matsimControler.getNetwork(),
				matsimControler.getConfig().travelTimeCalculator(),70);
		matsimControler.getEvents().addHandler(carTravelTimeCalculator);
		if (matsimControler.getConfig().scenario().isUseTransit()) {
			this.waitTimeCalculator = new PSimWaitTimeCalculator(
					matsimControler.getPopulation(),
					matsimControler.getScenario().getTransitSchedule(),
					matsimControler.getConfig().travelTimeCalculator()
							.getTraveltimeBinSize(),
					(int) (matsimControler.getConfig().qsim().getEndTime() - matsimControler
							.getConfig().qsim().getStartTime()));
			matsimControler.getEvents().addHandler(waitTimeCalculator);
			this.stopStopTimeCalculator = new PSimStopStopTimeCalculator(
					matsimControler.getScenario().getTransitSchedule(),
					matsimControler
							.getConfig().travelTimeCalculator()
							.getTraveltimeBinSize(), (int) (matsimControler.getConfig()
							.qsim().getEndTime() - matsimControler
							.getConfig().qsim().getStartTime()));
			matsimControler.getEvents().addHandler(stopStopTimeCalculator);
		}
	}


	/**
	 * Goes through the list of plan strategies and substitutes qualifying strategies with their PSim equivalents
	 */
	private void substituteStrategies() {
//		ArrayList<String> nonMutatingStrategies = new ArrayList<String>();
//		for(PlanStrategyRegistrar.Selector selector:PlanStrategyRegistrar.Selector.values()){
//			nonMutatingStrategies.add(selector.toString());
//		}
		for (StrategyConfigGroup.StrategySettings settings : matsimControler.getConfig().strategy().getStrategySettings()) {

			String classname = settings.getModuleName();
			
			if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
				classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
				settings.setModuleName(classname);
			}
//			if(nonMutatingStrategies.contains(classname))
//				continue;
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
