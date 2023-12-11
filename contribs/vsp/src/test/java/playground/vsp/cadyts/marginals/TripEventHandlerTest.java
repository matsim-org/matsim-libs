package playground.vsp.cadyts.marginals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.HandlingOfPlansWithoutRoutingMode;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TripEventHandlerTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * This test takes the pt-tutorial from the scenarios module and performs one iteration. Afterwards it is tested
	 * whether every agents has performed trips. For two agents the trips are investigated in detail.
	 * <p>
	 * Also one agent is excluded from the recording, to test, whether the agent filter works correctly.
	 *
	 * @throws MalformedURLException another stupid api
	 */
	@Test
	void test() throws MalformedURLException {

		// url is such a weird api
		URL ptTutorial = URI.create(ExamplesUtils.getTestScenarioURL("pt-tutorial").toString() + "0.config.xml").toURL();
		Config config = ConfigUtils.loadConfig(ptTutorial);
		config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);

		// use the config and run only one iteration
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// this is our test object
		TripEventHandler objectUnderTest = new TripEventHandler();

		// create a controler and bind the event handler to be executed during qsim
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(objectUnderTest);
				// exclude agent with id -> 876
				bind(AgentFilter.class).toInstance(id -> !id.equals(Id.createPersonId("876")));
			}
		});
		controler.run();

		// evaluate the recorded trips
		Map<Id<Person>, List<TripEventHandler.Trip>> personTrips = objectUnderTest.getTrips();

		// assert that every person was recorded except agent-876
		assertEquals(scenario.getPopulation().getPersons().size() - 1, personTrips.size());
		assertFalse(personTrips.containsKey(Id.createPersonId("876")));

		// assert a certain person has done a car trip
		List<TripEventHandler.Trip> tripsOfPerson101 = personTrips.get(Id.createPersonId("101"));
		assertEquals(2, tripsOfPerson101.size());
		assertEquals(TransportMode.car, tripsOfPerson101.get(0).getMainMode());
		assertEquals(TransportMode.car, tripsOfPerson101.get(1).getMainMode());

		// asssert a certain person has done a pt trip
		List<TripEventHandler.Trip> tripsOfPerson102 = personTrips.get(Id.createPersonId("102"));
		assertEquals(3, tripsOfPerson102.size());

		// this trip is walk -> pt -> walk
		assertEquals(TransportMode.pt, tripsOfPerson102.get(0).getMainMode());
		assertEquals(3, tripsOfPerson102.get(0).getLegs().size());
		assertEquals(TransportMode.walk, tripsOfPerson102.get(0).getLegs().get(0).getMode());
		assertEquals(TransportMode.pt, tripsOfPerson102.get(0).getLegs().get(1).getMode());
		assertEquals(TransportMode.walk, tripsOfPerson102.get(0).getLegs().get(2).getMode());

		// this trip is only transit_walk
		assertEquals(TransportMode.walk, tripsOfPerson102.get(1).getMainMode());
		assertEquals(1, tripsOfPerson102.get(1).getLegs().size());
		assertEquals(TransportMode.walk, tripsOfPerson102.get(1).getLegs().get(0).getMode());

		// this trip is walk -> pt -> walk
		assertEquals(TransportMode.pt, tripsOfPerson102.get(2).getMainMode());
		assertEquals(3, tripsOfPerson102.get(2).getLegs().size());
		assertEquals(TransportMode.walk, tripsOfPerson102.get(2).getLegs().get(0).getMode());
		assertEquals(TransportMode.pt, tripsOfPerson102.get(2).getLegs().get(1).getMode());
		assertEquals(TransportMode.walk, tripsOfPerson102.get(2).getLegs().get(2).getMode());
	}
}
