package playground.vsp.simpleParkingCostHandler;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.*;

/**
 * Tests for the ParkingCostHandler class.
 * The following test cases could be added:
 * - Test chain of parking events.
 * - Test activity prefixes excluded from parking.
 */
public class ParkingCostHandlerTest {
	private Injector injector;

	@BeforeEach
	public void setup() {
		Config config = ConfigUtils.createConfig();
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = NetworkUtils.createNetwork(config);

		TestsEventsManager eventsManager = new TestsEventsManager();
		ParkingCostConfigGroup configGroup = new ParkingCostConfigGroup();

		injector = Guice.createInjector(new AbstractModule() {
			@Override
			public void configure() {
				bind(ParkingCostConfigGroup.class).toInstance(configGroup);
				bind(EventsManager.class).toInstance(eventsManager);
				bind(Scenario.class).toInstance(scenario);
				bind(Network.class).toInstance(network);
			}
		});
	}
	// Basic Event Handling Tests
	@Test
	public void transitDriverStartsEventTest() {
		Person ptDriver = new Tester(0);
		TransitDriverStartsEvent tDSE = new TransitDriverStartsEvent(0, ptDriver.getId(), null, null, null, null);
		ParkingCostHandler pch = injector.getInstance(ParkingCostHandler.class);
		pch.handleEvent(tDSE);
		Assertions.assertTrue(pch.getPtDrivers().contains(ptDriver.getId()));
	}

	@Test
	public void personLeavesVehicleEventTest() {
		Person ptDriver = new Tester(0);
		Person tester = new Tester(1);

		TransitDriverStartsEvent tDSEvent = new TransitDriverStartsEvent(0, ptDriver.getId(), null, null, null, null);
		PersonLeavesVehicleEvent pLVEventParking = new PersonLeavesVehicleEvent(100, tester.getId(), null);
		PersonLeavesVehicleEvent pLVEventPtDriver = new PersonLeavesVehicleEvent(100, ptDriver.getId(), null);

		ParkingCostHandler pch = injector.getInstance(ParkingCostHandler.class);

		pch.handleEvent(tDSEvent);
		pch.handleEvent(pLVEventParking);
		pch.handleEvent(pLVEventPtDriver);

		Assertions.assertTrue(pch.getPtDrivers().contains(ptDriver.getId()));
		Assertions.assertFalse(pch.getPersonId2lastLeaveVehicleTime().containsKey(ptDriver.getId()));
		Assertions.assertTrue(pch.getPersonId2lastLeaveVehicleTime().containsKey(tester.getId()));
		Assertions.assertEquals(100.,pch.getPersonId2lastLeaveVehicleTime().get(tester.getId()));
	}
	@Test
	public void activityEndEventTest() {
		Person tester1 = new Tester(0);
		String actType1 = "test";

		Person tester2 = new Tester(1);
		String actType2 = "test-interaction";

		Person ptDriver = new Tester(2);

		ActivityEndEvent aEEventActivity = new ActivityEndEvent(0, tester1.getId(), null, null,actType1, null);
		ActivityEndEvent aEEventInteraction = new ActivityEndEvent(0, tester2.getId(), null, null, actType2, null);
		TransitDriverStartsEvent tDSEvent = new TransitDriverStartsEvent(0, ptDriver.getId(), null, null, null, null);

		ParkingCostHandler pch = injector.getInstance(ParkingCostHandler.class);

		pch.handleEvent(tDSEvent);
		pch.handleEvent(aEEventActivity);
		pch.handleEvent(aEEventInteraction);

		Assertions.assertNull(pch.getPersonId2previousActivity().get(tester2.getId()));
		Assertions.assertEquals(actType1, pch.getPersonId2previousActivity().get(tester1.getId()));

		Assertions.assertFalse(pch.getPersonId2previousActivity().containsKey(ptDriver.getId()));
		Assertions.assertTrue(pch.getPtDrivers().contains(ptDriver.getId()));
	}
	@Test
	public void personDepartureEventTest() {
		Person testerWithCar = new Tester(0);
		Person testerWithoutCar = new Tester(1);
		Person ptDriver = new Tester(2);

		PersonDepartureEvent pDEventCar = new PersonDepartureEvent(0, testerWithCar.getId(),null, "car",null);
		PersonDepartureEvent pDEventNoCar = new PersonDepartureEvent(0, testerWithoutCar.getId(), null, "pt", null);
		PersonDepartureEvent pDEventPtDriver = new PersonDepartureEvent(0, ptDriver.getId(),null,"pt",null);
		TransitDriverStartsEvent tDSEvent = new TransitDriverStartsEvent(0, ptDriver.getId(), null, null, null, null);

		ParkingCostHandler pch = injector.getInstance(ParkingCostHandler.class);

		pch.handleEvent(tDSEvent);
		pch.handleEvent(pDEventCar);
		pch.handleEvent(pDEventNoCar);
		pch.handleEvent(pDEventPtDriver);

		Assertions.assertTrue(pch.getPtDrivers().contains(ptDriver.getId()));
		Assertions.assertFalse(pch.getPersonId2relevantModeLinkId().containsKey(ptDriver.getId()));
		Assertions.assertFalse(pch.getPersonId2relevantModeLinkId().containsKey(testerWithoutCar.getId()));
		Assertions.assertTrue(pch.getPersonId2relevantModeLinkId().containsKey(testerWithCar.getId()));
	}
	@Test
	public void personEntersVehicleEventTest() {
		// tests basic functionality and cost calculation
		Link link = createNetworkAndSetupLink(10,440,5,15,500,30,30);
		// Persons construction
		Person ptDriver = new Tester(0); // should not pay at all
		Person resident = new Tester(1); // should pay residential parking costs
		Person shopper = new Tester(2); // should pay non-residential parking costs
		// Event construction
			// ptDriver events
		TransitDriverStartsEvent tDSEvent = new TransitDriverStartsEvent(0, ptDriver.getId(), null, null, null, null);
		PersonLeavesVehicleEvent pLVEventPtDriver = new PersonLeavesVehicleEvent(1000, ptDriver.getId(), null);
		PersonEntersVehicleEvent pEVEventPtDriver = new PersonEntersVehicleEvent(11000, ptDriver.getId(),null);
			// resident events
		List<Event> residentEvents = createParkingEvents(resident.getId(),link.getId(),"home","car",10);
			// shopper events
		List<Event> shopperEvents = createParkingEvents(shopper.getId(),link.getId(),"shopping","car",1);

		// Event handling
		ParkingCostHandler pch = injector.getInstance(ParkingCostHandler.class);
			// ptDriver handling
		pch.handleEvent(tDSEvent);
		pch.handleEvent(pLVEventPtDriver);
		pch.handleEvent(pEVEventPtDriver);
			// resident handling
		handleParkingEvents(pch, residentEvents);
			// shopper handling
		handleParkingEvents(pch, shopperEvents);

		// Assertions
		TestsEventsManager eventsManager = (TestsEventsManager) injector.getInstance(EventsManager.class);
			// ptDriver assertions
		Assertions.assertTrue(pch.getPtDrivers().contains(ptDriver.getId()));
		Assertions.assertNull(eventsManager.getEventByPersonId(ptDriver.getId()));
			// resident assertions
		Assertions.assertTrue(pch.getHasAlreadyPaidDailyResidentialParkingCosts().contains(resident.getId()));
		Assertions.assertEquals(-10.0, eventsManager.getEventByPersonId(resident.getId()).getAmount());
		Assertions.assertEquals("residential parking", eventsManager.getEventByPersonId(resident.getId()).getPurpose());
			// shopper assertions
		Assertions.assertFalse(pch.getHasAlreadyPaidDailyResidentialParkingCosts().contains(shopper.getId()));
		Assertions.assertEquals(-5, eventsManager.getEventByPersonId(shopper.getId()).getAmount());
		Assertions.assertEquals("non-residential parking", eventsManager.getEventByPersonId(shopper.getId()).getPurpose());

	}
	// Parking Cost Calculation Tests
	@Test
	public void penaltyParkingCostCalculationsTest() {
		// Network construction
		Link link = createNetworkAndSetupLink(0, 200,10,10,200,2,100);
		// Person construction
		Person tester1 = new Tester(0);
		Person tester2 = new Tester(1);
		// Event construction
		List<Event> events = createParkingEvents(tester1.getId(),link.getId(),"other","car",3);
		// Event handling
		ParkingCostHandler pch = injector.getInstance(ParkingCostHandler.class);
		handleParkingEvents(pch, events);
		handleParkingEvents(pch, createParkingEvents(tester2.getId(),link.getId(),"other","car",20));
		// Assertions
		TestsEventsManager eventsManager = (TestsEventsManager) injector.getInstance(EventsManager.class);
		Assertions.assertEquals(-100, eventsManager.getEventByPersonId(tester1.getId()).getAmount());
		Assertions.assertEquals(-200, eventsManager.getEventByPersonId(tester2.getId()).getAmount());
	}
	@Test
	public void firstHourParkingCostCalculationTest() {
		Link link = createNetworkAndSetupLink(0, 50,10,10,100,10,0);

		Person tester1 = new Tester(0);
		Person tester2 = new Tester(1);

		ParkingCostHandler pch = injector.getInstance(ParkingCostHandler.class);
		handleParkingEvents(pch, createParkingEvents(tester1.getId(),link.getId(),"other","car",1));
		handleParkingEvents(pch, createParkingEvents(tester2.getId(),link.getId(),"other","car",0.5)); // rounded up to 1hr

		TestsEventsManager eventsManager = (TestsEventsManager) injector.getInstance(EventsManager.class);
		Assertions.assertEquals(-10, eventsManager.getEventByPersonId(tester1.getId()).getAmount());
		Assertions.assertEquals(-10, eventsManager.getEventByPersonId(tester2.getId()).getAmount());
	}
	@Test
	public void hourlyParkingCostCalculationTest() {
		Link link = createNetworkAndSetupLink(0, 150,10,20,100,10,0);

		Person parking1hr = new Tester(0);
		Person parking3hr = new Tester(1);
		Person parking8hr = new Tester(2);

		ParkingCostHandler pch = injector.getInstance(ParkingCostHandler.class);
		handleParkingEvents(pch, createParkingEvents(parking1hr.getId(),link.getId(),"other","car",1));
		handleParkingEvents(pch, createParkingEvents(parking3hr.getId(),link.getId(),"other","car",3));
		handleParkingEvents(pch, createParkingEvents(parking8hr.getId(),link.getId(),"other","car",8));

		TestsEventsManager eventsManager = (TestsEventsManager) injector.getInstance(EventsManager.class);
		Assertions.assertEquals(-10, eventsManager.getEventByPersonId(parking1hr.getId()).getAmount()); // first hour parking cost only
		Assertions.assertEquals(-50, eventsManager.getEventByPersonId(parking3hr.getId()).getAmount()); // 1x 10 + 2x 20 hourly costs
		Assertions.assertEquals(-100, eventsManager.getEventByPersonId(parking8hr.getId()).getAmount()); // 150 in hourly costs, capped by maxDailyParkingCosts
	}
	@Test
	public void residentialFeeIsPaidOnceTest() {
		Link link = createNetworkAndSetupLink(100, 0,0,0,0,0,0);

		Person tester = new Tester(0);

		ParkingCostHandler pch = injector.getInstance(ParkingCostHandler.class);
		handleParkingEvents(pch, createParkingEvents(tester.getId(),link.getId(),"home","car",10));
		handleParkingEvents(pch, createParkingEvents(tester.getId(),link.getId(),"home","car",2));

		TestsEventsManager eventsManager = (TestsEventsManager) injector.getInstance(EventsManager.class);
		Assertions.assertEquals(1, eventsManager.getEvents().size());
		Assertions.assertEquals(-100, eventsManager.getEventByPersonId(tester.getId()).getAmount());
	}

	@Test
	public void handlerDefaultsTest() {
		Network network = injector.getInstance(Scenario.class).getNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId(1), new Coord(0, 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId(2), new Coord(10, 0));
		Link link = NetworkUtils.createAndAddLink(network, Id.createLinkId(1), node1, node2,10,30,100,3);

		Person tester = new Tester(0);

		ParkingCostHandler pch = injector.getInstance(ParkingCostHandler.class);
		handleParkingEvents(pch, createParkingEvents(tester.getId(),link.getId(),"other","car",10));

		TestsEventsManager eventsManager = (TestsEventsManager) injector.getInstance(EventsManager.class);
		Assertions.assertTrue(eventsManager.getEvents().isEmpty());
	}
	@Test
	public void testDailyParkingCost() {
		Link link = createNetworkAndSetupLink(0, 50,10,10,100,10,0);

		Person tester1 = new Tester(0);
		Person tester2 = new Tester(1);

		ParkingCostHandler pch = injector.getInstance(ParkingCostHandler.class);
		handleParkingEvents(pch, createParkingEvents(tester1.getId(),link.getId(),"other","car",10));
		handleParkingEvents(pch, createParkingEvents(tester2.getId(),link.getId(),"other","car",2));

		TestsEventsManager eventsManager = (TestsEventsManager) injector.getInstance(EventsManager.class);
		Assertions.assertEquals(-50, eventsManager.getEventByPersonId(tester1.getId()).getAmount());
		Assertions.assertEquals(-20, eventsManager.getEventByPersonId(tester2.getId()).getAmount());
	}

	// sets up given cost attributes for a given link
	private Link createNetworkAndSetupLink(double residentialParkingFee, double dailyParkingCost, double firstHourParkingCost,
										   double extraHourParkingCost, double maxDailyParkingCost, double maxParkingDurationHrs, double parkingPenaltyCost) {

		Network network = injector.getInstance(Scenario.class).getNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId(1), new Coord(0, 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId(2), new Coord(10, 0));
		Link link = NetworkUtils.createAndAddLink(network, Id.createLinkId(1), node1, node2,10,30,100,3);

		ParkingCostConfigGroup configGroup = injector.getInstance(ParkingCostConfigGroup.class);

		link.getAttributes().putAttribute(configGroup.getResidentialParkingFeeAttributeName(), residentialParkingFee);
		link.getAttributes().putAttribute(configGroup.getDailyParkingCostLinkAttributeName(), dailyParkingCost);
		link.getAttributes().putAttribute(configGroup.getFirstHourParkingCostLinkAttributeName(), firstHourParkingCost);
		link.getAttributes().putAttribute(configGroup.getExtraHourParkingCostLinkAttributeName(), extraHourParkingCost);
		link.getAttributes().putAttribute(configGroup.getMaxDailyParkingCostLinkAttributeName(), maxDailyParkingCost);
		link.getAttributes().putAttribute(configGroup.getMaxParkingDurationAttributeName(), maxParkingDurationHrs);
		link.getAttributes().putAttribute(configGroup.getParkingPenaltyAttributeName(), parkingPenaltyCost);

		return link;
	}

	private List<Event> createParkingEvents(Id<Person> personId, Id<Link> linkId, String actType, String mode, double durationHrs){
		List<Event> events = new ArrayList<>();
		events.add(new PersonLeavesVehicleEvent(0, personId, null));
		events.add(new ActivityEndEvent(0, personId, linkId,null, actType,null));
		events.add(new PersonDepartureEvent(0, personId, linkId, mode,null));
		events.add(new PersonEntersVehicleEvent(durationHrs * 3600, personId,null));
		return events;
	}

	private void handleParkingEvents(ParkingCostHandler pch, List<Event> events) {
		for (Event event : events) {
			if (event instanceof PersonLeavesVehicleEvent) {
				pch.handleEvent((PersonLeavesVehicleEvent) event);
			}
			if (event instanceof ActivityEndEvent) {
				pch.handleEvent((ActivityEndEvent) event);
			}
			if (event instanceof PersonDepartureEvent) {
				pch.handleEvent((PersonDepartureEvent) event);
			}
			if (event instanceof PersonEntersVehicleEvent) {
				pch.handleEvent((PersonEntersVehicleEvent) event);
			}
		}
	}
}
class TestsEventsManager implements EventsManager {
	private final List<PersonMoneyEvent> events = new ArrayList<>();

	@Override
	public void processEvent(Event event) {
		if (event instanceof PersonMoneyEvent) {
			events.add((PersonMoneyEvent) event);
		}
	}
	public List<PersonMoneyEvent> getEvents() {
		return events;
	}

	public PersonMoneyEvent getEventByPersonId(Id<Person> id) {

		List<PersonMoneyEvent> personMoneyEvents = events.stream().filter(e -> e.getPersonId().equals(id)).toList();

		if (personMoneyEvents.size() > 1) {
			Assertions.fail("Person has more money events than expected");
		}

		return personMoneyEvents.stream().findAny().orElse(null);
	}
	@Deprecated
	public void addHandler(EventHandler handler) {}
	@Deprecated
	public void removeHandler(EventHandler handler) {}
	@Deprecated
	public void resetHandlers(int iteration) {}
	@Deprecated
	public void initProcessing() {}
	@Deprecated
	public void afterSimStep(double time) {}
	@Deprecated
	public void finishProcessing() {}
}

class Tester implements Person {

	public Tester(long key) {
		this.id = Id.createPersonId(key);
	}

	private final Id<Person> id;

	@Override
	public Id<Person> getId() {return this.id;}
	@Deprecated
	public Attributes getAttributes() {return null;}
	@Deprecated
	public Map<String, Object> getCustomAttributes() {return null;}
	@Deprecated
	public List<? extends Plan> getPlans() {return null;}
	@Deprecated
	public boolean addPlan(Plan p) {return false;}
	@Deprecated
	public boolean removePlan(Plan p) {return false;}
	@Deprecated
	public Plan getSelectedPlan() {return null;}
	@Deprecated
	public void setSelectedPlan(Plan selectedPlan) {}
	@Deprecated
	public Plan createCopyOfSelectedPlanAndMakeSelected() {return null;}
}
