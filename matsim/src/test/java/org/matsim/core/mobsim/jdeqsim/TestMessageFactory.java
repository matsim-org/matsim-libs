package org.matsim.core.mobsim.jdeqsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestCase;

public class TestMessageFactory extends MatsimTestCase {
    EventsManager eventsManager = EventsUtils.createEventsManager();
    MessageFactory messageFactory = new MessageFactory(eventsManager);


    // check if gc turned on
    public void testMessageFactory1() {
        messageFactory.GC_ALL_MESSAGES();
        JDEQSimConfigGroup.setGC_MESSAGES(true);
        messageFactory.disposeEndLegMessage(new EndLegMessage(null, null, eventsManager));
        messageFactory.disposeEnterRoadMessage(new EnterRoadMessage(null, null, eventsManager));
        messageFactory.disposeStartingLegMessage(new StartingLegMessage(null, null, eventsManager));
        messageFactory.disposeLeaveRoadMessage(new LeaveRoadMessage(null, null, eventsManager));
        messageFactory.disposeEndRoadMessage(new EndRoadMessage(null, null, eventsManager));
        messageFactory.disposeDeadlockPreventionMessage(new DeadlockPreventionMessage(null, null, eventsManager));

        assertEquals(0, messageFactory.getEndLegMessageQueue().size());
        assertEquals(0, messageFactory.getEnterRoadMessageQueue().size());
        assertEquals(0, messageFactory.getStartingLegMessageQueue().size());
        assertEquals(0, messageFactory.getLeaveRoadMessageQueue().size());
        assertEquals(0, messageFactory.getEndRoadMessageQueue().size());
        assertEquals(0, messageFactory.getEndLegMessageQueue().size());
    }

    // check when gc turned off
    public void testMessageFactory2() {
        messageFactory.GC_ALL_MESSAGES();
        JDEQSimConfigGroup.setGC_MESSAGES(false);
        messageFactory.disposeEndLegMessage(new EndLegMessage(null, null, eventsManager));
        messageFactory.disposeEnterRoadMessage(new EnterRoadMessage(null, null, eventsManager));
        messageFactory.disposeStartingLegMessage(new StartingLegMessage(null, null, eventsManager));
        messageFactory.disposeLeaveRoadMessage(new LeaveRoadMessage(null, null, eventsManager));
        messageFactory.disposeEndRoadMessage(new EndRoadMessage(null, null, eventsManager));
        messageFactory.disposeDeadlockPreventionMessage(new DeadlockPreventionMessage(null, null, eventsManager));

        assertEquals(1, messageFactory.getEndLegMessageQueue().size());
        assertEquals(1, messageFactory.getEnterRoadMessageQueue().size());
        assertEquals(1, messageFactory.getStartingLegMessageQueue().size());
        assertEquals(1, messageFactory.getLeaveRoadMessageQueue().size());
        assertEquals(1, messageFactory.getEndRoadMessageQueue().size());
        assertEquals(1, messageFactory.getEndLegMessageQueue().size());
    }

    // check check use of Message factory
    public void testMessageFactory3() {
        messageFactory.GC_ALL_MESSAGES();
        JDEQSimConfigGroup.setGC_MESSAGES(false);
        messageFactory.disposeEndLegMessage(new EndLegMessage(null, null, eventsManager));
        messageFactory.disposeEnterRoadMessage(new EnterRoadMessage(null, null, eventsManager));
        messageFactory.disposeStartingLegMessage(new StartingLegMessage(null, null, eventsManager));
        messageFactory.disposeLeaveRoadMessage(new LeaveRoadMessage(null, null, eventsManager));
        messageFactory.disposeEndRoadMessage(new EndRoadMessage(null, null, eventsManager));
        messageFactory.disposeDeadlockPreventionMessage(new DeadlockPreventionMessage(null, null, eventsManager));

        messageFactory.getEndLegMessage(null, null);
        messageFactory.getEnterRoadMessage(null, null);
        messageFactory.getStartingLegMessage(null, null);
        messageFactory.getLeaveRoadMessage(null, null);
        messageFactory.getEndRoadMessage(null, null);
        messageFactory.getDeadlockPreventionMessage(null, null);

        assertEquals(0, messageFactory.getEndLegMessageQueue().size());
        assertEquals(0, messageFactory.getEnterRoadMessageQueue().size());
        assertEquals(0, messageFactory.getStartingLegMessageQueue().size());
        assertEquals(0, messageFactory.getLeaveRoadMessageQueue().size());
        assertEquals(0, messageFactory.getEndRoadMessageQueue().size());
        assertEquals(0, messageFactory.getEndLegMessageQueue().size());
    }

    // check initialization using constructer
    public void testMessageFactory5() {
        messageFactory.GC_ALL_MESSAGES();
        JDEQSimConfigGroup.setGC_MESSAGES(true);
        Scheduler scheduler = new Scheduler(new MessageQueue());
        Person person = PopulationUtils.getFactory().createPerson(Id.create("abc", Person.class));
        Vehicle vehicle = new Vehicle(scheduler, person, PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime, null, messageFactory);

        assertEquals(true, messageFactory.getEndLegMessage(scheduler, vehicle).scheduler == scheduler);
        assertEquals(true, messageFactory.getEnterRoadMessage(scheduler, vehicle).scheduler == scheduler);
        assertEquals(true, messageFactory.getStartingLegMessage(scheduler, vehicle).scheduler == scheduler);
        assertEquals(true, messageFactory.getLeaveRoadMessage(scheduler, vehicle).scheduler == scheduler);
        assertEquals(true, messageFactory.getEndRoadMessage(scheduler, vehicle).scheduler == scheduler);
        assertEquals(true, messageFactory.getDeadlockPreventionMessage(scheduler, vehicle).scheduler == scheduler);

        assertEquals(true, messageFactory.getEndLegMessage(scheduler, vehicle).vehicle == vehicle);
        assertEquals(true, messageFactory.getEnterRoadMessage(scheduler, vehicle).vehicle == vehicle);
        assertEquals(true, messageFactory.getStartingLegMessage(scheduler, vehicle).vehicle == vehicle);
        assertEquals(true, messageFactory.getLeaveRoadMessage(scheduler, vehicle).vehicle == vehicle);
        assertEquals(true, messageFactory.getEndRoadMessage(scheduler, vehicle).vehicle == vehicle);
        assertEquals(true, messageFactory.getDeadlockPreventionMessage(scheduler, vehicle).vehicle == vehicle);
    }

    // check initialization using rest
    public void testMessageFactory6() {
        messageFactory.GC_ALL_MESSAGES();
        JDEQSimConfigGroup.setGC_MESSAGES(false);
        Scheduler scheduler = new Scheduler(new MessageQueue());
        Person person = PopulationUtils.getFactory().createPerson(Id.create("abc", Person.class));
        Vehicle vehicle = new Vehicle(scheduler, person, PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime, null, messageFactory);

        assertEquals(true, messageFactory.getEndLegMessage(scheduler, vehicle).scheduler == scheduler);
        assertEquals(true, messageFactory.getEnterRoadMessage(scheduler, vehicle).scheduler == scheduler);
        assertEquals(true, messageFactory.getStartingLegMessage(scheduler, vehicle).scheduler == scheduler);
        assertEquals(true, messageFactory.getLeaveRoadMessage(scheduler, vehicle).scheduler == scheduler);
        assertEquals(true, messageFactory.getEndRoadMessage(scheduler, vehicle).scheduler == scheduler);
        assertEquals(true, messageFactory.getDeadlockPreventionMessage(scheduler, vehicle).scheduler == scheduler);

        assertEquals(true, messageFactory.getEndLegMessage(scheduler, vehicle).vehicle == vehicle);
        assertEquals(true, messageFactory.getEnterRoadMessage(scheduler, vehicle).vehicle == vehicle);
        assertEquals(true, messageFactory.getStartingLegMessage(scheduler, vehicle).vehicle == vehicle);
        assertEquals(true, messageFactory.getLeaveRoadMessage(scheduler, vehicle).vehicle == vehicle);
        assertEquals(true, messageFactory.getEndRoadMessage(scheduler, vehicle).vehicle == vehicle);
        assertEquals(true, messageFactory.getDeadlockPreventionMessage(scheduler, vehicle).vehicle == vehicle);
    }


}
