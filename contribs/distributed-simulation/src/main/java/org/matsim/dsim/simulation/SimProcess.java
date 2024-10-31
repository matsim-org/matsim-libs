package org.matsim.dsim.simulation;

import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.LP;
import org.matsim.api.SimEngine;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.dsim.messages.SimStepMessageProcessor;
import org.matsim.dsim.simulation.net.NetworkTrafficEngine;
import org.matsim.dsim.messages.SimStepMessage;


import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Log4j2
public class SimProcess implements Steppable, LP, SimStepMessageProcessor {

    // The Qsim has flexible engines. However, Activity, Teleportation and Netsim Engine are treated
    // in a special way. I'll have them as explicit members here, until we need more flexibility.
    private final Collection<? extends SimEngine> engines;
    private final ActivityEngine activityEngine;
    private final TeleportationEngine teleportationEngine;
    private final NetworkTrafficEngine networkTrafficEngine;

    private final SimStepMessaging messaging;
    private final EventsManager em;
    //private final Config config;
    private final Set<String> mainModes;

    private double currentTime;

    SimProcess(SimStepMessaging messaging, ActivityEngine activityEngine, TeleportationEngine teleportationEngine,
               NetworkTrafficEngine networkTrafficEngine, EventsManager em, Config config) {
        this.em = em;
        this.messaging = messaging;
        this.activityEngine = activityEngine;
        this.teleportationEngine = teleportationEngine;
        this.networkTrafficEngine = networkTrafficEngine;
        this.engines = List.of(activityEngine, teleportationEngine, networkTrafficEngine);
        for (var engine : engines) {
            engine.setNextStateHandler(this::acceptForNextState);
        }
        mainModes = new HashSet<>(config.qsim().getMainModes());
        log.info("#{} has {} agents, {} links, and {} nodes",
                messaging.getPart(), activityEngine.getAgentsAtActivities().size(),
                networkTrafficEngine.getSimNetwork().getLinks().size(),
                networkTrafficEngine.getSimNetwork().getNodes().size());
    }

    private void acceptForNextState(SimPerson person, double now) {
        person.advancePlan();
        switch (person.getCurrentState()) {
            case ACTIVITY -> activityEngine.accept(person, now);
            case LEG -> handleDeparture(person, now);
        }
    }

    private void handleDeparture(SimPerson person, double now) {

        if (!person.hasCurrentLeg()) {
            // the original qsim implementation issues a Stuck Event here.
            // We opted for ignoring this case for now
            return;
        }

        var leg = person.getCurrentLeg();
        var mode = leg.getMode();

        em.processEvent(new PersonDepartureEvent(
                now, person.getId(), person.getCurrentRouteElement(),
                leg.getMode(),
                leg.getRoutingMode()
        ));

        // this should be extended if we have more engines, such as pt or drt and others.
        // qsimconfiggroup has a set as main modes. Otherwise, we could maintain our own set
        if (mainModes.contains(mode)) {
            networkTrafficEngine.accept(person, now);
        } else {
            teleportationEngine.accept(person, now);
        }
    }

    @Override
    public void doSimStep(double time) {

        this.currentTime = time;

        for (Steppable engine : engines) {
            engine.doSimStep(time);
        }

        messaging.sendMessages(time);
    }

    @Override
    public void process(SimStepMessage msg) {

        assert msg.getSimstep() <= currentTime : "Message time (%.2f) does not match current time (%.2f)".formatted(msg.getSimstep(), currentTime);

        for (SimEngine engine : engines) {
            engine.process(msg, currentTime);
        }
    }

    @Override
    public IntSet waitForOtherRanks(double time) {
        return messaging.getNeighbors();
    }
}
