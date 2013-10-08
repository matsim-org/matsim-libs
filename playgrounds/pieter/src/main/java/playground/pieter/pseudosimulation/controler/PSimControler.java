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
public class PSimControler extends Controler{
	
	
	private LinkedHashSet<Plan> plansForPseudoSimulation = new LinkedHashSet<Plan>();
	private LinkedHashSet<IdImpl> agentsForPseudoSimulation = new LinkedHashSet<IdImpl>();
	private HashMap<IdImpl,Double> nonSimulatedAgentSelectedPlanScores = new HashMap<IdImpl, Double>(); 
	public static String AGENT_ATT = "PseudoSimAgent";
	private WaitTimeStuckCalculator waitTimeCalculator;
	private StopStopTimeCalculator stopStopTimeCalculator;
	private PSimTravelTimeCalculator carTravelTimeCalculator;
	private PSimPlanStrategyRegistrar psimStrategies;



	
	public PSimControler(String[] args) {
		super(ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0])));
		this.psimStrategies = new PSimPlanStrategyRegistrar(this);
		//substitute qualifying plan strategies with their PSim equivalents
		this.substituteStrategies();
		this.addControlerListener(new MobSimSwitcher(this));
		this.addControlerListener(new QSimScoreWriter(this));
		this.addControlerListener(new BeforePSimSelectedPlanScoreRecorder(this));
		this.addControlerListener(new AfterScoringSelectedPlanScoreRestoreListener(this));
		this.carTravelTimeCalculator = new PSimTravelTimeCalculator(getNetwork(),
				getConfig().travelTimeCalculator(),70);
		this.getEvents().addHandler(carTravelTimeCalculator);
		if (this.getConfig().scenario().isUseTransit()) {
			this.waitTimeCalculator = new PSimWaitTimeCalculator(
					this.getPopulation(),
					this.getScenario().getTransitSchedule(),
					this.getConfig().travelTimeCalculator()
							.getTraveltimeBinSize(),
					(int) (this.getConfig().qsim().getEndTime() - this
							.getConfig().qsim().getStartTime()));
			this.getEvents().addHandler(waitTimeCalculator);
			this.stopStopTimeCalculator = new PSimStopStopTimeCalculator(
					this.getScenario().getTransitSchedule(),
					 this
							.getConfig().travelTimeCalculator()
							.getTraveltimeBinSize(), (int) (this.getConfig()
							.qsim().getEndTime() - this
							.getConfig().qsim().getStartTime()));
			this.getEvents().addHandler(stopStopTimeCalculator);
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
		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {

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

//	protected void loadControlerListeners() {
//		// Cannot make this method final since is is overridden about 13 times.  kai, jan'13
//		// Yet it looks like this will remain non-final since it makes some sense to override these (with or without super....).
//		// The core controler listeners are separate, after all.  kai, feb'13
//		// yy On the other hand, we could write a method clearControlerListeners() and one would have a similar flexibility without
//		// inheritance.  kai, may'13
//		// zz vote for clearControlerListeners(). dg, may'13
//
//		// optional: LegHistogram
//		this.addControlerListener(new LegHistogramListener(this.events, this.getCreateGraphs()));
//	
//		// optional: score stats
//		this.scoreStats = new ScoreStats(this.population,
//				this.getControlerIO().getOutputFilename(FILENAME_SCORESTATS), this.getCreateGraphs());
//		this.addControlerListener(this.getScoreStats());
//	
//		// optional: travel distance stats
//		this.travelDistanceStats = new TravelDistanceStats(this.population, this.network,
//				this.getControlerIO() .getOutputFilename(FILENAME_TRAVELDISTANCESTATS), this.this.getCreateGraphs());
//		this.addControlerListener(this.travelDistanceStats);
//		this.controlerListenerManager.removeControlerListener(LegHistogramListener.class);
//	
//		// load counts, if requested
//		if (this.config.counts().getCountsFileName() != null) {
//			CountControlerListener ccl = new CountControlerListener(this.config.counts());
//			this.addControlerListener(ccl);
//			this.counts = ccl.getCounts();
//		}
//	
//		if (this.config.linkStats().getWriteLinkStatsInterval() > 0) {
//			this.addControlerListener(new LinkStatsControlerListener(this.config.linkStats()));
//		}
//	
//		if (this.config.scenario().isUseTransit()) {
//			if (this.config.ptCounts().getAlightCountsFileName() != null) {
//				// only works when all three files are defined! kai, oct'10
//				addControlerListener(new PtCountControlerListener(this.config));
//			}
//		}
//	
//	
//		if ( !this.config.vspExperimental().getActivityDurationInterpretation().equals(ActivityDurationInterpretation.minOfDurationAndEndTime)
//				|| this.config.vspExperimental().isRemovingUnneccessaryPlanAttributes() ) {
//			addControlerListener(new VspPlansCleaner());
//		}
//	
//	}


}
