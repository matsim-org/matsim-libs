package org.matsim.codeexamples.network;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * This class shows how to generate Network Change Events based on the speeds measured in an existing simulation.
 * These change Events may be useful to simulate only some of the traffic of a simulation (e.g., fleets of taxis)
 */


public class CreateNetworkChangeEventsFromExistingSimulation {
    private static final int ENDTIME = 30 * 3600;
    private static final int TIMESTEP = 15 * 60;
    private static final String NETWORKFILE = "";
    private static final String SIMULATION_EVENTS_FILE = "";
    private static final String CHANGE_EVENTS_FILE = "";
    private static final double MINIMUMFREESPEED = 3;
    private Scenario scenario;
    private TravelTimeCalculator tcc;
    private List<NetworkChangeEvent> networkChangeEvents;

    public CreateNetworkChangeEventsFromExistingSimulation() {
        this.networkChangeEvents = new ArrayList<>();

    }

    public static void main(String[] args) {
        CreateNetworkChangeEventsFromExistingSimulation ncg = new CreateNetworkChangeEventsFromExistingSimulation();

        ncg.run();

    }

    private void run() {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORKFILE);
        tcc = readEvents();
        createNetworkChangeEvents(scenario.getNetwork(), tcc);
        new NetworkChangeEventsWriter().write(CHANGE_EVENTS_FILE, networkChangeEvents);
    }

    public void createNetworkChangeEvents(Network network, TravelTimeCalculator tcc2) {
        for (Link l : network.getLinks().values()) {
            if (l.getId().toString().startsWith("pt")) continue;

            double length = l.getLength();
            double previousTravelTime = l.getLength() / l.getFreespeed();

            for (double time = 0; time < ENDTIME; time = time + TIMESTEP) {

                double newTravelTime = tcc2.getLinkTravelTimes().getLinkTravelTime(l, time, null, null);
                if (newTravelTime != previousTravelTime) {
                    NetworkChangeEvent nce = new NetworkChangeEvent(time);
                    nce.addLink(l);
                    double newFreespeed = length / newTravelTime;
                    if (newFreespeed < MINIMUMFREESPEED) newFreespeed = MINIMUMFREESPEED;
                    ChangeValue freespeedChange = new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, newFreespeed);
                    nce.setFreespeedChange(freespeedChange);


                    this.networkChangeEvents.add(nce);
                    previousTravelTime = newTravelTime;
                }
            }
        }
    }


    private TravelTimeCalculator readEvents() {
        EventsManager manager = EventsUtils.createEventsManager();

        TravelTimeCalculatorConfigGroup ttccg = new TravelTimeCalculatorConfigGroup();
        TravelTimeCalculator tc = new TravelTimeCalculator(scenario.getNetwork(), ttccg);
        manager.addHandler(tc);
        new MatsimEventsReader(manager).readFile(SIMULATION_EVENTS_FILE);
        return tc;
    }

    public List<NetworkChangeEvent> getNetworkChangeEvents() {
        return networkChangeEvents;
    }


}
