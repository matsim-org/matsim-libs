/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package ch.sbb.matsim.mobsim.qsim.pt;

import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEngineModule;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.pt.DefaultTransitDriverAgentFactory;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.utils.EventsCollector;

/**
 * @author mrieser / SBB
 */
public class SBBTransitQSimEngineTest {

    private static final Logger log = LogManager.getLogger(SBBTransitQSimEngineTest.class);

    private static void assertEqualEvent(Class<? extends Event> eventClass, double time, Event event) {
        Assertions.assertTrue(event.getClass().isAssignableFrom(event.getClass()));
        Assertions.assertEquals(time, event.getTime(), 1e-7);
    }

	@Test
	void testDriver() {
        TestFixture f = new TestFixture();

        EventsManager eventsManager = EventsUtils.createEventsManager(f.config);
        QSim qSim = new QSimBuilder(f.config).useDefaults().build(f.scenario, eventsManager);
        SBBTransitQSimEngine trEngine = new SBBTransitQSimEngine(qSim, null, new TransitStopAgentTracker(eventsManager), new DefaultTransitDriverAgentFactory());
        qSim.addMobsimEngine(trEngine);

        trEngine.insertAgentsIntoMobsim();

        Map<Id<Person>, MobsimAgent> agents = qSim.getAgents();
        Assertions.assertEquals(1, agents.size(), "Expected one driver as agent.");
        MobsimAgent agent = agents.values().iterator().next();
        Assertions.assertTrue(agent instanceof SBBTransitDriverAgent);
        SBBTransitDriverAgent driver = (SBBTransitDriverAgent) agent;
        TransitRoute route = driver.getTransitRoute();
        List<TransitRouteStop> stops = route.getStops();
        double depTime = driver.getActivityEndTime();

        assertNextStop(driver, stops.get(0), depTime);
        assertNextStop(driver, stops.get(1), depTime);
        assertNextStop(driver, stops.get(2), depTime);
        assertNextStop(driver, stops.get(3), depTime);
        assertNextStop(driver, stops.get(4), depTime);

        Assertions.assertNull(driver.getNextRouteStop());
    }

    private void assertNextStop(SBBTransitDriverAgent driver, TransitRouteStop stop, double routeDepTime) {
        double arrOffset = stop.getArrivalOffset().or(stop.getDepartureOffset()).seconds();
        double depOffset = stop.getDepartureOffset().or(stop.getArrivalOffset()).seconds();
        TransitStopFacility f = stop.getStopFacility();

        Assertions.assertEquals(stop, driver.getNextRouteStop());

        driver.arrive(stop, routeDepTime + arrOffset);
        double stopTimeSum = 0.0;
        double stopTime;
        do {
            stopTime = driver.handleTransitStop(f, routeDepTime + arrOffset + stopTimeSum);
            stopTimeSum += stopTime;
        } while (stopTime > 0);
        Assertions.assertEquals(depOffset - arrOffset, stopTimeSum, 1e-7);
        Assertions.assertEquals(0.0, stopTime, 1e-7, "last stop time should have been 0.0");
        driver.depart(f, routeDepTime + depOffset);
    }

	@Test
	void testEvents_withoutPassengers_withoutLinks() {
        TestFixture f = new TestFixture();

        EventsManager eventsManager = EventsUtils.createEventsManager(f.config);
        QSim qSim = new QSimBuilder(f.config) //
                .addQSimModule(new ActivityEngineModule())
                .addQSimModule(new SBBTransitEngineQSimModule())
                .addQSimModule(new TestQSimModule(f.config))
                .configureQSimComponents(new SBBTransitEngineQSimModule())
                .configureQSimComponents(configurator -> {
                    configurator.addNamedComponent(ActivityEngineModule.COMPONENT_NAME);
                })
                .build(f.scenario, eventsManager);

        EventsCollector collector = new EventsCollector();
        eventsManager.addHandler(collector);
        qSim.run();
        List<Event> allEvents = collector.getEvents();

        for (Event event : allEvents) {
            System.out.println(event.toString());
        }

        Assertions.assertEquals(15, allEvents.size(), "wrong number of events.");
        assertEqualEvent(TransitDriverStartsEvent.class, 30000, allEvents.get(0));
        assertEqualEvent(PersonDepartureEvent.class, 30000, allEvents.get(1));
        assertEqualEvent(PersonEntersVehicleEvent.class, 30000, allEvents.get(2));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30000, allEvents.get(3));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30000, allEvents.get(4));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30100, allEvents.get(5));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30120, allEvents.get(6));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30300, allEvents.get(7));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30300, allEvents.get(8));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30570, allEvents.get(9));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30600, allEvents.get(10));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30720, allEvents.get(11));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30720, allEvents.get(12));
        assertEqualEvent(PersonLeavesVehicleEvent.class, 30720, allEvents.get(13));
        assertEqualEvent(PersonArrivalEvent.class, 30720, allEvents.get(14));
    }

	/**
	* This test also checks that the passenger boarding/leaving times are correctly taken into account
	*/
	@Test
	void testEvents_withPassengers_withoutLinks() {
        TestFixture f = new TestFixture();
        f.addSingleTransitDemand();

        EventsManager eventsManager = EventsUtils.createEventsManager(f.config);
        QSim qSim = new QSimBuilder(f.config) //
                .addQSimModule(new ActivityEngineModule())
                .addQSimModule(new SBBTransitEngineQSimModule())
                .addQSimModule(new TestQSimModule(f.config))
                .addQSimModule(new PopulationModule())
                .configureQSimComponents(new SBBTransitEngineQSimModule())
                .configureQSimComponents(configurator -> {
                    configurator.addNamedComponent(ActivityEngineModule.COMPONENT_NAME);
                    configurator.addNamedComponent(PopulationModule.COMPONENT_NAME);
                })
                .build(f.scenario, eventsManager);

        EventsCollector collector = new EventsCollector();
        eventsManager.addHandler(collector);
        qSim.run();
        List<Event> allEvents = collector.getEvents();

        for (Event event : allEvents) {
            System.out.println(event.toString());
        }

        Assertions.assertEquals(22, allEvents.size(), "wrong number of events.");
        assertEqualEvent(ActivityEndEvent.class, 29500, allEvents.get(0)); // passenger
        assertEqualEvent(PersonDepartureEvent.class, 29500, allEvents.get(1)); // passenger
        assertEqualEvent(AgentWaitingForPtEvent.class, 29500, allEvents.get(2)); // passenger
        assertEqualEvent(TransitDriverStartsEvent.class, 30000, allEvents.get(3));
        assertEqualEvent(PersonDepartureEvent.class, 30000, allEvents.get(4)); // driver
        assertEqualEvent(PersonEntersVehicleEvent.class, 30000, allEvents.get(5)); // driver
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30000, allEvents.get(6));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30000, allEvents.get(7));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30100, allEvents.get(8));
        assertEqualEvent(PersonEntersVehicleEvent.class, 30101, allEvents.get(9)); // passenger
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30120, allEvents.get(10));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30300, allEvents.get(11));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30300, allEvents.get(12));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30570, allEvents.get(13));
        assertEqualEvent(PersonLeavesVehicleEvent.class, 30571, allEvents.get(14)); // passenger
        assertEqualEvent(PersonArrivalEvent.class, 30571, allEvents.get(15)); // passenger
        assertEqualEvent(ActivityStartEvent.class, 30571, allEvents.get(16)); // passenger
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30600, allEvents.get(17));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30720, allEvents.get(18));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30720, allEvents.get(19));
        assertEqualEvent(PersonLeavesVehicleEvent.class, 30720, allEvents.get(20)); // driver
        assertEqualEvent(PersonArrivalEvent.class, 30720, allEvents.get(21)); // driver
    }

	@Test
	void testEvents_withThreePassengers_withoutLinks() {
        TestFixture f = new TestFixture();
        f.addTripleTransitDemand();

        EventsManager eventsManager = EventsUtils.createEventsManager(f.config);
        QSim qSim = new QSimBuilder(f.config) //
                .addQSimModule(new ActivityEngineModule())
                .addQSimModule(new SBBTransitEngineQSimModule())
                .addQSimModule(new TestQSimModule(f.config))
                .addQSimModule(new PopulationModule())
                .configureQSimComponents(new SBBTransitEngineQSimModule())
                .configureQSimComponents(configurator -> {
                    configurator.addNamedComponent(ActivityEngineModule.COMPONENT_NAME);
                    configurator.addNamedComponent(PopulationModule.COMPONENT_NAME);
                })
                .build(f.scenario, eventsManager);

        EventsCollector collector = new EventsCollector();
        eventsManager.addHandler(collector);
        qSim.run();
        List<Event> allEvents = collector.getEvents();

        for (Event event : allEvents) {
            System.out.println(event.toString());
        }

        Assertions.assertEquals(36, allEvents.size(), "wrong number of events.");
        assertEqualEvent(ActivityEndEvent.class, 29500, allEvents.get(0)); // passenger 1
        assertEqualEvent(ActivityEndEvent.class, 29500, allEvents.get(1)); // passenger 2
        assertEqualEvent(ActivityEndEvent.class, 29500, allEvents.get(2)); // passenger 3
        assertEqualEvent(PersonDepartureEvent.class, 29500, allEvents.get(3)); // passenger 1
        assertEqualEvent(PersonDepartureEvent.class, 29500, allEvents.get(4)); // passenger 2
        assertEqualEvent(PersonDepartureEvent.class, 29500, allEvents.get(5)); // passenger 3
        assertEqualEvent(AgentWaitingForPtEvent.class, 29500, allEvents.get(6)); // passenger 1
        assertEqualEvent(AgentWaitingForPtEvent.class, 29500, allEvents.get(7)); // passenger 2
        assertEqualEvent(AgentWaitingForPtEvent.class, 29500, allEvents.get(8)); // passenger 3
        assertEqualEvent(TransitDriverStartsEvent.class, 30000, allEvents.get(9));
        assertEqualEvent(PersonDepartureEvent.class, 30000, allEvents.get(10)); // driver
        assertEqualEvent(PersonEntersVehicleEvent.class, 30000, allEvents.get(11)); // driver
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30000, allEvents.get(12));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30000, allEvents.get(13));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30100, allEvents.get(14));
        assertEqualEvent(PersonEntersVehicleEvent.class, 30101, allEvents.get(15)); // passenger 1
        assertEqualEvent(PersonEntersVehicleEvent.class, 30103, allEvents.get(16)); // passenger 2
        assertEqualEvent(PersonEntersVehicleEvent.class, 30105, allEvents.get(17)); // passenger 3
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30120, allEvents.get(18));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30300, allEvents.get(19));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30300, allEvents.get(20));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30570, allEvents.get(21));
        assertEqualEvent(PersonLeavesVehicleEvent.class, 30571, allEvents.get(22)); // passenger 1
        assertEqualEvent(PersonArrivalEvent.class, 30571, allEvents.get(23)); // passenger 1
        assertEqualEvent(ActivityStartEvent.class, 30571, allEvents.get(24)); // passenger 1
        assertEqualEvent(PersonLeavesVehicleEvent.class, 30573, allEvents.get(25)); // passenger 2
        assertEqualEvent(PersonArrivalEvent.class, 30573, allEvents.get(26)); // passenger 2
        assertEqualEvent(ActivityStartEvent.class, 30573, allEvents.get(27)); // passenger 2
        assertEqualEvent(PersonLeavesVehicleEvent.class, 30575, allEvents.get(28)); // passenger 3
        assertEqualEvent(PersonArrivalEvent.class, 30575, allEvents.get(29)); // passenger 3
        assertEqualEvent(ActivityStartEvent.class, 30575, allEvents.get(30)); // passenger 3
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30600, allEvents.get(31));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30720, allEvents.get(32));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30720, allEvents.get(33));
        assertEqualEvent(PersonLeavesVehicleEvent.class, 30720, allEvents.get(34)); // driver
        assertEqualEvent(PersonArrivalEvent.class, 30720, allEvents.get(35)); // driver
    }

	@Test
	void testEvents_withoutPassengers_withLinks() {
        TestFixture f = new TestFixture();
        f.sbbConfig.setCreateLinkEventsInterval(1);

        EventsManager eventsManager = EventsUtils.createEventsManager(f.config);
        QSim qSim = new QSimBuilder(f.config) //
                .addQSimModule(new ActivityEngineModule())
                .addQSimModule(new SBBTransitEngineQSimModule())
                .addQSimModule(new TestQSimModule(f.config))
                .configureQSimComponents(new SBBTransitEngineQSimModule())
                .configureQSimComponents(configurator -> {
                    configurator.addNamedComponent(ActivityEngineModule.COMPONENT_NAME);
                })
                .build(f.scenario, eventsManager);

        EventsCollector collector = new EventsCollector();
        eventsManager.addHandler(collector);
        qSim.run();
        List<Event> allEvents = collector.getEvents();

        for (Event event : allEvents) {
            System.out.println(event.toString());
        }

        Assertions.assertEquals(23, allEvents.size(), "wrong number of events.");
        assertEqualEvent(TransitDriverStartsEvent.class, 30000, allEvents.get(0));
        assertEqualEvent(PersonDepartureEvent.class, 30000, allEvents.get(1));
        assertEqualEvent(PersonEntersVehicleEvent.class, 30000, allEvents.get(2));
        assertEqualEvent(VehicleEntersTrafficEvent.class, 30000, allEvents.get(3));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30000, allEvents.get(4));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30000, allEvents.get(5));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30100, allEvents.get(6));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30120, allEvents.get(7));
        assertEqualEvent(LinkLeaveEvent.class, 30121, allEvents.get(8)); // link 1
        assertEqualEvent(LinkEnterEvent.class, 30121, allEvents.get(9)); // link 2
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30300, allEvents.get(10));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30300, allEvents.get(11));
        assertEqualEvent(LinkLeaveEvent.class, 30301, allEvents.get(12)); // link 2
        assertEqualEvent(LinkEnterEvent.class, 30301, allEvents.get(13)); // link 3
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30570, allEvents.get(14));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30600, allEvents.get(15));
        assertEqualEvent(LinkLeaveEvent.class, 30601, allEvents.get(16)); // link 3
        assertEqualEvent(LinkEnterEvent.class, 30601, allEvents.get(17)); // link 4
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30720, allEvents.get(18));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30720, allEvents.get(19));
        assertEqualEvent(VehicleLeavesTrafficEvent.class, 30720, allEvents.get(20));
        assertEqualEvent(PersonLeavesVehicleEvent.class, 30720, allEvents.get(21));
        assertEqualEvent(PersonArrivalEvent.class, 30720, allEvents.get(22));
    }

	@Test
	void testEvents_withoutPassengers_withLinks_Sesselbahn() {
        TestFixture f = new TestFixture();
        f.addSesselbahn(true, false);
        f.sbbConfig.setCreateLinkEventsInterval(1);

        EventsManager eventsManager = EventsUtils.createEventsManager(f.config);
        QSim qSim = new QSimBuilder(f.config) //
                .addQSimModule(new ActivityEngineModule())
                .addQSimModule(new SBBTransitEngineQSimModule())
                .addQSimModule(new TestQSimModule(f.config))
                .configureQSimComponents(configurator -> {
                    configurator.addNamedComponent(ActivityEngineModule.COMPONENT_NAME);
                })
                .configureQSimComponents(new SBBTransitEngineQSimModule())
                .build(f.scenario, eventsManager);

        EventsCollector collector = new EventsCollector();
        eventsManager.addHandler(collector);
        qSim.run();
        List<Event> allEvents = collector.getEvents();

        for (Event event : allEvents) {
            System.out.println(event.toString());
        }

        Assertions.assertEquals(13, allEvents.size(), "wrong number of events.");
        assertEqualEvent(TransitDriverStartsEvent.class, 35000, allEvents.get(0));
        assertEqualEvent(PersonDepartureEvent.class, 35000, allEvents.get(1));
        assertEqualEvent(PersonEntersVehicleEvent.class, 35000, allEvents.get(2));
        assertEqualEvent(VehicleEntersTrafficEvent.class, 35000, allEvents.get(3));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 35000, allEvents.get(4)); // stop B
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 35000, allEvents.get(5)); // stop B
        assertEqualEvent(LinkLeaveEvent.class, 35001, allEvents.get(6)); // link 1
        assertEqualEvent(LinkEnterEvent.class, 35001, allEvents.get(7)); // link 2
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 35120, allEvents.get(8)); // stop C
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 35120, allEvents.get(9));
        assertEqualEvent(VehicleLeavesTrafficEvent.class, 35120, allEvents.get(10));
        assertEqualEvent(PersonLeavesVehicleEvent.class, 35120, allEvents.get(11));
        assertEqualEvent(PersonArrivalEvent.class, 35120, allEvents.get(12));
    }

	/**
	* during development, I had a case where the traveltime-offset between two stops was 0 seconds. This resulted in the 2nd stop being immediately handled, and only after that the
	* linkLeave/linkEnter-Events were generated. As the 2nd stop happened to be the last one, the driver already left the vehicle, which then resulted in an exception in some event handler trying to
	* figure out the driver of the vehicle driving around. This tests reproduces this situation in order to check that the code can correctly cope with such a situation.
	*/
	@Test
	void testEvents_withoutPassengers_withLinks_SesselbahnMalformed() {
        TestFixture f = new TestFixture();
        f.addSesselbahn(true, true);
        f.sbbConfig.setCreateLinkEventsInterval(1);

        EventsManager eventsManager = EventsUtils.createEventsManager(f.config);
        QSim qSim = new QSimBuilder(f.config) //
                .addQSimModule(new ActivityEngineModule())
                .addQSimModule(new SBBTransitEngineQSimModule())
                .addQSimModule(new TestQSimModule(f.config))
                .configureQSimComponents(new SBBTransitEngineQSimModule())
                .configureQSimComponents(configurator -> {
                    configurator.addNamedComponent(ActivityEngineModule.COMPONENT_NAME);
                })
                .build(f.scenario, eventsManager);

        EventsCollector collector = new EventsCollector();
        eventsManager.addHandler(collector);
        qSim.run();
        List<Event> allEvents = collector.getEvents();

        for (Event event : allEvents) {
            System.out.println(event.toString());
        }

        Assertions.assertEquals(13, allEvents.size(), "wrong number of events.");
        assertEqualEvent(TransitDriverStartsEvent.class, 35000, allEvents.get(0));
        assertEqualEvent(PersonDepartureEvent.class, 35000, allEvents.get(1));
        assertEqualEvent(PersonEntersVehicleEvent.class, 35000, allEvents.get(2));
        assertEqualEvent(VehicleEntersTrafficEvent.class, 35000, allEvents.get(3));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 35000, allEvents.get(4)); // stop B
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 35000, allEvents.get(5)); // stop B
        assertEqualEvent(LinkLeaveEvent.class, 35000, allEvents.get(6)); // link 1
        assertEqualEvent(LinkEnterEvent.class, 35000, allEvents.get(7)); // link 2
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 35000, allEvents.get(8)); // stop C
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 35000, allEvents.get(9));
        assertEqualEvent(VehicleLeavesTrafficEvent.class, 35000, allEvents.get(10));
        assertEqualEvent(PersonLeavesVehicleEvent.class, 35000, allEvents.get(11));
        assertEqualEvent(PersonArrivalEvent.class, 35000, allEvents.get(12));
    }

	/**
	* During first tests, some events were out-of-order in time, leading to an exception, due to the first stop on a route having a departure offset > 0. This tests reproduces this situation in order
	* to check that the code can correctly cope with such a situation.
	*/
	@Test
	void testEvents_withoutPassengers_withLinks_DelayedFirstDeparture() {
        TestFixture f = new TestFixture();
        f.delayDepartureAtFirstStop();
        f.sbbConfig.setCreateLinkEventsInterval(1);

        EventsManager eventsManager = EventsUtils.createEventsManager(f.config);
        QSim qSim = new QSimBuilder(f.config) //
                .addQSimModule(new ActivityEngineModule())
                .addQSimModule(new SBBTransitEngineQSimModule())
                .addQSimModule(new TestQSimModule(f.config))
                .configureQSimComponents(new SBBTransitEngineQSimModule())
                .configureQSimComponents(configurator -> {
                    configurator.addNamedComponent(ActivityEngineModule.COMPONENT_NAME);
                })
                .build(f.scenario, eventsManager);

        EventsCollector collector = new EventsCollector();
        eventsManager.addHandler(collector);
        qSim.run();
        List<Event> allEvents = collector.getEvents();

        for (Event event : allEvents) {
            System.out.println(event.toString());
        }

        Assertions.assertEquals(23, allEvents.size(), "wrong number of events.");
        assertEqualEvent(TransitDriverStartsEvent.class, 30000, allEvents.get(0));
        assertEqualEvent(PersonDepartureEvent.class, 30000, allEvents.get(1));
        assertEqualEvent(PersonEntersVehicleEvent.class, 30000, allEvents.get(2));
        assertEqualEvent(VehicleEntersTrafficEvent.class, 30000, allEvents.get(3));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30000, allEvents.get(4));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30020, allEvents.get(5));
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30100, allEvents.get(6));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30120, allEvents.get(7));
        assertEqualEvent(LinkLeaveEvent.class, 30121, allEvents.get(8)); // link 1
        assertEqualEvent(LinkEnterEvent.class, 30121, allEvents.get(9)); // link 2
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30300, allEvents.get(10));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30300, allEvents.get(11));
        assertEqualEvent(LinkLeaveEvent.class, 30301, allEvents.get(12)); // link 2
        assertEqualEvent(LinkEnterEvent.class, 30301, allEvents.get(13)); // link 3
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30570, allEvents.get(14));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30600, allEvents.get(15));
        assertEqualEvent(LinkLeaveEvent.class, 30601, allEvents.get(16)); // link 3
        assertEqualEvent(LinkEnterEvent.class, 30601, allEvents.get(17)); // link 4
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30720, allEvents.get(18));
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30720, allEvents.get(19));
        assertEqualEvent(VehicleLeavesTrafficEvent.class, 30720, allEvents.get(20));
        assertEqualEvent(PersonLeavesVehicleEvent.class, 30720, allEvents.get(21));
        assertEqualEvent(PersonArrivalEvent.class, 30720, allEvents.get(22));
    }

	/**
	* During tests, the travel time between two stops was not correctly calculated when the two stops were connected with a single direct link, and each of the stops is on a zero-length loop link.
	*/
	@Test
	void testEvents_withoutPassengers_withLinks_FromToLoopLink() {
        TestFixture f = new TestFixture();
        f.addLoopyRoute(true);
        f.sbbConfig.setCreateLinkEventsInterval(1);

        EventsManager eventsManager = EventsUtils.createEventsManager(f.config);
        QSim qSim = new QSimBuilder(f.config) //
                .addQSimModule(new ActivityEngineModule())
                .addQSimModule(new SBBTransitEngineQSimModule())
                .addQSimModule(new TestQSimModule(f.config))
                .configureQSimComponents(configurator -> {
                    configurator.addNamedComponent(ActivityEngineModule.COMPONENT_NAME);
                })
                .configureQSimComponents(new SBBTransitEngineQSimModule())
                .build(f.scenario, eventsManager);

        EventsCollector collector = new EventsCollector();
        eventsManager.addHandler(collector);
        qSim.run();
        List<Event> allEvents = collector.getEvents();

        for (Event event : allEvents) {
            System.out.println(event.toString());
        }

        Assertions.assertEquals(23, allEvents.size(), "wrong number of events.");
        assertEqualEvent(TransitDriverStartsEvent.class, 30000, allEvents.get(0));
        assertEqualEvent(PersonDepartureEvent.class, 30000, allEvents.get(1));
        assertEqualEvent(PersonEntersVehicleEvent.class, 30000, allEvents.get(2));
        assertEqualEvent(VehicleEntersTrafficEvent.class, 30000, allEvents.get(3)); // link 1
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30000, allEvents.get(4)); // stop A
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30000, allEvents.get(5)); // stop A
        assertEqualEvent(LinkLeaveEvent.class, 30001, allEvents.get(6)); // link 1
        assertEqualEvent(LinkEnterEvent.class, 30001, allEvents.get(7)); // link -2, loop link
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30100, allEvents.get(8)); // stop BLoop
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30120, allEvents.get(9)); // stop BLoop
        assertEqualEvent(LinkLeaveEvent.class, 30121, allEvents.get(10)); // link -2
        assertEqualEvent(LinkEnterEvent.class, 30121, allEvents.get(11)); // link 2
        assertEqualEvent(LinkLeaveEvent.class, 30300, allEvents.get(12)); // link 2
        assertEqualEvent(LinkEnterEvent.class, 30300, allEvents.get(13)); // link -3, loop link
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30300, allEvents.get(14)); // stop CLoop
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30300, allEvents.get(15)); // stop CLoop
        assertEqualEvent(LinkLeaveEvent.class, 30301, allEvents.get(16)); // link -3
        assertEqualEvent(LinkEnterEvent.class, 30301, allEvents.get(17)); // link 3
        assertEqualEvent(VehicleArrivesAtFacilityEvent.class, 30570, allEvents.get(18)); // stop D
        assertEqualEvent(VehicleDepartsAtFacilityEvent.class, 30570, allEvents.get(19));
        assertEqualEvent(VehicleLeavesTrafficEvent.class, 30570, allEvents.get(20)); // link 3
        assertEqualEvent(PersonLeavesVehicleEvent.class, 30570, allEvents.get(21));
        assertEqualEvent(PersonArrivalEvent.class, 30570, allEvents.get(22));
    }

	@Test
	void testMisconfiguration() {
        TestFixture f = new TestFixture();
        f.config.transit().setTransitModes(CollectionUtils.stringToSet("pt,train,bus"));
        f.sbbConfig.setDeterministicServiceModes(CollectionUtils.stringToSet("train,tram,ship"));

        EventsManager eventsManager = EventsUtils.createEventsManager(f.config);
        QSim qSim = new QSimBuilder(f.config) //
                .addQSimModule(new ActivityEngineModule())
                .addQSimModule(new SBBTransitEngineQSimModule())
                .addQSimModule(new TestQSimModule(f.config))
                .configureQSimComponents(new SBBTransitEngineQSimModule())
                .configureQSimComponents(configurator -> {
                    configurator.addNamedComponent(ActivityEngineModule.COMPONENT_NAME);
                })
                .build(f.scenario, eventsManager);

        try {
            qSim.run();
            Assertions.fail("Expected a RuntimeException due misconfiguration, but got none.");
        } catch (RuntimeException e) {
            log.info("Caught expected exception, all is fine.", e);
        }

    }

	@Test
	void testCreateEventsInterval() {
        TestFixture f = new TestFixture();
        f.sbbConfig.setCreateLinkEventsInterval(3);

        EventsManager eventsManager = EventsUtils.createEventsManager(f.config);
        TestQSimModule testModule = new TestQSimModule(f.config);

        QSimBuilder builder = new QSimBuilder(f.config) //
                .addQSimModule(new ActivityEngineModule())
                .addQSimModule(new SBBTransitEngineQSimModule())
                .addQSimModule(testModule)
                .configureQSimComponents(new SBBTransitEngineQSimModule())
                .configureQSimComponents(configurator -> {
                    configurator.addNamedComponent(ActivityEngineModule.COMPONENT_NAME);
                });

        for (int iteration = 0; iteration <= 10; iteration++) {
            testModule.context.setIteration(iteration);
            QSim qSim = builder.build(f.scenario, eventsManager);

            EventsCollector collector = new EventsCollector();
            eventsManager.addHandler(collector);
            qSim.run();

            int expectedEventsCount = 15;
            if (iteration == 0 || iteration == 3 || iteration == 6 || iteration == 9) {
                expectedEventsCount = 23;
            }
            Assertions.assertEquals(expectedEventsCount, collector.getEvents().size(), "wrong number of events in iteration " + iteration);
        }
    }
}
