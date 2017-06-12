package playground.ikaddoura.analysis.detailedPersonTripAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.events.NoiseEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.CongestionAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.NoiseAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.old.PersonTripBasicAnalysis;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.old.PersonTripCongestionNoiseAnalysis;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.old.PersonTripCongestionNoiseAnalysisRun;
import playground.ikaddoura.analysis.vtts.VTTSHandler;
import playground.tschlenther.createNetwork.ForkNetworkCreator;
import playground.vsp.congestion.events.CongestionEventsReader;


public class PersonTripAnalysisTest {

	private static final Logger log = Logger.getLogger(PersonTripCongestionNoiseAnalysisRun.class);

	private String outputPath;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Before
	public void generateOutput() {
		
		Config config = ConfigUtils.createConfig();
		ActivityParams activityParams = new ActivityParams();
		activityParams.setActivityType("work");
		config.planCalcScore().addActivityParams(activityParams);
		activityParams = new ActivityParams();
		activityParams.setActivityType("home");
		config.planCalcScore().addActivityParams(activityParams);
		config.plans().setInputFile(utils.getClassInputDirectory() + "PersonTripAnalysisPlans.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		ForkNetworkCreator fnc = new ForkNetworkCreator(scenario, false, false);
		fnc.createNetwork();
		
		String eventsFile = utils.getClassInputDirectory() + "PersonTripAnalysisEvents.xml";
		outputPath = utils.getOutputDirectory();
		File outputFolder = new File(outputPath);			
		outputFolder.mkdirs();
		
		// standard events analysis
	
		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler();
		basicHandler.setScenario(scenario);

		VTTSHandler vttsHandler = new VTTSHandler(scenario);
		CongestionAnalysisHandler congestionHandler = new CongestionAnalysisHandler(basicHandler);
		NoiseAnalysisHandler noiseHandler = new NoiseAnalysisHandler();
		noiseHandler.setBasicHandler(basicHandler);
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(basicHandler);
		events.addHandler(vttsHandler);
		events.addHandler(congestionHandler);
		events.addHandler(noiseHandler);
		
		log.info("Reading the events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		log.info("Reading the events file... Done.");

		vttsHandler.computeFinalVTTS();
				
		// plans
		
		Map<Id<Person>, Double> personId2userBenefit = new HashMap<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			personId2userBenefit.put(person.getId(), person.getSelectedPlan().getScore() / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney());
		}

		// congestion events analysis
		
		if (congestionHandler.isCaughtCongestionEvent()) {
			log.info("Congestion events have already been analyzed based on the standard events file.");
			
		} else {
			EventsManager eventsCongestion = EventsUtils.createEventsManager();
			eventsCongestion.addHandler(congestionHandler);
	
			log.info("Reading the congestion events file...");
			CongestionEventsReader congestionEventsReader = new CongestionEventsReader(eventsCongestion);		
			congestionEventsReader.readFile(eventsFile);
			log.info("Reading the congestion events file... Done.");		
		}	
		
		// noise events analysis
	
		if (noiseHandler.isCaughtNoiseEvent()) {
			log.info("Noise events have already been analyzed based on the standard events file.");
		} else {
			EventsManager eventsNoise = EventsUtils.createEventsManager();
			eventsNoise.addHandler(noiseHandler);
					
			log.info("Reading noise events file...");
			NoiseEventsReader noiseEventReader = new NoiseEventsReader(eventsNoise);		
			noiseEventReader.readFile(eventsFile);
			log.info("Reading noise events file... Done.");	
		}	
		
		// print the results
		
		PersonTripCongestionNoiseAnalysis analysis = new PersonTripCongestionNoiseAnalysis();
		PersonTripBasicAnalysis basicAnalysis = new PersonTripBasicAnalysis();

		log.info("Print trip information...");
		analysis.printTripInformation(outputPath, TransportMode.car, basicHandler, vttsHandler, congestionHandler, noiseHandler);
		analysis.printTripInformation(outputPath, null, basicHandler, vttsHandler, congestionHandler, noiseHandler);
		log.info("Print trip information... Done.");

		log.info("Print person information...");
		analysis.printPersonInformation(outputPath, TransportMode.car, personId2userBenefit, basicHandler, vttsHandler, congestionHandler, noiseHandler);	
		analysis.printPersonInformation(outputPath, null, personId2userBenefit, basicHandler, vttsHandler, congestionHandler, noiseHandler);	
		log.info("Print person information... Done.");
		
		SortedMap<Double, List<Double>> departureTime2tolls = basicAnalysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2payment(), 3600., 30 * 3600.);
		basicAnalysis.printAvgValuePerParameter(outputPath + "tollsPerDepartureTime_car.csv", departureTime2tolls);
		
		SortedMap<Double, List<Double>> tripDistance2tolls = basicAnalysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2tripDistance(), basicHandler.getPersonId2tripNumber2payment(), 2000., 40 * 1000.);
		basicAnalysis.printAvgValuePerParameter(outputPath + "tollsPerTripDistance_car.csv", tripDistance2tolls);
		
		analysis.printAggregatedResults(outputPath, TransportMode.car, personId2userBenefit, basicHandler, vttsHandler, congestionHandler, noiseHandler);
		analysis.printAggregatedResults(outputPath, null, personId2userBenefit, basicHandler, vttsHandler, congestionHandler, noiseHandler);
	}
	
	@Test
	public void testPersonAndTripInfo(){
		File personInfoFile = new File(outputPath + "person_info_car.csv");	
		File tripInfoFile = new File(outputPath + "trip_info_car.csv");	
		ArrayList<String[]> personInfos = new ArrayList<String[]>();
		ArrayList<String[]> tripInfos = new ArrayList<String[]>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(personInfoFile));
			String line = br.readLine();
			while(line != null) {
				if (line.startsWith("person")) {
					Assert.assertTrue("Output changed!?", line.equals("person Id;number of car trips;" +
							"at least one stuck and abort car trip (yes/no);avg. VTTS per car trip [monetary units per hour];" +
							"car total travel time (day) [sec];car total travel distance (day) [m];" +
							"travel related user benefits (based on the selected plans score) [monetary units];" +
							"total toll payments (day) [monetary units];caused noise cost (day) [monetary units];" +
							"affected noise cost (day) [monetary units];caused congestion (day) [sec];" +
							"affected congestion (day) [sec];affected congestion cost (day) [monetary units]"));
				} else {
					personInfos.add(line.split(";"));
				}
				line = br.readLine();
			}
			br.close();
			
			br = new BufferedReader(new FileReader(tripInfoFile));
			line = br.readLine();
			while(line != null) {
				if (line.startsWith("person")) {
					Assert.assertTrue("Output changed!?", line.equals("person Id;trip no.;mode;stuck and abort trip (yes/no);" +
							"VTTS (trip) [monetary units per hour];departure time (trip) [sec];arrival time (trip) [sec];" +
							"travel time (trip) [sec];travel distance (trip) [m];affected congestion (trip) [sec];" +
							"affected congestion cost (trip) [monetary units];caused congestion (trip) [sec];" +
							"approximate caused noise cost (trip) [monetary units]"));
				} else {
					tripInfos.add(line.split(";"));
				}
				line = br.readLine();
			}
			br.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<Id<Person>, Double> travelTimeSums= new HashMap<Id<Person>, Double>();
		Map<Id<Person>, Double> travelDistanceSums= new HashMap<Id<Person>, Double>();
		for (String[] line : tripInfos) {
			if (!travelTimeSums.containsKey(Id.createPersonId(line[0]))) {
				travelTimeSums.put(Id.createPersonId(line[0]), Double.parseDouble(line[7]));
				travelDistanceSums.put(Id.createPersonId(line[0]), Double.parseDouble(line[8]));
			} else {
				double cache = travelTimeSums.get(Id.createPersonId(line[0]));
				travelTimeSums.put(Id.createPersonId(line[0]), Double.parseDouble(line[7]) + cache);
				cache = travelDistanceSums.get(Id.createPersonId(line[0]));
				travelDistanceSums.put(Id.createPersonId(line[0]), Double.parseDouble(line[8]) + cache);
			}
		}
		for (String[] personInfo : personInfos) {
			Assert.assertEquals("TravelTimes are not equal for Person " + personInfo[0],
					travelTimeSums.get(Id.createPersonId(personInfo[0])), Double.parseDouble(personInfo[4]), MatsimTestUtils.EPSILON);
			Assert.assertEquals("TravelDistances are not equal for Person " + personInfo[0],
					travelDistanceSums.get(Id.createPersonId(personInfo[0])), Double.parseDouble(personInfo[5]), MatsimTestUtils.EPSILON);
		}
		
		// test scoring of the selected plans of both agents (where other plans exist)
		Assert.assertEquals("Scoring of selected plan does not work right for Person " + Id.createPersonId(0),
				987.654321, Double.parseDouble(personInfos.get(0)[6]), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Scoring of selected plan does not work right for Person " + Id.createPersonId(1),
				123.456789, Double.parseDouble(personInfos.get(1)[6]), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void testAggregatedInfo(){
		File aggregatedInfoFile = new File(outputPath + "aggregated_info_car.csv");	
		ArrayList<String[]> aggregatedInfos = new ArrayList<String[]>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(aggregatedInfoFile));
			String line = br.readLine();
			while(line != null) {
				aggregatedInfos.add(line.split(";"));
				System.out.println(line);
				line = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Assert.assertEquals("Number of car trips wrong", 3, Integer.parseInt(aggregatedInfos.get(2)[1]));
		Assert.assertEquals("Car travel distance wrong", 12.0, 
				Double.parseDouble(aggregatedInfos.get(5)[1]), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Car travel time wrong", 0.5138888888888888, 
				Double.parseDouble(aggregatedInfos.get(7)[1]), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Total congestion wrong", 0.027777777777777776, 
				Double.parseDouble(aggregatedInfos.get(12)[1]), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Affected noise cost wrong", 100.0, 
				Double.parseDouble(aggregatedInfos.get(15)[1]), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Caused noise cost wrong", 100.0, 
				Double.parseDouble(aggregatedInfos.get(16)[1]), MatsimTestUtils.EPSILON);
	}
}