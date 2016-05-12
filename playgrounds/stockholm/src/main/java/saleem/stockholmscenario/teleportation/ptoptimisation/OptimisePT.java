package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.Set;

import opdytsintegration.TimeDiscretization;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.CreatePseudoNetwork;

import saleem.stockholmscenario.teleportation.PTCapacityAdjusmentPerSample;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;

public class OptimisePT {
	@SuppressWarnings({ "rawtypes", "unused" })
	public static void main(String[] args) {
		System.out.println("STARTED ...");
		
		String path = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\config.xml";
//		String path = "/home/saleem/input/config.xml";
        Config config = ConfigUtils.loadConfig(path);
        MatsimServices controler = new Controler(config);
        final Scenario scenario = ScenarioUtils.loadScenario(config);
        double samplesize = config.qsim().getStorageCapFactor();
		final String originalOutputDirectory = scenario.getConfig().controler()
				.getOutputDirectory(); // gets otherwise overwritten in config
		final AbstractModule module = new ControlerDefaultsModule();
 
		
		final int maxMemorizedTrajectoryLength = Integer.MAX_VALUE;  // revisit this based on available RAM
		final boolean interpolate = true; // always
		final int maxRandomSearchIterations = 500; // comp. time per iteration is approx. duration of two full simulations
		final int maxRandomSearchTransitions = Integer.MAX_VALUE; // revisit this later
		final boolean includeCurrentBest = false; // always
		final ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(
				100, 50);
		final TimeDiscretization timeDiscretization = new TimeDiscretization(0,
				3600, 24); // OK to start with
		final ObjectiveFunction objectiveFunction = new PTObjectiveFunction(scenario); // TODO this is minimized
		
		// Changing vehicle and road capacity according to sample size
		PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
		capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
		
		Network network = scenario.getNetwork();
		TransitSchedule schedule = scenario.getTransitSchedule();
		new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
//		NetworkWriter networkWriter =  new NetworkWriter(network);
//		networkWriter.write("/home/saleem/input/PseudoNetwork.xml");
//		networkWriter.write("H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\PseudoNetwork.xml");
		final PTSchedule ptschedule = new PTSchedule(scenario, scenario.getTransitSchedule(),scenario.getTransitVehicles());//Decision Variable
		
			
		final DecisionVariableRandomizer<PTSchedule> decisionVariableRandomizer = 
				new PTScheduleRandomiser(scenario, ptschedule);
		
		
//		Map<Id<TransitStopFacility>, TransitStopFacility> stopFacilities = scenario.getTransitSchedule().getFacilities();
		final Set<Id<TransitStopFacility>> relevantStopIds = scenario.getTransitSchedule().getFacilities().keySet();
		final double occupancyScale = 1;
		
		
		@SuppressWarnings("unchecked")		
		final PTMATSimSimulator<PTSchedule> matsimSimulator = new PTMATSimSimulator(
				new PTStateFactory(timeDiscretization, occupancyScale), scenario, timeDiscretization,
				relevantStopIds,  module);
		final RandomSearch<PTSchedule> randomSearch = new RandomSearch<>(
				matsimSimulator, decisionVariableRandomizer, ptschedule,
				convergenceCriterion, maxRandomSearchIterations,
				maxRandomSearchTransitions, 2,
				MatsimRandom.getRandom(), interpolate, objectiveFunction,
				includeCurrentBest);
		randomSearch.setLogFileName(originalOutputDirectory + "opdyts.log");
		randomSearch.setConvergenceTrackingFileName(originalOutputDirectory
				+ "opdyts.con");
		randomSearch.setOuterIterationLogFileName(originalOutputDirectory
				+ "opdyts.opt");
		// randomSearch.run(0.0, 0.0); // TODO change this to adaptive gap weights
		final SelfTuner tuner = new SelfTuner(0.95);
		tuner.setNoisySystem(true);
		randomSearch.run(tuner);
		System.out.println("... DONE.");
	}
}
