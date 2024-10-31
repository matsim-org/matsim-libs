package org.matsim.dsim.simulation;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.dsim.messages.PersonMsg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

class ActivityEngineTest {

    @Test
    public void init() {

        var timeInterpretation = new TimeInterpretation(ConfigUtils.createConfig());
        var em = mock(EventsManager.class);
        var persons = createPersons();
        var engine = new ActivityEngine(persons, timeInterpretation, em);

        assertEquals(persons.size(), engine.getAgentsAtActivities().size());
    }

    @Test
    public void wakeupOrder() {

        var timeInterpretation = new TimeInterpretation(ConfigUtils.createConfig());
        var em = mock(EventsManager.class);
        var persons = createPersons();
        var engine = new ActivityEngine(persons, timeInterpretation, em);

        persons.sort(Comparator.comparingDouble(p -> timeInterpretation.getActivityEndTime(p.getCurrentActivity(), 0)));
        var personsIterator = persons.iterator();

        // set next state handler which runs all the assertions
        engine.setNextStateHandler((person, now) -> {
            var expected = personsIterator.next();
            assertEquals(expected.getId(), person.getId());
            var expectedEndTime = timeInterpretation.getActivityEndTime(expected.getCurrentActivity(), 0);
            assertEquals(expectedEndTime, now);
        });

        for (var i = 0; i < 100; i++) {
            engine.doSimStep(i);
        }
        assertEquals(0, engine.getAgentsAtActivities().size());
    }

    @Test
    public void acceptPerson() {
        var timeInterpretation = new TimeInterpretation(ConfigUtils.createConfig());
        var em = mock(EventsManager.class);
        var persons = createPersons();
        var engine = new ActivityEngine(persons, timeInterpretation, em);
        var expected = List.of(
                Tuple.of(Id.createPersonId("some-other-person"), 10),
                Tuple.of(Id.createPersonId("accepted-person"), 15),
                Tuple.of(Id.createPersonId("some-person"), 20)
        ).iterator();

        // test correct order in next state handler. Will be called from doSimStep
        engine.setNextStateHandler((person, now) -> {
            var e = expected.next();
            assertEquals(e.getFirst(), person.getId());
            assertEquals((double) e.getSecond(), now);
        });

        // put a person between the two others
        // set max dur for activity, to make sure the end time is computed based on the
        // passed time.

        engine.accept(new SimPerson(
                PersonMsg.builder()
                        .setId(Id.createPersonId("accepted-person"))
                        .setPlan(List.of(SimpleActivity.builder()
                                .setMaximumDuration(OptionalTime.defined(6))
                                .setEndTime(OptionalTime.undefined())
                                .setType("some")
                                .setLinkId(Id.createLinkId("some-link"))
                                .setCoord(new Coord(0, 0))
                                .build())
                        )
                        .build()), 9);

        for (var i = 0; i < 100; i++) {
            engine.doSimStep(i);
        }

        assertEquals(0, engine.getAgentsAtActivities().size());
    }

    @Test
    public void events() {

        var timeInterpretation = new TimeInterpretation(ConfigUtils.createConfig());
        // pass a mocked events manager and do assertions via doAnswer-api. The assertions
        // will be triggerd during accept and doSimStep calls below
        var em = mock(EventsManager.class);
        doAnswer(call -> {
            var firstArg = call.getArgument(0);
            assertInstanceOf(Event.class, firstArg);
            if (firstArg instanceof ActivityStartEvent ev) {
                assertEquals(9, ev.getTime());
                assertEquals(Id.createPersonId("accepted-person"), ev.getPersonId());
                assertEquals(Id.createLinkId("some-link"), ev.getLinkId());
                assertEquals("some", ev.getActType());
                assertEquals(new Coord(0, 0), ev.getCoord());
            } else if (firstArg instanceof ActivityEndEvent ev) {
                assertEquals(15, ev.getTime());
                assertEquals(Id.createPersonId("accepted-person"), ev.getPersonId());
                assertEquals(Id.createLinkId("some-link"), ev.getLinkId());
                assertEquals("some", ev.getActType());
                assertEquals(new Coord(0, 0), ev.getCoord());
            } else {
                throw new RuntimeException("Only expected ActivityStart and ActivityEndEvent, but received " + firstArg.getClass());
            }
            return null;
        }).when(em).processEvent(any());
        var engine = new ActivityEngine(List.of(), timeInterpretation, em);
        engine.setNextStateHandler((_, _) -> {
            // nothing to do here
        });

        // let a person start and end their activity
        engine.accept(new SimPerson(
                PersonMsg.builder()
                        .setId(Id.createPersonId("accepted-person"))
                        .setPlan(List.of(SimpleActivity.builder()
                                .setMaximumDuration(OptionalTime.defined(6))
                                .setEndTime(OptionalTime.undefined())
                                .setType("some")
                                .setLinkId(Id.createLinkId("some-link"))
                                .setCoord(new Coord(0, 0))
                                .build())
                        ).build()), 9);
        engine.doSimStep(15);

        verify(em, times(2)).processEvent(any());
    }

    private ArrayList<SimPerson> createPersons() {
        var result = new ArrayList<SimPerson>();
        result.add(new SimPerson(PersonMsg.builder()
                .setId(Id.createPersonId("some-person"))
                .setPlan(List.of(SimpleActivity.builder()
                        .setEndTime(OptionalTime.defined(20))
                        .setType("some")
                        .setLinkId(Id.createLinkId("some-link"))
                        .setCoord(new Coord(0, 0))
                        .build()))
                .build()));
        result.add(new SimPerson(PersonMsg.builder()
                .setId(Id.createPersonId("some-other-person"))
                .setPlan(List.of(SimpleActivity.builder()
                        .setEndTime(OptionalTime.defined(10))
                        .setType("other")
                        .setLinkId(Id.createLinkId("some-other-link"))
                        .setCoord(new Coord(0, 0))
                        .build()))
                .build()));
        return result;
    }
}