package playground.ikaddoura.analysis.detailedPersonTripAnalysis;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.CongestionAnalysisHandler;
import playground.tschlenther.createNetwork.ForkNetworkCreator;
import playground.vsp.congestion.events.CongestionEventsReader;

/**
 * @author gthunig
 * 
 * This class tests the Analysis which is provided by the
 * CongestionAnalysisHandler. 
 * Therefor several scenarios are created which are declared above their testmethod.
 * Every scenario has its own method.
 * Every scenario has the same network which is declared in its class; 
 * current status: ForkNetworkCreator
 */
public class CongestionAnalysisTest {

	private static final Logger log = Logger.getLogger(CongestionAnalysisTest.class);

	private static final boolean printResults = false;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	/**
	 * Scenario: 3 Persons
	 *  1.Person: has 2 trips without delay in any form
	 *  2.Person: has 1 trip without delay in any form
	 *  3.Person: has no trip without delay in any form
	 */
	@Test
	public void testNoCongestion() {
		
		String eventsFile = utils.getInputDirectory() + "testNoCongestionEvents.xml";
		
		Scenario scenario = createScenario(3);
		CongestionAnalysisHandler congestionHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionHandler, scenario);
		
		Assert.assertFalse("There should not be a handled CongestionEvent!", congestionHandler.isCaughtCongestionEvent());
		Assert.assertEquals("The totalDelay should be 0!", 0.0, congestionHandler.getTotalDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertNull("There should not be a causedDelay for person 0, trip 1!", 
				congestionHandler.getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)));
		Assert.assertNull("There should not be a affectedDelay for person 0, trip 1!", 
				congestionHandler.getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
		Assert.assertNull("There should not be a causedDelay for person 0, trip 2!", 
				congestionHandler.getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)));
		Assert.assertNull("There should not be a affectedDelay for person 0, trip 2!", 
				congestionHandler.getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
		
		Assert.assertNull("There should not be a causedDelay for person 1, trip 1!", 
				congestionHandler.getPersonId2tripNumber2causedDelay().get(Id.create(1, Person.class)));
		Assert.assertNull("There should not be a affectedDelay for person 1, trip 1!", 
				congestionHandler.getPersonId2tripNumber2affectedDelay().get(Id.create(1, Person.class)));
		
		Assert.assertNull("There should not be a causedDelay for person 2, trip 1!", 
				congestionHandler.getPersonId2tripNumber2causedDelay().get(Id.create(1, Person.class)));
		Assert.assertNull("There should not be a affectedDelay for person 2, trip 1!", 
				congestionHandler.getPersonId2tripNumber2affectedDelay().get(Id.create(1, Person.class)));
		
	}
	
	/**
	 * Scenario: 2 Persons
	 *  1.Person: has 1 trip with causingDelay of 100 to person2
	 *  2.Person: has 1 trip with affectedDelay of 100 from person1
	 */
	@Test
	public void testSingleCongestion() {
		
		String eventsFile = utils.getInputDirectory() + "testSingleCongestionEvents.xml";
		
		Scenario scenario = createScenario(2);
		CongestionAnalysisHandler congestionHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionHandler, scenario);
		
		Assert.assertTrue("There should be a handled CongestionEvent!", congestionHandler.isCaughtCongestionEvent());
		Assert.assertEquals("The totalDelay should be 100!", 100.0, congestionHandler.getTotalDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("There should be a causedDelay of 100 for person 0, trip 1!", 100.0, 
				congestionHandler.getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertNull("There should not be a affectedDelay for person 0, trip 1!", 
				congestionHandler.getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
		
		Assert.assertNull("There should not be a causedDelay for person 1, trip 1!", 
				congestionHandler.getPersonId2tripNumber2causedDelay().get(Id.create(1, Person.class)));
		Assert.assertEquals("There should be a affectedDelay of 100 for person 1, trip 1!", 100.0, 
				congestionHandler.getPersonId2tripNumber2affectedDelay().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		
	}
	
	/**
	 * Scenario: 3 Persons
	 *  1.Person: has 1 trip with causingDelay of 100 to person2 and 200 to person3
	 *  2.Person: has 1 trip with affectedDelay of 100 from person1
	 *  3.Person: has 1 trip with affectedDelay of 100 from person1
	 */
	@Test
	public void testStackingCongestion() {
		
		String eventsFile = utils.getInputDirectory() + "testStackingCongestionEvents.xml";
		
		Scenario scenario = createScenario(3);
		CongestionAnalysisHandler congestionHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionHandler, scenario);
		
		Assert.assertTrue("There should be a handled CongestionEvent!", congestionHandler.isCaughtCongestionEvent());
		Assert.assertEquals("The totalDelay should be 300!", 300.0, congestionHandler.getTotalDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("There should be a causedDelay of 300 for person 0, trip 1!", 300.0, 
				congestionHandler.getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertNull("There should not be a affectedDelay for person 0, trip 1!", 
				congestionHandler.getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
		
		Assert.assertNull("There should not be a causedDelay for person 1, trip 1!", 
				congestionHandler.getPersonId2tripNumber2causedDelay().get(Id.create(1, Person.class)));
		Assert.assertEquals("There should be a affectedDelay of 100 for person 1, trip 1!", 100.0, 
				congestionHandler.getPersonId2tripNumber2affectedDelay().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		
		Assert.assertNull("There should not be a causedDelay for person 2, trip 1!", 
				congestionHandler.getPersonId2tripNumber2causedDelay().get(Id.create(2, Person.class)));
		Assert.assertEquals("There should be a affectedDelay of 200 for person 2, trip 1!", 200.0, 
				congestionHandler.getPersonId2tripNumber2affectedDelay().get(Id.create(2, Person.class)).get(1), MatsimTestUtils.EPSILON);
		
	}
	
	/**
	 * Scenario: 2 Persons
	 *  1.Person: has 2 trips with causingDelay of 100 to person2 from trip 1 but currentTrip is 2
	 *  2.Person: has 1 trip with affectedDelay of 100 from person1
	 */
	@Test
	public void testCausingAgentInNextTripCongestion() {
		
		String eventsFile = utils.getInputDirectory() + "testCausingAgentInNextTripCongestionEvents.xml";
		
		Scenario scenario = createScenario(2);
		CongestionAnalysisHandler congestionHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionHandler, scenario);
		
		Assert.assertTrue("There should be a handled CongestionEvent!", congestionHandler.isCaughtCongestionEvent());
		Assert.assertEquals("The totalDelay should be 100!", 100.0, congestionHandler.getTotalDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("There should be a causedDelay of 100 for person 0, trip 1!", 100.0, 
				congestionHandler.getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertNull("There should not be a affectedDelay for person 0, trip 1!", 
				congestionHandler.getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
		Assert.assertNull("There should not be a causedDelay for person 0, trip 2!", 
				congestionHandler.getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)).get(2));
		Assert.assertNull("There should not be a affectedDelay for person 0, trip 2!", 
				congestionHandler.getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
		
		Assert.assertNull("There should not be a causedDelay for person 1, trip 1!", 
				congestionHandler.getPersonId2tripNumber2causedDelay().get(Id.create(1, Person.class)));
		Assert.assertEquals("There should be a affectedDelay of 100 for person 1, trip 1!", 100.0, 
				congestionHandler.getPersonId2tripNumber2affectedDelay().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		
	}
	
	/**
	 * Scenario: 2 Persons
	 *  1.Person: has 1 trip with causingDelay of 100 to person2 but already arrived
	 *  2.Person: has 1 trip with affectedDelay of 100 from person1
	 */
	@Test
	public void testCausingAgentArrivedCongestion() {
		
		String eventsFile = utils.getInputDirectory() + "testCausingAgentArrivedCongestionEvents.xml";
		
		Scenario scenario = createScenario(2);
		CongestionAnalysisHandler congestionHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionHandler, scenario);
		
		Assert.assertTrue("There should be a handled CongestionEvent!", congestionHandler.isCaughtCongestionEvent());
		Assert.assertEquals("The totalDelay should be 100!", 100.0, congestionHandler.getTotalDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("There should be a causedDelay of 100 for person 0, trip 1!", 100.0, 
				congestionHandler.getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertNull("There should not be a affectedDelay for person 0, trip 1!", 
				congestionHandler.getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
		
		Assert.assertNull("There should not be a causedDelay for person 1, trip 1!", 
				congestionHandler.getPersonId2tripNumber2causedDelay().get(Id.create(1, Person.class)));
		Assert.assertEquals("There should be a affectedDelay of 100 for person 1, trip 1!", 100.0, 
				congestionHandler.getPersonId2tripNumber2affectedDelay().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		
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
	
	public CongestionAnalysisHandler analyseScenario(String eventsFile, Scenario scenario) {

		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler(scenario);
		CongestionAnalysisHandler congestionHandler = new CongestionAnalysisHandler(basicHandler);	
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(basicHandler);
		events.addHandler(congestionHandler);
		
		log.info("Reading the events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		CongestionEventsReader congestionEventsReader = new CongestionEventsReader(events);		
		congestionEventsReader.parse(eventsFile);
		log.info("Reading the events file... Done.");
		
		return congestionHandler;
	}
	
	public void printResults(CongestionAnalysisHandler congestionHandler, Scenario scenario) {
		System.out.println("isCaughtCongestionEvent: " + congestionHandler.isCaughtCongestionEvent());
		System.out.println("totalDelay: " + congestionHandler.getTotalDelay());
		for (Person person : scenario.getPopulation().getPersons().values()) {
			System.out.println("Person Id : " + person.getId());
			System.out.println("causedDelay: " + congestionHandler.getPersonId2tripNumber2causedDelay().get(person.getId()));
			System.out.println("affectedDelay: " + congestionHandler.getPersonId2tripNumber2affectedDelay().get(person.getId()));
		}
	}
	
}
