package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import opdytsintegration.TimeDiscretization;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import saleem.stockholmscenario.utils.CollectionUtil;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;

public class OptimisePT {
	@SuppressWarnings({ "rawtypes", "unused" })
	public static void main(String[] args) {
		System.out.println("STARTED ...");
		String path = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\config.xml";
        Config config = ConfigUtils.loadConfig(path);
        MatsimServices controler = new Controler(config);
        final Scenario scenario = ScenarioUtils.loadScenario(config);
        double samplesize = config.qsim().getStorageCapFactor();
		final String originalOutputDirectory = scenario.getConfig().controler()
				.getOutputDirectory(); // gets otherwise overwritten in config
		final AbstractModule module = new ControlerDefaultsModule();
        final int maxMemorizedTrajectoryLength = Integer.MAX_VALUE;
		final boolean interpolate = true;
		final int maxRandomSearchIterations = 500;
		final int maxRandomSearchTransitions = Integer.MAX_VALUE;
		final boolean includeCurrentBest = false;
		final ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(
				4, 2);
		final TimeDiscretization timeDiscretization = new TimeDiscretization(0,
				1800, 48);
		final ObjectiveFunction objectiveFunction = new PTObjectiveFunction();
		Network network = scenario.getNetwork();
		Vehicles vehicles = scenario.getTransitVehicles();
		CollectionUtil<VehicleType> cutil = new CollectionUtil<VehicleType>();
		ArrayList<VehicleType> vehcilestypes = cutil.toArrayList(vehicles.getVehicleTypes().values().iterator());
		Iterator vehtypes = vehcilestypes.iterator();
		while(vehtypes.hasNext()){
			VehicleType vt = (VehicleType)vehtypes.next();
			VehicleCapacity cap = vt.getCapacity();
			cap.setSeats((int)Math.ceil(cap.getSeats()*samplesize));
			cap.setStandingRoom((int)Math.ceil(cap.getStandingRoom()*samplesize));
			vt.setCapacity(cap);
			vt.setPcuEquivalents(vt.getPcuEquivalents()*samplesize);
			System.out.println("Sample Size is: " + samplesize);
		}
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
		NetworkWriter networkWriter =  new NetworkWriter(network);
		networkWriter.write("H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\PseudoNetwork.xml");
		final PTSchedule ptschedule = new PTSchedule(scenario, scenario.getTransitSchedule(),scenario.getTransitVehicles());
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
		randomSearch.run();

		System.out.println("... DONE.");
	}
}
