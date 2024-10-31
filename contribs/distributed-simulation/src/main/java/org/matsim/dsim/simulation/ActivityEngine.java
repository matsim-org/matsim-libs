package org.matsim.dsim.simulation;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.NextStateHandler;
import org.matsim.api.SimEngine;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.dsim.messages.SimStepMessage;

import java.util.*;

@Log4j2
public class ActivityEngine implements SimEngine {

    @Getter
    private final Queue<SimPersonEntry> agentsAtActivities = new PriorityQueue<>(Comparator.comparingDouble(SimPersonEntry::endTime));
    private final Collection<SimPerson> finishedAgents = new ArrayList<>();
    private final TimeInterpretation timeInterpretation;
    private final EventsManager em;

    // This has to be settable if engines are supposed to be passed to the
    // simulation and the simulation wires up a callback later.
    @Setter
    private NextStateHandler nextStateHandler;

    public ActivityEngine(Collection<SimPerson> persons, TimeInterpretation timeInterpretation, EventsManager em) {
        this.timeInterpretation = timeInterpretation;
        for (SimPerson person : persons) {
            var endTime = timeInterpretation.getActivityEndTime(person.getCurrentActivity(), 0);
            agentsAtActivities.add(new SimPersonEntry(endTime, person));
        }
        this.em = em;
    }

    @Override
    public void accept(SimPerson person, double now) {

        Activity act = person.getCurrentActivity();
        double endTime = timeInterpretation.getActivityEndTime(act, now);

        var actStartEvent = new ActivityStartEvent(now,
                person.getId(),
                act.getLinkId(),
                act.getFacilityId(),
                act.getType(),
                act.getCoord()
        );

        em.processEvent(actStartEvent);

        if (Double.isInfinite(endTime)) {
            finishedAgents.add(person);
        } else if (endTime <= now) {
            var actEndEvent = new ActivityEndEvent(
                    now,
                    person.getId(),
                    act.getLinkId(),
                    act.getFacilityId(),
                    act.getType(),
                    act.getCoord()
            );
            em.processEvent(actEndEvent);
            nextStateHandler.accept(person, now);
        } else {
            agentsAtActivities.add(new SimPersonEntry(endTime, person));
        }
    }

    @Override
    public void process(SimStepMessage stepMessage, double now) {
        // don't do anything. We ar not expecting any messages
    }

    @Override
    public void doSimStep(double now) {

        while (firstPersonReady(now)) {
            var entry = agentsAtActivities.poll();
            var person = entry.person();
            var act = person.getCurrentActivity();

            var actEndEvent = new ActivityEndEvent(
                    now,
                    person.getId(),
                    act.getLinkId(),
                    act.getFacilityId(),
                    act.getType(),
                    act.getCoord()
            );
            em.processEvent(actEndEvent);
            nextStateHandler.accept(entry.person(), now);
        }
    }

    private boolean firstPersonReady(double now) {
        return !agentsAtActivities.isEmpty() && agentsAtActivities.peek().endTime() <= now;
    }

    private record SimPersonEntry(double endTime, SimPerson person) {
    }
}
