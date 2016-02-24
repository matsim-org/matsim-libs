package org.matsim.core.mobsim.qsim.jdeqsimengine;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.jdeqsim.JDEQSimConfigGroup;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.mobsim.jdeqsim.Road;
import org.matsim.core.mobsim.jdeqsim.Vehicle;
import org.matsim.core.mobsim.jdeqsim.util.Timer;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

class JDEQSimEngine implements MobsimEngine, ActivityHandler {

    private final static Logger log = Logger.getLogger(JDEQSimEngine.class);

    private JDEQSimConfigGroup config;
    private Scenario scenario;
    private EventsManager eventsManager;
    private Timer t;
    private SteppableScheduler scheduler;
    private int numberOfAgents = 0;
    private AgentCounter agentCounter;

    public JDEQSimEngine(JDEQSimConfigGroup config, Scenario scenario, EventsManager eventsManager, AgentCounter agentCounter, SteppableScheduler scheduler) {
        this.config = config;
        this.scheduler = scheduler;
        this.scenario = scenario;
        this.eventsManager = eventsManager;
        this.agentCounter = agentCounter;
    }

    @Override
    public void onPrepareSim() {
        new JDEQSimulation(config, scenario, eventsManager); // Initialize JDEQSim static fields
        t = new Timer();
        t.startTimer();

        Road.setAllRoads(new HashMap<Id<Link>, Road>());

        // initialize network
        Road road;
        for (Link link : this.scenario.getNetwork().getLinks().values()) {
            road = new Road(scheduler, link);
            Road.getAllRoads().put(link.getId(), road);
        }

    }

    @Override
    public boolean handleActivity(MobsimAgent agent) {
        // We expect all the agents to appear here at the beginning of the simulation (starting their
        // overnight activity.) That's when we enter them into JDEQSim and never let them out.
        new Vehicle(scheduler, ((HasPerson) agent).getPerson(), scenario.getConfig().plans().getActivityDurationInterpretation());
        numberOfAgents++;
        return true;
    }

    @Override
    public void afterSim() {
        t.endTimer();
        log.info("Time needed for one iteration (only JDEQSimulation part): " + t.getMeasuredTime() + "[ms]");
    }

    @Override
    public void setInternalInterface(InternalInterface internalInterface) {
    }

    @Override
    public void doSimStep(double time) {
        scheduler.doSimStep(time);
        // JDEQSim doesn't track agents going to sleep (they just don't produce a new action).
        // So we ask the scheduler if the queue has run dry, and if so, we let all the agents
        // for which we are responsible go to sleep at once.
        if (scheduler.isFinished()) {
            while (numberOfAgents > 0) {
                agentCounter.decLiving();
                numberOfAgents--;
            }
        }
    }

}
