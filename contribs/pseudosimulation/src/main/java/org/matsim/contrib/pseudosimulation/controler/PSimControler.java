package org.matsim.contrib.pseudosimulation.controler;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.contrib.pseudosimulation.controler.listeners.AfterScoringSelectedPlanScoreRestoreListener;
import org.matsim.contrib.pseudosimulation.controler.listeners.BeforePSimSelectedPlanScoreRecorder;
import org.matsim.contrib.pseudosimulation.controler.listeners.MobSimSwitcher;
import org.matsim.contrib.pseudosimulation.controler.listeners.QSimScoreWriter;
import org.matsim.contrib.pseudosimulation.mobsim.SwitchingMobsimProvider;
import org.matsim.contrib.pseudosimulation.replanning.PSimPlanStrategyTranslationAndRegistration;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimStopStopTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimTravelTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimWaitTimeCalculator;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.pt.router.TransitRouterFactory;

import java.util.HashMap;
import java.util.LinkedHashSet;

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
	
	private final Controler matsimControler;
	public Controler getMATSimControler() {
		return matsimControler;
	}



	private LinkedHashSet<Plan> plansForPseudoSimulation = new LinkedHashSet<>();
	private LinkedHashSet<Id> agentsForPseudoSimulation = new LinkedHashSet<>();
	private HashMap<Id,Double> nonSimulatedAgentSelectedPlanScores = new HashMap<>();
	public static String AGENT_ATT = "PseudoSimAgent";
	private WaitTimeStuckCalculator waitTimeCalculator;
	private StopStopTimeCalculator stopStopTimeCalculator;
	private final PSimTravelTimeCalculator carTravelTimeCalculator;
	private final PSimPlanStrategyTranslationAndRegistration psimStrategies;



	
	public PSimControler(String[] args) {
		matsimControler = new Controler(ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0])));
		
		this.psimStrategies = new PSimPlanStrategyTranslationAndRegistration(this);
		//substitute qualifying plan strategies with their PSim equivalents
		this.substituteStrategies();
		matsimControler.setMobsimFactory(SwitchingMobsimProvider.class);
		matsimControler.addControlerListener(new MobSimSwitcher(this));
		matsimControler.addControlerListener(new QSimScoreWriter(this));
		matsimControler.addControlerListener(new BeforePSimSelectedPlanScoreRecorder(this));
		matsimControler.addControlerListener(new AfterScoringSelectedPlanScoreRestoreListener(this));
        this.carTravelTimeCalculator = new PSimTravelTimeCalculator(matsimControler.getScenario().getNetwork(),
				matsimControler.getConfig().travelTimeCalculator(),70);
		matsimControler.getEvents().addHandler(carTravelTimeCalculator);
		if (matsimControler.getConfig().scenario().isUseTransit()) {
            this.waitTimeCalculator = new PSimWaitTimeCalculator(
                    matsimControler.getScenario().getPopulation(),
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
		for (StrategyConfigGroup.StrategySettings settings : matsimControler.getConfig().strategy().getStrategySettings()) {

			String classname = settings.getStrategyName();
			
			if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
				classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
				settings.setStrategyName(classname);
			}
//			if(nonMutatingStrategies.contains(classname))
//				continue;
			if(!psimStrategies.getCompatibleStrategies().contains(classname)){
				throw new RuntimeException("Strategy "+classname+"not known to be compatible with PseudoSim. Exiting.");
			}else{
				settings.setStrategyName(classname+"PSim");
			}
			Logger.getLogger(this.getClass()).info("Mutating plan strategies prepared for PSim");
		}
		
	}


	public void addPlanForPseudoSimulation(Plan p){
		plansForPseudoSimulation.add(p);
		agentsForPseudoSimulation.add((Id) p.getPerson().getId());
	}


	
	public LinkedHashSet<Plan> getPlansForPseudoSimulation() {
		return plansForPseudoSimulation;
	}


	public void clearPlansForPseudoSimulation(){
		plansForPseudoSimulation = new LinkedHashSet<>();
		agentsForPseudoSimulation = new LinkedHashSet<>();
		nonSimulatedAgentSelectedPlanScores = new HashMap<>();
	}



	public LinkedHashSet<Id> getAgentsForPseudoSimulation() {
		return agentsForPseudoSimulation;
	}



	public HashMap<Id,Double> getNonSimulatedAgentSelectedPlanScores() {
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


	public void setTransitRouterFactory(TransitRouterFactory transitRouterFactory) {
		this.matsimControler.setTransitRouterFactory(transitRouterFactory);
		
	}


	public Scenario getScenario() {
		return matsimControler.getScenario();
	}


	public void setScoringFunctionFactory(
			CharyparNagelOpenTimesScoringFunctionFactory charyparNagelOpenTimesScoringFunctionFactory) {
		this.matsimControler.setScoringFunctionFactory(charyparNagelOpenTimesScoringFunctionFactory);
		
	}




}
