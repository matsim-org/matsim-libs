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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.CongestionAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.NoiseAnalysisHandler;
import playground.ikaddoura.integrationCN.CNEventsReader;
import playground.tschlenther.createNetwork.ForkNetworkCreator;

/**
 * @author gthunig, ikaddoura
 * 
 * This class tests if the CNEventsReader is capable of reading both congestion- and noise-events of the same file. 
 * A small events file is analyzed, and a scenario is created using the ForkNetworkCreator.
 * 
 */
public class CongestionNoiseTest {

	private static final Logger log = Logger.getLogger(CongestionNoiseTest.class);

	private static final boolean printResults = false;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	// Congestion only tests

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
		CongestionAndNoiseHandler congestionAndNoiseHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionAndNoiseHandler, scenario);
		
		Assert.assertFalse("There should not be a handled CongestionEvent!", congestionAndNoiseHandler.getCongestionHandler().isCaughtCongestionEvent());
		Assert.assertEquals("The totalDelay should be 0!", 0.0, congestionAndNoiseHandler.getCongestionHandler().getTotalDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertNull("There should not be a causedDelay for person 0, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)));
		Assert.assertNull("There should not be a affectedDelay for person 0, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
		Assert.assertNull("There should not be a causedDelay for person 0, trip 2!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)));
		Assert.assertNull("There should not be a affectedDelay for person 0, trip 2!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
		
		Assert.assertNull("There should not be a causedDelay for person 1, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(1, Person.class)));
		Assert.assertNull("There should not be a affectedDelay for person 1, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(1, Person.class)));
		
		Assert.assertNull("There should not be a causedDelay for person 2, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(1, Person.class)));
		Assert.assertNull("There should not be a affectedDelay for person 2, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(1, Person.class)));
		
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
		CongestionAndNoiseHandler congestionAndNoiseHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionAndNoiseHandler, scenario);
		
		Assert.assertTrue("There should be a handled CongestionEvent!", congestionAndNoiseHandler.getCongestionHandler().isCaughtCongestionEvent());
		Assert.assertEquals("The totalDelay should be 100!", 100.0, congestionAndNoiseHandler.getCongestionHandler().getTotalDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("There should be a causedDelay of 100 for person 0, trip 1!", 100.0, 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertNull("There should not be a affectedDelay for person 0, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
		
		Assert.assertNull("There should not be a causedDelay for person 1, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(1, Person.class)));
		Assert.assertEquals("There should be a affectedDelay of 100 for person 1, trip 1!", 100.0, 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		
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
		CongestionAndNoiseHandler congestionAndNoiseHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionAndNoiseHandler, scenario);
		
		Assert.assertTrue("There should be a handled CongestionEvent!", congestionAndNoiseHandler.getCongestionHandler().isCaughtCongestionEvent());
		Assert.assertEquals("The totalDelay should be 300!", 300.0, congestionAndNoiseHandler.getCongestionHandler().getTotalDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("There should be a causedDelay of 300 for person 0, trip 1!", 300.0, 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertNull("There should not be a affectedDelay for person 0, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
		
		Assert.assertNull("There should not be a causedDelay for person 1, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(1, Person.class)));
		Assert.assertEquals("There should be a affectedDelay of 100 for person 1, trip 1!", 100.0, 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		
		Assert.assertNull("There should not be a causedDelay for person 2, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(2, Person.class)));
		Assert.assertEquals("There should be a affectedDelay of 200 for person 2, trip 1!", 200.0, 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(2, Person.class)).get(1), MatsimTestUtils.EPSILON);
		
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
		CongestionAndNoiseHandler congestionAndNoiseHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionAndNoiseHandler, scenario);
		
		Assert.assertTrue("There should be a handled CongestionEvent!", congestionAndNoiseHandler.getCongestionHandler().isCaughtCongestionEvent());
		Assert.assertEquals("The totalDelay should be 100!", 100.0, congestionAndNoiseHandler.getCongestionHandler().getTotalDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("There should be a causedDelay of 100 for person 0, trip 1!", 100.0, 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertNull("There should not be a affectedDelay for person 0, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
		Assert.assertNull("There should not be a causedDelay for person 0, trip 2!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)).get(2));
		Assert.assertNull("There should not be a affectedDelay for person 0, trip 2!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
		
		Assert.assertNull("There should not be a causedDelay for person 1, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(1, Person.class)));
		Assert.assertEquals("There should be a affectedDelay of 100 for person 1, trip 1!", 100.0, 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		
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
		CongestionAndNoiseHandler congestionAndNoiseHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionAndNoiseHandler, scenario);
		
		Assert.assertTrue("There should be a handled CongestionEvent!", congestionAndNoiseHandler.getCongestionHandler().isCaughtCongestionEvent());
		Assert.assertEquals("The totalDelay should be 100!", 100.0, congestionAndNoiseHandler.getCongestionHandler().getTotalDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("There should be a causedDelay of 100 for person 0, trip 1!", 100.0, 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertNull("There should not be a affectedDelay for person 0, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
		
		Assert.assertNull("There should not be a causedDelay for person 1, trip 1!", 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(1, Person.class)));
		Assert.assertEquals("There should be a affectedDelay of 100 for person 1, trip 1!", 100.0, 
				congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		
	}
	
	// Noise only tests
	
	/**
	 * Scenario: 3 Persons
	 *  1.Person: has 2 trips without delay in any form
	 *  2.Person: has 1 trip without delay in any form
	 *  3.Person: has no trip without delay in any form
	 */
	@Test
	public void testNoNoise() {
		
		String eventsFile = utils.getInputDirectory() + "testNoNoiseEvents.xml";
		
		Scenario scenario = createScenario(3);
		CongestionAndNoiseHandler congestionAndNoiseHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionAndNoiseHandler, scenario);
		
		Assert.assertFalse("There should not be a handled NoiseEvent!", congestionAndNoiseHandler.getNoiseHandler().isCaughtNoiseEvent());
		Assert.assertEquals("The total causedNoiseCosts should be 0!", 0.0, congestionAndNoiseHandler.getNoiseHandler().getCausedNoiseCost(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("The total affectedNoiseCosts should be 0!", 0.0, congestionAndNoiseHandler.getNoiseHandler().getAffectedNoiseCost(), MatsimTestUtils.EPSILON);
		
		Assert.assertNull("There should not be causedNoiseCosts at all for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(0, Person.class)));
		Assert.assertNull("There should not be a causedNoiseCost for any trip for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(0, Person.class)));
		Assert.assertNull("There should not be affectedNoiseCosts at all for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(0, Person.class)));
		
		Assert.assertNull("There should not be causedNoiseCosts at all for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(1, Person.class)));
		Assert.assertNull("There should not be affectedNoiseCosts at all for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(1, Person.class)));
		
		Assert.assertNull("There should not be causedNoiseCosts at all for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(2, Person.class)));
		Assert.assertNull("There should not be affectedNoiseCosts at all for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(2, Person.class)));
		
	}
	
	/**
	 * Scenario: 2 Persons
	 *  1.Person: is causing noise of 100 to person2
	 *  2.Person: has an affected amount of 100 noise by person1
	 */
	@Test
	public void testSingleNoise() {
		
		String eventsFile = utils.getInputDirectory() + "testSingleNoiseEvents.xml";
		
		Scenario scenario = createScenario(2);
		CongestionAndNoiseHandler congestionAndNoiseHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionAndNoiseHandler, scenario);
		
		Assert.assertTrue("There should be a handled NoiseEvent!", congestionAndNoiseHandler.getNoiseHandler().isCaughtNoiseEvent());
		Assert.assertEquals("The total causedNoiseCosts should be 100!", 100.0, congestionAndNoiseHandler.getNoiseHandler().getCausedNoiseCost(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("The total affectedNoiseCosts should be 100!", 100.0, congestionAndNoiseHandler.getNoiseHandler().getAffectedNoiseCost(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("The total causedNoiseCosts of person 0 should be 100!", 100.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(0, Person.class)), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("There should be a causedNoiseCost for at least one trip for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(0, Person.class)));
		Assert.assertEquals("The causedNoiseCosts of person 0 for trip 0 should be 100!", 100.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertNull("There should not be affectedNoiseCosts at all for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(0, Person.class)));
		
		Assert.assertNull("There should not be causedNoiseCosts at all for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(1, Person.class)));
		Assert.assertEquals("The affectedNoiseCosts of person 1 should be 100!", 100.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(1, Person.class)), MatsimTestUtils.EPSILON);
		
	}
	
	/**
	 * Scenario: 3 Persons
	 *  1.Person: is causing noise of 100 to person3
	 *  2.Person: is causing noise of 100 to person3
	 *  3.Person: has an affected amount of 200 noise by person1 and person2
	 */
	@Test
	public void testMultipleCausedNoise() {
		
		String eventsFile = utils.getInputDirectory() + "testMultipleCausedNoiseEvents.xml";
		
		Scenario scenario = createScenario(3);
		CongestionAndNoiseHandler congestionAndNoiseHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionAndNoiseHandler, scenario);
		
		Assert.assertTrue("There should be a handled NoiseEvent!", congestionAndNoiseHandler.getNoiseHandler().isCaughtNoiseEvent());
		Assert.assertEquals("The total causedNoiseCosts should be 200!", 200.0, congestionAndNoiseHandler.getNoiseHandler().getCausedNoiseCost(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("The total affectedNoiseCosts should be 200!", 200.0, congestionAndNoiseHandler.getNoiseHandler().getAffectedNoiseCost(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("The total causedNoiseCosts of person 0 should be 100!", 100.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(0, Person.class)), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("There should be a causedNoiseCost for at least one trip for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(0, Person.class)));
		Assert.assertEquals("The causedNoiseCosts of person 0 for trip 0 should be 100!", 100.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertNull("There should not be affectedNoiseCosts at all for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(0, Person.class)));
		
		Assert.assertEquals("The total causedNoiseCosts of person 1 should be 100!", 100.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(1, Person.class)), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("There should be a causedNoiseCost for at least one trip for person 1!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(1, Person.class)));
		Assert.assertEquals("The causedNoiseCosts of person 1 for trip 0 should be 100!", 100.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertNull("There should not be affectedNoiseCosts at all for person 1!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(1, Person.class)));
		
		Assert.assertNull("There should not be causedNoiseCosts at all for person 2!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(2, Person.class)));
		Assert.assertEquals("The affectedNoiseCosts of person 2 should be 200!", 200.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(2, Person.class)), MatsimTestUtils.EPSILON);
		
	}
	
	/**
	 * Scenario: 3 Persons
	 *  1.Person: is causing noise of 100 to person2 and 100 to person3
	 *  2.Person: has an affected amount of 100 noise by person1
	 *  3.Person: has an affected amount of 100 noise by person1
	 */
	@Test
	public void testMultipleAffectedNoise() {
		
		String eventsFile = utils.getInputDirectory() + "testMultipleAffectedNoiseEvents.xml";
		
		Scenario scenario = createScenario(3);
		CongestionAndNoiseHandler congestionAndNoiseHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionAndNoiseHandler, scenario);
		
		Assert.assertTrue("There should be a handled NoiseEvent!", congestionAndNoiseHandler.getNoiseHandler().isCaughtNoiseEvent());
		Assert.assertEquals("The total causedNoiseCosts should be 100!", 100.0, congestionAndNoiseHandler.getNoiseHandler().getCausedNoiseCost(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("The total affectedNoiseCosts should be 200!", 200.0, congestionAndNoiseHandler.getNoiseHandler().getAffectedNoiseCost(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("The total causedNoiseCosts of person 0 should be 100!", 100.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(0, Person.class)), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("There should be a causedNoiseCost for at least one trip for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(0, Person.class)));
		Assert.assertEquals("The causedNoiseCosts of person 0 for trip 0 should be 100!", 100.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertNull("There should not be affectedNoiseCosts at all for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(0, Person.class)));
		
		Assert.assertNull("There should not be causedNoiseCosts at all for person 1!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(1, Person.class)));
		Assert.assertEquals("The affectedNoiseCosts of person 1 should be 100!", 100.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(1, Person.class)), MatsimTestUtils.EPSILON);
		
		Assert.assertNull("There should not be causedNoiseCosts at all for person 2!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(2, Person.class)));
		Assert.assertEquals("The affectedNoiseCosts of person 2 should be 100!", 100.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(2, Person.class)), MatsimTestUtils.EPSILON);
		
	}
	
	/**
	 * Scenario: 3 Persons
	 *  1.Person: is causing noise of 100 to person2 and 100 to person3
	 *  2.Person: has an affected amount of 100 noise by person1
	 *  3.Person: has an affected amount of 100 noise by person1
	 */
	@Test
	public void testOwnCausedNoiseAffected() {
		
		String eventsFile = utils.getInputDirectory() + "testOwnCausedNoiseAffectedEvents.xml";
		
		Scenario scenario = createScenario(1);
		CongestionAndNoiseHandler congestionAndNoiseHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(congestionAndNoiseHandler, scenario);
		
		Assert.assertTrue("There should be a handled NoiseEvent!", congestionAndNoiseHandler.getNoiseHandler().isCaughtNoiseEvent());
		Assert.assertEquals("The total causedNoiseCosts should be 100!", 100.0, congestionAndNoiseHandler.getNoiseHandler().getCausedNoiseCost(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("The total affectedNoiseCosts should be 100!", 100.0, congestionAndNoiseHandler.getNoiseHandler().getAffectedNoiseCost(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("The total causedNoiseCosts of person 0 should be 100!", 100.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(0, Person.class)), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("There should be a causedNoiseCost for at least one trip for person 0!", 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(0, Person.class)));
		Assert.assertEquals("The causedNoiseCosts of person 0 for trip 0 should be 100!", 100.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("The affectedNoiseCosts of person 0 should be 100!", 100.0, 
				congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(0, Person.class)), MatsimTestUtils.EPSILON);
		
	}
	
	// Combined congestion noise test
	
	
		/**
		 * Scenario: 3 Persons
		 *  1.Person: is causing noise of 100 to person3 and congestion of 100 to person2
		 *  2.Person: is causing noise of 100 to person3 and gets affected congestion of 100 by person1
		 *  3.Person: has an affected amount of 200 noise by person1 and person2
		 */
		@Test
		public void testCombinedCongestionNoise() {
			
			String eventsFile = utils.getInputDirectory() + "testCombinedCongestionNoiseEvents.xml";
			
			Scenario scenario = createScenario(3);
			CongestionAndNoiseHandler congestionAndNoiseHandler = analyseScenario(eventsFile, scenario);
			
			if (printResults) printResults(congestionAndNoiseHandler, scenario);
			
			// check congestion
			
			Assert.assertTrue("There should be a handled CongestionEvent!", congestionAndNoiseHandler.getCongestionHandler().isCaughtCongestionEvent());
			Assert.assertEquals("The totalDelay should be 100!", 100.0, congestionAndNoiseHandler.getCongestionHandler().getTotalDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("There should be a causedDelay of 100 for person 0, trip 1!", 100.0, 
					congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
			Assert.assertNull("There should not be a affectedDelay for person 0, trip 1!", 
					congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(0, Person.class)));
			
			Assert.assertNull("There should not be a causedDelay for person 1, trip 1!", 
					congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(Id.create(1, Person.class)));
			Assert.assertEquals("There should be a affectedDelay of 100 for person 1, trip 1!", 100.0, 
					congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
			
			// check noise
			
			Assert.assertTrue("There should be a handled NoiseEvent!", congestionAndNoiseHandler.getNoiseHandler().isCaughtNoiseEvent());
			Assert.assertEquals("The total causedNoiseCosts should be 200!", 200.0, congestionAndNoiseHandler.getNoiseHandler().getCausedNoiseCost(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("The total affectedNoiseCosts should be 200!", 200.0, congestionAndNoiseHandler.getNoiseHandler().getAffectedNoiseCost(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("The total causedNoiseCosts of person 0 should be 100!", 100.0, 
					congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(0, Person.class)), MatsimTestUtils.EPSILON);
			Assert.assertNotNull("There should be a causedNoiseCost for at least one trip for person 0!", 
					congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(0, Person.class)));
			Assert.assertEquals("The causedNoiseCosts of person 0 for trip 0 should be 100!", 100.0, 
					congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
			Assert.assertNull("There should not be affectedNoiseCosts at all for person 0!", 
					congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(0, Person.class)));
			
			Assert.assertEquals("The total causedNoiseCosts of person 1 should be 100!", 100.0, 
					congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(1, Person.class)), MatsimTestUtils.EPSILON);
			Assert.assertNotNull("There should be a causedNoiseCost for at least one trip for person 1!", 
					congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(1, Person.class)));
			Assert.assertEquals("The causedNoiseCosts of person 1 for trip 0 should be 100!", 100.0, 
					congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
			Assert.assertNull("There should not be affectedNoiseCosts at all for person 1!", 
					congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(1, Person.class)));
			
			Assert.assertNull("There should not be causedNoiseCosts at all for person 2!", 
					congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(Id.create(2, Person.class)));
			Assert.assertEquals("The affectedNoiseCosts of person 2 should be 200!", 200.0, 
					congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(Id.create(2, Person.class)), MatsimTestUtils.EPSILON);
			
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
	
	public CongestionAndNoiseHandler analyseScenario(String eventsFile, Scenario scenario) {

		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler(scenario);
		CongestionAnalysisHandler congestionHandler = new CongestionAnalysisHandler(basicHandler);
		NoiseAnalysisHandler noiseHandler = new NoiseAnalysisHandler(basicHandler);
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(basicHandler);
		events.addHandler(congestionHandler);
		events.addHandler(noiseHandler);
		
		log.info("Reading the events file...");
		
//		MatsimEventsReader reader = new MatsimEventsReader(events);
//		reader.readFile(eventsFile);
//		CongestionEventsReader congestionEventsReader = new CongestionEventsReader(events);		
//		congestionEventsReader.parse(eventsFile);
		
		CNEventsReader reader = new CNEventsReader(events);
		reader.parse(eventsFile);
		
		log.info("Reading the events file... Done.");
		CongestionAndNoiseHandler congestionAndNoiseHandler = new CongestionAndNoiseHandler(congestionHandler, noiseHandler);
		return congestionAndNoiseHandler;
	}
	
	public void printResults(CongestionAndNoiseHandler congestionAndNoiseHandler, Scenario scenario) {
		System.out.println("isCaughtCongestionEvent: " + congestionAndNoiseHandler.getCongestionHandler().isCaughtCongestionEvent());
		System.out.println("totalDelay: " + congestionAndNoiseHandler.getCongestionHandler().getTotalDelay());
		for (Person person : scenario.getPopulation().getPersons().values()) {
			System.out.println("Person Id : " + person.getId());
			System.out.println("causedDelay: " + congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2causedDelay().get(person.getId()));
			System.out.println("affectedDelay: " + congestionAndNoiseHandler.getCongestionHandler().getPersonId2tripNumber2affectedDelay().get(person.getId()));
		}
		System.out.println("isCaughtNoiseEvent: " + congestionAndNoiseHandler.getNoiseHandler().isCaughtNoiseEvent());
		System.out.println("total causedNoiseCost: " + congestionAndNoiseHandler.getNoiseHandler().getCausedNoiseCost());
		System.out.println("total affectedNoiseCost: " + congestionAndNoiseHandler.getNoiseHandler().getAffectedNoiseCost());
		for (Person person : scenario.getPopulation().getPersons().values()) {
			System.out.println("Person Id : " + person.getId());
			System.out.println("Person " + person.getId() + "s total causedNoiseCost: " + 
					congestionAndNoiseHandler.getNoiseHandler().getPersonId2causedNoiseCost().get(person.getId()));
			System.out.println("Person " + person.getId() + "s PersonId2TripNumber2causedNoiseCost: " + 
					congestionAndNoiseHandler.getNoiseHandler().getPersonId2tripNumber2causedNoiseCost().get(person.getId()));
			System.out.println("Person " + person.getId() + "s total affectedNoiseCost: " + 
					congestionAndNoiseHandler.getNoiseHandler().getPersonId2affectedNoiseCost().get(person.getId()));
		}
	}
	
	private class CongestionAndNoiseHandler {
		private CongestionAnalysisHandler congestionHandler;
		private NoiseAnalysisHandler noiseHandler;
		
		public CongestionAndNoiseHandler(CongestionAnalysisHandler congestionHandler, NoiseAnalysisHandler noiseHandler) {
			this.congestionHandler = congestionHandler;
			this.noiseHandler = noiseHandler;
		}
		
		public CongestionAnalysisHandler getCongestionHandler() {
			return congestionHandler;
		}

		public NoiseAnalysisHandler getNoiseHandler() {
			return noiseHandler;
		}
	}
	
}
