package playground.ikaddoura.analysis.detailedPersonTripAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
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

	private static final boolean printResults = false;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	/**
	 * Scenario: 2 Persons
	 *  1.Person: has 2 trips with payments
	 *  2.Person: has 1 trip with payment
	 */
	@Test
	public void testPersonMoney() {
		String eventsFile = utils.getInputDirectory() + "testPersonMoneyEvents.xml";
		
		Scenario scenario = createScenario(2);
		BasicPersonTripAnalysisHandler basicHandler = analyseScenario(eventsFile, scenario);
	
		Assert.assertEquals("Unexpected total payment: ", -80.0, basicHandler.getTotalPayments(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected amount: ", -20.0, 
				basicHandler.getPersonId2tripNumber2payment().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected amount: ", -30.0, 
				basicHandler.getPersonId2tripNumber2payment().get(Id.create(0, Person.class)).get(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected amount: ", -30.0, 
				basicHandler.getPersonId2tripNumber2payment().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		
		if (printResults) printResults(basicHandler, scenario);
	}
	
	/**
	 * BasicPersonTripAnalysisHandler - line 329: why event.getTime() == 30*3600 and not >= 30*3600?
	 * Scenario: 2 Persons
	 *  1.Person: has 2 trips, stucks within the 2. trip
	 *  2.Person: has 1 trip, stucks after the trip
	 */
	@Test
	public void testPersonStuck() {
		String eventsFile = utils.getInputDirectory() + "testPersonStuckEvents.xml";
		
		Scenario scenario = createScenario(2);
		BasicPersonTripAnalysisHandler basicHandler = analyseScenario(eventsFile, scenario);
	
		Assert.assertTrue(basicHandler.getPersonId2tripNumber2stuckAbort().get(Id.create(0, Person.class)).get(2));
		Assert.assertTrue(basicHandler.getPersonId2tripNumber2stuckAbort().get(Id.create(1, Person.class)).get(1));
		
		if (printResults) printResults(basicHandler, scenario);
	}
	
	/**
	 * Scenario where a Person rides a bike
	 * current status: Fail; because its assumed that a legmode that is not "car" has to be "pt"
	 * so bike is not covert. but maybe there is no need to change that, because bikers and walkers
	 * are commonly teleported
	 * in that case add the @Ignore statement over @Test or just delete the Test
	 */
	@Test
	public void testTronsportModeBike() {
		String eventsFile = utils.getInputDirectory() + "testTronsportModeBikeEvents.xml";
		
		Scenario scenario = createScenario(1);
		BasicPersonTripAnalysisHandler basicHandler = analyseScenario(eventsFile, scenario);
	
		if (printResults) printResults(basicHandler, scenario);
	}
	
	/**
	 * Scenario: 3 Persons
	 * 	1.Person: 2 different trips
	 *  2.Person: 3 different trips
	 *  3.Person: no trips; activity "home" ends and activity "work" starts on the same link
	 */
	@Test
	public void testVariousTripCounts() {
		
		String eventsFile = utils.getInputDirectory() + "testVariousTripCountsEvents.xml";
		
		Scenario scenario = createScenario(3);
		BasicPersonTripAnalysisHandler basicHandler = analyseScenario(eventsFile, scenario);
	
		Assert.assertTrue(basicHandler.getPersonId2tripNumber2tripDistance().get(Id.create(0, Person.class)).get(1).equals(4000.0));
		Assert.assertTrue(basicHandler.getPersonId2tripNumber2tripDistance().get(Id.create(0, Person.class)).get(2).equals(4000.0));
		Assert.assertTrue(basicHandler.getPersonId2tripNumber2tripDistance().get(Id.create(1, Person.class)).get(1).equals(4000.0));
		Assert.assertTrue(basicHandler.getPersonId2tripNumber2tripDistance().get(Id.create(2, Person.class)).get(1).equals(0.0));
		
		if (printResults) printResults(basicHandler, scenario);
	}
	
	/**
	 * Scenario: 1 Person
	 * 	1.Person: 2 different trips with different vehicles
	 * current status: Fail; because vehicle id is assumed to be always the same as person id
	 */
	@Test
	public void testVariousVehiclesPerPerson() {
		
		String eventsFile = utils.getInputDirectory() + "testVariousVehiclesPerPersonEvents.xml";
		
		Scenario scenario = createScenario(1);
		BasicPersonTripAnalysisHandler basicHandler = analyseScenario(eventsFile, scenario);
	
		if (printResults) printResults(basicHandler, scenario);
	}
	
	public Scenario createScenario(int personNumber) {

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		// create Population
		for (int i = 0; i < personNumber; i++) {
			scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(Id.create(i, Person.class)));
		}
		
		ForkNetworkCreator fnc = new ForkNetworkCreator(scenario, false, false);
		fnc.createNetwork();
			
		return scenario;
	}
	
	public BasicPersonTripAnalysisHandler analyseScenario(String eventsFile, Scenario scenario) {

		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler(scenario);	
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(basicHandler);
		
		log.info("Reading the events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		log.info("Reading the events file... Done.");
		
		return basicHandler;
	}
	
	public void printResults(BasicPersonTripAnalysisHandler basicHandler, Scenario scenario) {
		System.out.println("totalPayments: " + basicHandler.getTotalPayments());
		for (Person person : scenario.getPopulation().getPersons().values()) {
			System.out.println("Person Id : " + person.getId());
			System.out.println("currentTripNumber: " + basicHandler.getPersonId2currentTripNumber().get(person.getId()));
			System.out.println("distanceEnterValue: " + basicHandler.getPersonId2distanceEnterValue().get(person.getId()));
			System.out.println("tripNumber2amount: " + basicHandler.getPersonId2tripNumber2payment().get(person.getId()));
			System.out.println("tripNumber2arrivalTime: " + basicHandler.getPersonId2tripNumber2arrivalTime().get(person.getId()));
			System.out.println("tripNumber2departureTime: " + basicHandler.getPersonId2tripNumber2departureTime().get(person.getId()));
			System.out.println("tripNumber2legMode: " + basicHandler.getPersonId2tripNumber2legMode().get(person.getId()));
			System.out.println("tripNumber2stuckAbort: " + basicHandler.getPersonId2tripNumber2stuckAbort().get(person.getId()));
			System.out.println("tripNumber2travelTime: " + basicHandler.getPersonId2tripNumber2travelTime().get(person.getId()));
			System.out.println("tripNumber2tripDistance: " + basicHandler.getPersonId2tripNumber2tripDistance().get(person.getId()));
			
		}
	}
}
