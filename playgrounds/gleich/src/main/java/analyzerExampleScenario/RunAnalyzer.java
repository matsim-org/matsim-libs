package analyzerExampleScenario;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.jdeqsim.Vehicle;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.Vehicles;

import analyzer.act2mode.Act2ModeWithPlanCoordAnalysis;
import playground.vsp.analysis.modules.ptCircuityAnalyzer.*;

import playground.vsp.analysis.modules.act2mode.ActivityToModeAnalysis;
import playground.vsp.analysis.modules.boardingAlightingCount.BoardingAlightingCountAnalyzer;
import playground.vsp.analysis.modules.bvgAna.delayAtStopHistogram.VehDelayAtStopHistogramAnalyzer;
import playground.vsp.analysis.modules.bvgAna.ptTripTravelTime.PtTripTravelTimeTransfersAnalyzer;
import playground.vsp.analysis.modules.carDistance.CarDistanceAnalyzer;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;
import playground.vsp.analysis.modules.legModeDistanceDistribution.LegModeDistanceDistribution;
import playground.vsp.analysis.modules.monetaryTransferPayments.MonetaryPaymentsAnalyzer;
import playground.vsp.analysis.modules.plansSubset.GetPlansSubset;
import playground.vsp.analysis.modules.ptAccessibility.PtAccessibility;
import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;
import playground.vsp.analysis.modules.ptOperator.PtOperatorAnalyzer;
import playground.vsp.analysis.modules.ptPaxVolumes.PtPaxVolumesAnalyzer;
import playground.vsp.analysis.modules.ptRoutes2paxAnalysis.PtRoutes2PaxAnalysis;
import playground.vsp.analysis.modules.ptTravelStats.TravelStatsAnalyzer;
import playground.vsp.analysis.modules.ptTripAnalysis.traveltime.TTtripAnalysis;
import playground.vsp.analysis.modules.stuckAgents.GetStuckEventsAndPlans;
import playground.vsp.analysis.modules.transitSchedule2Shp.TransitSchedule2Shp;
import playground.vsp.analysis.modules.transitScheduleAnalyser.TransitScheduleAnalyser;
import playground.vsp.analysis.modules.transitVehicleVolume.TransitVehicleVolumeAnalyzer;
import playground.vsp.analysis.modules.travelTime.TravelTimeAnalyzer;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsAnalyzer;
import playground.vsp.analysis.modules.waitingTimes.WaitingTimesAnalyzer;
import playground.vsp.analysis.modules.welfareAnalyzer.WelfareAnalyzer;

public class RunAnalyzer {
	
	private static Scenario scenario;
	private static String eventFile = "Z:/WinHome/ArbeitWorkspace/Analyzer/output/test1/ITERS/it.1/1.events.xml.gz";
	private static String outputDirectory = "Z:/WinHome/ArbeitWorkspace/Analyzer/output/";
	
	public static void main(String[] args){
		// Some analyzers read the result plan files
		Config config = ConfigUtils.loadConfig("Z:/WinHome/ArbeitWorkspace/Analyzer/input/config_pttutorial_resultPaths.xml"); //loads plans for iteration 0

		scenario = ScenarioUtils.loadScenario(config);
		//new MatsimNetworkReader(scenario).readFile("input/network_tut.xml");
		//new MatsimPopulationReader(scenario).readFile("output/test1/ITERS/it.10/10.plans.xml.gz");
		
		rAct2Mode();//shows the arrivals at wrong coordinates
		rAct2ModeWithPlanCoord();
		rBoardingAlightingCountAnalyzer();
		rBvgAna();// mehrere Analyzer
		rCarDistanceAnalyzer();
		/*rEmissionsAnalyzer(); */ //not included: needs an emission events file created with the package emissionsWriter which needs settings made with VspExperimentalConfigGroup which is not intended for public use
		rLegModeDistanceDistribution(); // reads the plans in the scenario -> scenario should include the most recent plans (set in config)
		rMonetaryPaymentsAnalyzer();
		rPlansSubset();
		rPtAccesibility();
		rPtCircuityAnalysis();
		rPtDriverPrefix();
		rPtOperator();
		rPtPaxVolumes();
		rPtRoutes2PaxAnalysis();
		rPtTravelStats();
		/*rPtTripAnalysis(); */ // not working and not included
		rStuckAgents();
		rTransitSchedule2Shp();
		rTransitScheduleAnalyser();
		rTransitVehicleVolume();
		rTravelTimeAnalyzer();
		rUserBenefits();
		rWaitingTimes();
		rWelfareAnalyzer();
	}
	
	private static void rAct2Mode() {
		//works without facilities, although stated to be necessary in ActivityToModeAnalysis
		Set<Id> personsOfInterest = new TreeSet<Id>();

		for(Id id: scenario.getPopulation().getPersons().keySet()){
			personsOfInterest.add(id);
		}
		ActivityToModeAnalysis analysis = new ActivityToModeAnalysis(scenario, personsOfInterest, 30*60, "DHDN_GK4");
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = analysis.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		analysis.postProcessData();
		(new File(outputDirectory + "Act2Mode")).mkdir(); 
		analysis.writeResults(outputDirectory + "Act2Mode/");
	}

	private static void rAct2ModeWithPlanCoord() {
		Set<Id> personsOfInterest = new TreeSet<Id>();
		//personsOfInterest.add(new IdImpl("car_11_to_12_Nr100"));
		
		for(Id id: scenario.getPopulation().getPersons().keySet()){
			personsOfInterest.add(id);
		}
		
		Act2ModeWithPlanCoordAnalysis analysis = new Act2ModeWithPlanCoordAnalysis(scenario, personsOfInterest, 30*60, "DHDN_GK4");
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = analysis.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		analysis.postProcessData();
		(new File(outputDirectory + "Act2ModeWithPlanCoord")).mkdir(); 
		analysis.writeResults(outputDirectory + "Act2ModeWithPlanCoord/");
	}

	private static void rBvgAna(){
		VehDelayAtStopHistogramAnalyzer cda = new VehDelayAtStopHistogramAnalyzer(100);
		cda.init((ScenarioImpl) scenario);
		cda.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = cda.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		cda.postProcessData();
		(new File(outputDirectory + "BvgAna")).mkdir(); 
		cda.writeResults(outputDirectory + "BvgAna/");
		
		PtTripTravelTimeTransfersAnalyzer transfer = new PtTripTravelTimeTransfersAnalyzer();
		transfer.init((ScenarioImpl) scenario);
		transfer.preProcessData();
		EventsManager events2 = EventsUtils.createEventsManager();
		List<EventHandler> handler2 = transfer.getEventHandler();
		for(EventHandler eh : handler2){
			events2.addHandler(eh);
		}
		MatsimEventsReader reader2 = new MatsimEventsReader(events2);
		reader2.readFile(eventFile);
		transfer.postProcessData();
		transfer.writeResults(outputDirectory + "BvgAna/");
		//pTripTransfers.txt empty due to no transfers in the example Scenario
	}

	private static void rBoardingAlightingCountAnalyzer(){
		BoardingAlightingCountAnalyzer ba = new BoardingAlightingCountAnalyzer(scenario, 30*60, "DHDN_GK4");
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = ba.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		ba.postProcessData();
		(new File(outputDirectory + "BoardingAlightingCountAnalyzer")).mkdir(); 
		ba.writeResults(outputDirectory + "BoardingAlightingCountAnalyzer/");
	}
	
	private static void rCarDistanceAnalyzer() {
		CarDistanceAnalyzer cda = new CarDistanceAnalyzer();
		cda.init((ScenarioImpl) scenario);
		cda.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = cda.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		cda.postProcessData();
		(new File(outputDirectory + "CarDistanceAnalyzer")).mkdir(); 
		cda.writeResults(outputDirectory + "CarDistanceAnalyzer/");
	}
	
	private static void rEmissionsAnalyzer() {
		 //insert emissions event file
		EmissionsAnalyzer ema = new EmissionsAnalyzer("Z:/WinHome/ArbeitWorkspace/Analyzer/output/test1/ITERS/it.10/10.events.xml.gz");
		ema.init((ScenarioImpl) scenario);
		ema.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = ema.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		ema.postProcessData();
		(new File(outputDirectory + "EmissionsAnalyzer")).mkdir(); 
		ema.writeResults(outputDirectory + "EmissionsAnalyzer/");
	}
	
	private static void rLegModeDistanceDistribution(){
		LegModeDistanceDistribution lmdd = new LegModeDistanceDistribution();
		lmdd.init(scenario);
		lmdd.postProcessData();
		(new File(outputDirectory + "LegModeDistanceDistribution")).mkdir(); 
		lmdd.writeResults(outputDirectory + "LegModeDistanceDistribution/");
	}
	
	private static void rMonetaryPaymentsAnalyzer(){
		MonetaryPaymentsAnalyzer mpa = new MonetaryPaymentsAnalyzer();
		mpa.init((ScenarioImpl) scenario);
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = mpa.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		mpa.postProcessData();
		(new File(outputDirectory + "MonetaryPaymentsAnalyzer")).mkdir(); 
		mpa.writeResults(outputDirectory + "MonetaryPaymentsAnalyzer/");
	}
	
	private static void rPlansSubset(){
		Set<Id> selection = new TreeSet<Id>();
		selection.add(new IdImpl(2));
		selection.add(new IdImpl(256));
		GetPlansSubset gps = new GetPlansSubset(scenario, selection, true);
		//gps.preProcessData(); //unused, no EventHandler
		gps.postProcessData();
		(new File(outputDirectory + "PlansSubset")).mkdir(); 
		gps.writeResults(outputDirectory + "PlansSubset/");
	}
	
	private static void rPtAccesibility(){
		List<Integer> distanceCluster = new ArrayList<Integer>();
		distanceCluster.add(200);
		distanceCluster.add(1000);
		SortedMap<String, List<String>> activityCluster = new TreeMap<String, List<String>>();
		List<String> home = new ArrayList<String>();
		home.add("h");
		List<String> work = new ArrayList<String>();
		work.add("w");
		List<String> shop = new ArrayList<String>();
		shop.add("s");//unused in scenario
		activityCluster.put("h", home);
		activityCluster.put("w", work);
		activityCluster.put("s", shop);
		PtAccessibility pta = new PtAccessibility(scenario, distanceCluster, 16, activityCluster, "DHDN_GK4", 10);
		pta.preProcessData();
		pta.postProcessData();
		(new File(outputDirectory + "PtAccessibility")).mkdir(); 
		pta.writeResults(outputDirectory + "PtAccessibility/");
	}
	
	private static void rPtCircuityAnalysis(){
		ScenarioImpl sc = (ScenarioImpl) scenario;
		Vehicles vehicles = sc.getVehicles();
		PtCircuityAnalyzer analysis = new PtCircuityAnalyzer(scenario, vehicles);
		analysis.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = analysis.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		analysis.postProcessData();
		(new File(outputDirectory + "PtCircuityAnalysis")).mkdir(); 
		analysis.writeResults(outputDirectory + "PtCircuityAnalysis/");
	}
	
	private static void rPtDriverPrefix(){
		PtDriverIdAnalyzer pda = new PtDriverIdAnalyzer();
		pda.init((ScenarioImpl)scenario);
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = pda.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		(new File(outputDirectory + "PtDriverIdAnalyzer")).mkdir(); 
		pda.writeResults(outputDirectory + "PtDriverIdAnalyzer/");//no output file, only console
	}
	
	private static void rPtOperator(){
		PtOperatorAnalyzer poa = new PtOperatorAnalyzer();
		poa.init((ScenarioImpl)scenario);
		poa.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = poa.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		poa.postProcessData();
		(new File(outputDirectory + "PtOperator")).mkdir(); 
		poa.writeResults(outputDirectory + "PtOperator/");
	}
	
	private static void rPtPaxVolumes(){
		PtPaxVolumesAnalyzer ppv = new PtPaxVolumesAnalyzer(scenario, 30.0*60.0, "DHDN_GK4");
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = ppv.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		ppv.postProcessData();
		(new File(outputDirectory + "PtPaxVolumes")).mkdir(); 
		ppv.writeResults(outputDirectory + "PtPaxVolumes/");
	}
	
	private static void rPtRoutes2PaxAnalysis(){
		
		Map<Id, TransitLine> lines = scenario.getTransitSchedule().getTransitLines();
		//scenario.getScenarioElement(vehicles)
		ScenarioImpl sc = (ScenarioImpl) scenario;

		Vehicles vehicles = sc.getVehicles();
		//(Map<Id, TransitLine> lines, Vehicles vehicles, double interval, int maxSlices)
		PtRoutes2PaxAnalysis ppa = new PtRoutes2PaxAnalysis(lines, vehicles, 60*60, 24*(60/60));
		
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = ppa.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		(new File(outputDirectory + "PtRoutes2PaxAnalysis")).mkdir(); 
		ppa.writeResults(outputDirectory + "PtRoutes2PaxAnalysis/");
	}
	
	private static void rPtTravelStats(){
		TravelStatsAnalyzer tsa = new TravelStatsAnalyzer(scenario, 30.0*60.0);
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = tsa.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		(new File(outputDirectory + "PtTravelStats")).mkdir(); 
		tsa.writeResults(outputDirectory + "PtTravelStats/");
	}
	
	private static void rPtTripAnalysis(){
		List<String> ptModes = new LinkedList<String>();
		ptModes.add("bus");
		ptModes.add("train");
		List<String> networkModes = new LinkedList<String>();
		networkModes.add("train");
		networkModes.add("car");
		networkModes.add("bus");
		TTtripAnalysis analysis = new TTtripAnalysis(ptModes, networkModes, scenario.getPopulation());
		(new File(outputDirectory + "PtTripAnalysis")).mkdir(); 
		analysis.writeResults(outputDirectory + "PtTripAnalysis/");
	}
	
	private static void rStuckAgents(){
		GetStuckEventsAndPlans stuck = new GetStuckEventsAndPlans(scenario);
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = stuck.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		stuck.postProcessData();
		(new File(outputDirectory + "StuckAgents")).mkdir(); 
		stuck.writeResults(outputDirectory + "StuckAgents/");
		//empty due to no stuck events to be written
	}
	
	private static void rTransitSchedule2Shp(){
		TransitSchedule2Shp tshp = new TransitSchedule2Shp(scenario, "DHDN_GK4");
		(new File(outputDirectory + "TransitSchedule2Shp")).mkdir(); 
		tshp.writeResults(outputDirectory + "TransitSchedule2Shp/");
	}
	
	private static void rTransitScheduleAnalyser(){
		TransitScheduleAnalyser tsa = new TransitScheduleAnalyser(scenario);
		(new File(outputDirectory + "TransitScheduleAnalyser")).mkdir(); 
		tsa.writeResults(outputDirectory + "TransitScheduleAnalyser/");
	}
	
	private static void rTransitVehicleVolume(){
		TransitVehicleVolumeAnalyzer tsa = new TransitVehicleVolumeAnalyzer(scenario, 30.0*60.0, "DHDN_GK4");
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = tsa.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		tsa.postProcessData();
		(new File(outputDirectory + "TransitVehicleVolume")).mkdir(); 
		tsa.writeResults(outputDirectory + "TransitVehicleVolume/");
	}
		
	private static void rTravelTimeAnalyzer() {
		TravelTimeAnalyzer tt = new TravelTimeAnalyzer();
		tt.init((ScenarioImpl) scenario);
		tt.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = tt.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		tt.postProcessData();
		(new File(outputDirectory + "TravelTimeAnalyzer")).mkdir(); 
		tt.writeResults(outputDirectory + "TravelTimeAnalyzer/");
	}

	private static void rUserBenefits() {
		UserBenefitsAnalyzer uba = new UserBenefitsAnalyzer();
		uba.init((ScenarioImpl) scenario);
		uba.preProcessData();
		(new File(outputDirectory + "UserBenefits")).mkdir(); 
		uba.writeResults(outputDirectory + "UserBenefits/");
		
	}
	
	private static void rWaitingTimes(){
		WaitingTimesAnalyzer tt = new WaitingTimesAnalyzer();
		tt.init((ScenarioImpl) scenario);
		tt.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = tt.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		tt.postProcessData();
		(new File(outputDirectory + "WaitingTimes")).mkdir(); 
		tt.writeResults(outputDirectory + "WaitingTimes/");
	}
	
	private static void rWelfareAnalyzer(){
		WelfareAnalyzer tt = new WelfareAnalyzer();
		tt.init((ScenarioImpl) scenario);
		tt.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = tt.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		tt.postProcessData();
		(new File(outputDirectory + "WelfareAnalyzer")).mkdir(); 
		tt.writeResults(outputDirectory + "WelfareAnalyzer/");
	}
}
