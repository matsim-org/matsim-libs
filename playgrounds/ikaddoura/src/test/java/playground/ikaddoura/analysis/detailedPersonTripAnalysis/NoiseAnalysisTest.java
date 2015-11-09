package playground.ikaddoura.analysis.detailedPersonTripAnalysis;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
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
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.NoiseAnalysisHandler;
import playground.ikaddoura.noise2.events.NoiseEventsReader;
import playground.tschlenther.createNetwork.ForkNetworkCreator;
import playground.vsp.congestion.events.CongestionEventsReader;

/**
 * @author gthunig
 * 
 * This class tests the Analysis which is provided by the
 * NoiseAnalysisHandler. 
 * Therefor several scenarios are created which are declared above their testmethod.
 * Every scenario has its own method.
 * Every scenario has the same network which is declared in its class; 
 * current status: ForkNetworkCreator
 */
public class NoiseAnalysisTest {

	private static final Logger log = Logger.getLogger(CongestionAnalysisTest.class);

	private static final boolean printResults = true;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
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
		NoiseAnalysisHandler noiseHandler = analyseScenario(eventsFile, scenario);
		
		if (printResults) printResults(noiseHandler, scenario);
		
		Assert.assertFalse("There should not be a handled NoiseEvent!", noiseHandler.isCaughtNoiseEvent());
		Assert.assertEquals("The total causedNoiseCosts should be 0!", 0.0, noiseHandler.getCausedNoiseCost(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("The total affectedNoiseCosts should be 0!", 0.0, noiseHandler.getAffectedNoiseCost(), MatsimTestUtils.EPSILON);
		
		Assert.assertNull("There should not be causedNoiseCosts at all for person 0!", 
				noiseHandler.getPersonId2causedNoiseCost().get(Id.create(0, Person.class)));
		//TODO why not initialize
		Assert.assertNull("There should not be a causedNoiseCost for any trip for person 0!", 
				noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(Id.create(0, Person.class)));
		Assert.assertNull("There should not be affectedNoiseCosts at all for person 0!", 
				noiseHandler.getPersonId2affectedNoiseCost().get(Id.create(0, Person.class)));
		
		Assert.assertNull("There should not be causedNoiseCosts at all for person 0!", 
				noiseHandler.getPersonId2causedNoiseCost().get(Id.create(1, Person.class)));
		Assert.assertNull("There should not be affectedNoiseCosts at all for person 0!", 
				noiseHandler.getPersonId2affectedNoiseCost().get(Id.create(1, Person.class)));
		
		Assert.assertNull("There should not be causedNoiseCosts at all for person 0!", 
				noiseHandler.getPersonId2causedNoiseCost().get(Id.create(2, Person.class)));
		Assert.assertNull("There should not be affectedNoiseCosts at all for person 0!", 
				noiseHandler.getPersonId2affectedNoiseCost().get(Id.create(2, Person.class)));
		
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
	
	public NoiseAnalysisHandler analyseScenario(String eventsFile, Scenario scenario) {

		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler(scenario);
		NoiseAnalysisHandler noiseHandler = new NoiseAnalysisHandler(basicHandler);	
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(basicHandler);
		events.addHandler(noiseHandler);
		
		log.info("Reading the events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		NoiseEventsReader noiseEventReader = new NoiseEventsReader(events);		
		noiseEventReader.parse(eventsFile);
		log.info("Reading the events file... Done.");
		
		return noiseHandler;
	}
	
	public void printResults(NoiseAnalysisHandler noiseHandler, Scenario scenario) {
		System.out.println("isCaughtNoiseEvent: " + noiseHandler.isCaughtNoiseEvent());
		System.out.println("total causedNoiseCost: " + noiseHandler.getCausedNoiseCost());
		System.out.println("total affectedNoiseCost: " + noiseHandler.getAffectedNoiseCost());
		for (Person person : scenario.getPopulation().getPersons().values()) {
			System.out.println("Person Id : " + person.getId());
			System.out.println("Person " + person.getId() + "s total causedNoiseCost: " + 
					noiseHandler.getPersonId2causedNoiseCost().get(person.getId()));
			System.out.println("Person " + person.getId() + "s PersonId2TripNumber2causedNoiseCost: " + 
					noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(person.getId()));
			System.out.println("Person " + person.getId() + "s total affectedNoiseCost: " + 
					noiseHandler.getPersonId2affectedNoiseCost().get(person.getId()));
		}
	}
	
}
