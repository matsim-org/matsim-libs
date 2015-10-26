package playground.ikaddoura.analysis.detailedPersonTripAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.CongestionAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.NoiseAnalysisHandler;
import playground.ikaddoura.analysis.vtts.VTTSHandler;
import playground.tschlenther.createNetwork.ForkNetworkCreator;
import playground.vsp.congestion.events.CongestionEventsReader;

/**
 * @author gthunig
 * 
 * This class tests the Basic Person Trip Analysis which is provided by the
 * BasicPersonTripAnalysisHandler. 
 * Therefor several scenarios are created which are declared above their testmethod.
 * Every scenario has its own method.
 * Every scenario has the same network which is declared in its class; 
 * current status: ForkNetworkCreator
 */
public class BasicPersonTripAnalysisTest {
	
	private static final Logger log = Logger.getLogger(PersonTripAnalysisMain.class);
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
//	TODO when transport mode not car then its assumed that its pt but what is with bike?
	
	/**
	 * Scenario: 4 Persons
	 * 	1.Person: 2 different trips
	 *  2.Person: 3 different trips
	 *  3.Person: no trips; activity "home" ends and activity "work" starts on the same link
	 *  4.Person: no trips; no occurrence in the events-file
	 */
	@Test
	public void testVariousTripCounts() {
		
		String eventsFile = utils.getInputDirectory() + "testVariousTripCountsEvents.xml";
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		// create Population
		scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(Id.create("0", Person.class)));
		scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(Id.create("1", Person.class)));
		scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(Id.create("2", Person.class)));
		scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(Id.create("3", Person.class)));
		
		ForkNetworkCreator fnc = new ForkNetworkCreator(scenario, false, false);
		fnc.createNetwork();
				
		// analysis
		
		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler(scenario);	
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(basicHandler);
		
		log.info("Reading the events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		log.info("Reading the events file... Done.");
		
		System.out.println("totalPayments: " + basicHandler.getTotalPayments());
		for (Person person : scenario.getPopulation().getPersons().values()) {
			System.out.println("Person Id : " + person.getId());
			System.out.println("currentTripNumber: " + basicHandler.getPersonId2currentTripNumber().get(person.getId()));
			System.out.println("distanceEnterValue: " + basicHandler.getPersonId2distanceEnterValue().get(person.getId()));
			System.out.println("tripNumber2amount: " + basicHandler.getPersonId2tripNumber2amount().get(person.getId()));
			System.out.println("tripNumber2arrivalTime: " + basicHandler.getPersonId2tripNumber2arrivalTime().get(person.getId()));
			System.out.println("tripNumber2departureTime: " + basicHandler.getPersonId2tripNumber2departureTime().get(person.getId()));
			System.out.println("tripNumber2legMode: " + basicHandler.getPersonId2tripNumber2legMode().get(person.getId()));
			System.out.println("tripNumber2stuckAbort: " + basicHandler.getPersonId2tripNumber2stuckAbort().get(person.getId()));
			System.out.println("tripNumber2travelTime: " + basicHandler.getPersonId2tripNumber2travelTime().get(person.getId()));
			System.out.println("tripNumber2tripDistance: " + basicHandler.getPersonId2tripNumber2tripDistance().get(person.getId()));
			
		}
	}
	
	/**
	 * Scenario: 1 Persons
	 * 	1.Person: 2 different trips with different vehicles
	 * current status: Fail; because vehicle id is assumed to be always the same as person id
	 */
	@Test
	public void testVariousVehiclesPerPerson() {
		
		String eventsFile = utils.getInputDirectory() + "testVariousVehiclesPerPersonEvents.xml";
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		// create Population
		scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(Id.create("0", Person.class)));
		scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(Id.create("1", Person.class)));
		
		ForkNetworkCreator fnc = new ForkNetworkCreator(scenario, false, false);
		fnc.createNetwork();
				
		// analysis
		
		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler(scenario);	
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(basicHandler);
		
		log.info("Reading the events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		log.info("Reading the events file... Done.");
		
		System.out.println("totalPayments: " + basicHandler.getTotalPayments());
		for (Person person : scenario.getPopulation().getPersons().values()) {
			System.out.println("Person Id : " + person.getId());
			System.out.println("currentTripNumber: " + basicHandler.getPersonId2currentTripNumber().get(person.getId()));
			System.out.println("distanceEnterValue: " + basicHandler.getPersonId2distanceEnterValue().get(person.getId()));
			System.out.println("tripNumber2amount: " + basicHandler.getPersonId2tripNumber2amount().get(person.getId()));
			System.out.println("tripNumber2arrivalTime: " + basicHandler.getPersonId2tripNumber2arrivalTime().get(person.getId()));
			System.out.println("tripNumber2departureTime: " + basicHandler.getPersonId2tripNumber2departureTime().get(person.getId()));
			System.out.println("tripNumber2legMode: " + basicHandler.getPersonId2tripNumber2legMode().get(person.getId()));
			System.out.println("tripNumber2stuckAbort: " + basicHandler.getPersonId2tripNumber2stuckAbort().get(person.getId()));
			System.out.println("tripNumber2travelTime: " + basicHandler.getPersonId2tripNumber2travelTime().get(person.getId()));
			System.out.println("tripNumber2tripDistance: " + basicHandler.getPersonId2tripNumber2tripDistance().get(person.getId()));
			
		}
	}
}
