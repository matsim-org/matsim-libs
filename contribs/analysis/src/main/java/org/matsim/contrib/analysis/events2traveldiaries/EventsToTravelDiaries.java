package org.matsim.contrib.analysis.events2traveldiaries;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.analysis.events2traveldiaries.travelcomponents.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * @author pieterfourie, sergioo
 *         <p>
 *         Converts events into journeys, trips/stages, transfers and activities
 *         tables. Originally designed for transit scenarios with full transit
 *         simulation, but should work with most teleported modes
 *         </p>
 */

public class EventsToTravelDiaries implements
        TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler,
        PersonLeavesVehicleEventHandler, PersonDepartureEventHandler,
        PersonArrivalEventHandler, ActivityStartEventHandler,
        ActivityEndEventHandler, PersonStuckEventHandler,
        LinkEnterEventHandler, LinkLeaveEventHandler,
        TeleportationArrivalEventHandler, VehicleArrivesAtFacilityEventHandler {

    private final Network network;
    private double walkSpeed;
    // Attributes
    private Map<Id, TravellerChain> chains = new HashMap<>();
    private Map<Id, Coord> locations = new HashMap<>();
    private Map<Id, PTVehicle> ptVehicles = new HashMap<>();
    private HashSet<Id> transitDriverIds = new HashSet<>();
    private HashMap<Id, Id> driverIdFromVehicleId = new HashMap<>();
    private int stuck = 0;
    private TransitSchedule transitSchedule;
    private boolean isTransitScenario = false;
    private String diagnosticString = "39669_2";


    public EventsToTravelDiaries(TransitSchedule transitSchedule,
                                 Network network, Config config) {

        this.network = network;
        this.walkSpeed = new TransitRouterConfig(config).getBeelineWalkSpeed();
        this.transitSchedule = transitSchedule;
        this.isTransitScenario = true;
    }

    public EventsToTravelDiaries(Scenario scenario) {
        // yy seems a bit overkill to generate a config just to obtain one scalar value. kai, may'15

        this.network = scenario.getNetwork();
        isTransitScenario = scenario.getConfig().scenario().isUseTransit() && scenario.getConfig().scenario().isUseVehicles();
        if (isTransitScenario) {
            this.transitSchedule = scenario.getTransitSchedule();
            this.walkSpeed = new TransitRouterConfig(scenario.getConfig()).getBeelineWalkSpeed();
        }

    }

    public static void runEventsProcessing(Properties properties) {
        boolean isTransit = false;
        ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
                .createScenario(ConfigUtils.loadConfig(properties.get("configFile").toString()));
        scenario.getConfig().scenario().setUseTransit(true);
        if (!properties.get("transitScheduleFile").toString().equals("")) {
            new TransitScheduleReader(scenario)
                    .readFile(properties.get("transitScheduleFile").toString());
            isTransit = true;
        }
        new MatsimNetworkReader(scenario).readFile(properties.get("networkFile").toString());

        EventsManager eventsManager = EventsUtils.createEventsManager();
        EventsToTravelDiaries test;
        // if(linkTrafficComponent.isSelected()){
        // test = new EventsToPlanElements(
        // scenario.getTransitSchedule(), scenario.getNetwork(),
        // scenario.getConfig(),new File(postgresPropertiesComponent.getText())
        // ,tableSuffixComponent.getText());
        // }else{
        if (isTransit) {
            test = new EventsToTravelDiaries(
                    scenario.getTransitSchedule(), scenario.getNetwork(),
                    scenario.getConfig());

        } else {
            test = new EventsToTravelDiaries(scenario);
        }
        // }
        eventsManager.addHandler(test);
        new MatsimEventsReader(eventsManager).readFile(properties.get("eventsFile").toString());

        try {

            test.writeSimulationResultsToTabSeparated(properties.get("outputPath").toString(),
                    properties.get("tableSuffix").toString());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("Number of stuck vehicles/passengers: "
                + test.getStuck());

    }

    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(args[0])));
        EventsToTravelDiaries.runEventsProcessing(properties);
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        try {
            if (isTransitScenario) {
                if (transitDriverIds.contains(event.getPersonId()))
                    return;
            }
            TravellerChain chain = chains.get(event.getPersonId());
            locations.put(event.getPersonId(),
                    network.getLinks().get(event.getLinkId()).getCoord());
            if (chain == null) {
                chain = new TravellerChain();
                chains.put(event.getPersonId(), chain);
                Activity act = chain.addActivity();
                act.setCoord(network.getLinks().get(event.getLinkId())
                        .getCoord());
                act.setEndTime(event.getTime());
                act.setFacility(event.getFacilityId());
                act.setStartTime(0.0);
                act.setType(event.getActType());

            } else if (!event.getActType().equals(
                    PtConstants.TRANSIT_ACTIVITY_TYPE)) {
                Activity act = chain.getActs().getLast();
                act.setEndTime(event.getTime());
            }
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        try {
            if (isTransitScenario) {
                if (transitDriverIds.contains(event.getPersonId()))
                    return;
            }
            TravellerChain chain = chains.get(event.getPersonId());
            boolean beforeInPT = chain.isInPT();
            if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
                chain.setInPT(true);

            } else {
                chain.setInPT(false);
                chain.traveling = false;
                Activity act = chain.addActivity();
                act.setCoord(network.getLinks().get(event.getLinkId())
                        .getCoord());
                act.setFacility(event.getFacilityId());
                act.setStartTime(event.getTime());
                act.setType(event.getActType());
                // end the preceding journey
                Journey journey = chain.getJourneys().getLast();
                journey.setDest(act.getCoord());
                journey.setEndTime(event.getTime());
                journey.setToAct(act);
                if (beforeInPT)
                    journey.getWalks().getLast().setEgressWalk(true);
            }
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
            ;
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        try {
            if (isTransitScenario) {
                if (transitDriverIds.contains(event.getPersonId()))
                    return;
            }
            TravellerChain chain = chains.get(event.getPersonId());
            switch (event.getLegMode()) {
                case "walk":
                case "transit_walk": {
                    Journey journey = chain.getJourneys().getLast();
                    Walk walk = journey.getWalks().getLast();
                    walk.setDest(network.getLinks().get(event.getLinkId())
                            .getCoord());
                    walk.setEndTime(event.getTime());
                    walk.setDistance(walk.getDuration() * walkSpeed);
                    break;
                }
                case TransportMode.car: {
                    Journey journey = chain.getJourneys().getLast();
                    journey.setDest(network.getLinks().get(event.getLinkId())
                            .getCoord());
                    journey.setEndTime(event.getTime());
                    Trip trip = journey.getTrips().getLast();
                    trip.setDistance(journey.getDistance());
                    trip.setEndTime(event.getTime());
                    chain.inCar = false;
                    break;
                }
                case "pt":
                    if (isTransitScenario) {
                        Journey journey = chain.getJourneys().getLast();
                        Trip trip = journey.getTrips().getLast();
                        trip.setDest(network.getLinks().get(event.getLinkId())
                                .getCoord());
                        trip.setEndTime(event.getTime());
                        journey.setPossibleTransfer(new Transfer());
                        journey.getPossibleTransfer().setStartTime(event.getTime());
                        journey.getPossibleTransfer().setFromTrip(trip);
                    } else {
                        Journey journey = chain.getJourneys().getLast();
                        journey.setEndTime(event.getTime());
                        journey.setDest(network.getLinks().get(event.getLinkId())
                                .getCoord());
                        journey.setEndTime(event.getTime());
                    }
                    break;
                default:
                    Journey journey = chain.getJourneys().getLast();
                    journey.setEndTime(event.getTime());
                    journey.setDest(network.getLinks().get(event.getLinkId())
                            .getCoord());
                    journey.setEndTime(event.getTime());
                    break;
            }
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        try {
            if (transitDriverIds.contains(event.getPersonId()))
                return;
            TravellerChain chain = chains.get(event.getPersonId());
            Journey journey;
            Trip trip;
            switch (event.getLegMode()) {
                case TransportMode.walk:
                    //fall through to the next
                case TransportMode.transit_walk:
                    if (!chain.traveling) {
                        chain.traveling = true;
                        journey = chain.addJourney();
                        journey.setOrig(network.getLinks().get(event.getLinkId())
                                .getCoord());
                        journey.setFromAct(chain.getActs().getLast());
                        journey.setStartTime(event.getTime());
                        Walk walk = journey.addWalk();
                        walk.setAccessWalk(true);
                        walk.setStartTime(event.getTime());
                        walk.setOrig(journey.getOrig());
                    } else {
                        journey = chain.getJourneys().getLast();
                        Walk walk = journey.addWalk();
                        walk.setStartTime(event.getTime());
                        walk.setOrig(network.getLinks().get(event.getLinkId())
                                .getCoord());
                        journey.getPossibleTransfer().getWalks().add(walk);
                    }
                    break;
                case TransportMode.car:
                    chain.inCar = true;
                    journey = chain.addJourney();
                    journey.setCarJourney(true);
                    journey.setOrig(network.getLinks().get(event.getLinkId())
                            .getCoord());
                    journey.setFromAct(chain.getActs().getLast());
                    journey.setStartTime(event.getTime());
                    trip = journey.addTrip();
                    trip.setMode("car");
                    trip.setStartTime(event.getTime());
                    break;
                case TransportMode.pt:
                    if (isTransitScenario) {
                        // person waits till they enter the vehicle
                        journey = chain.getJourneys().getLast();
                        Wait wait = journey.addWait();
                        if (journey.getWaits().size() == 1)
                            wait.setAccessWait(true);
                        wait.setStartTime(event.getTime());
                        wait.setCoord(network.getLinks().get(event.getLinkId())
                                .getCoord());
                        if (!wait.isAccessWait()) {
                            journey.getPossibleTransfer().getWaits().add(wait);
                        }
                    } else {
                        journey = chain.addJourney();
                        journey.setTeleportJourney(true);
                        journey.setOrig(network.getLinks().get(event.getLinkId())
                                .getCoord());
                        journey.setFromAct(chain.getActs().getLast());
                        journey.setStartTime(event.getTime());
                        journey.setMainmode(event.getLegMode());
                        trip = journey.addTrip();
                        trip.setMode(event.getLegMode());
                        trip.setStartTime(event.getTime());
                    }
                    break;
                default:
                    journey = chain.addJourney();
                    journey.setTeleportJourney(true);
                    journey.setOrig(network.getLinks().get(event.getLinkId())
                            .getCoord());
                    journey.setFromAct(chain.getActs().getLast());
                    journey.setStartTime(event.getTime());
                    journey.setMainmode(event.getLegMode());
                    trip = journey.addTrip();
                    trip.setMode(event.getLegMode());
                    trip.setStartTime(event.getTime());
                    break;

            }
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }

    @Override
    public void handleEvent(PersonStuckEvent event) {
        try {
            if (!transitDriverIds.contains(event.getPersonId())) {
                TravellerChain chain = chains.get(event.getPersonId());
                setStuck(getStuck() + 1);
                if (chain.getJourneys().size() > 0)
                    chain.getJourneys().removeLast();
            }
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        try {
            if (transitDriverIds.contains(event.getPersonId()))
                return;
            if (ptVehicles.keySet().contains(event.getVehicleId())) {
                TravellerChain chain = chains.get(event.getPersonId());
                Journey journey = chain.getJourneys().getLast();
                // first, handle the end of the wait
                journey.getWaits().getLast().setEndTime(event.getTime());
                // now, create a new trip
                ptVehicles.get(event.getVehicleId()).addPassenger(
                        event.getPersonId());
                Trip trip = journey.addTrip();
                PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
                trip.setLine(vehicle.transitLineId);
                trip.setMode(transitSchedule.getTransitLines()
                        .get(vehicle.transitLineId).getRoutes()
                        .get(vehicle.transitRouteId).getTransportMode());
                trip.setBoardingStop(vehicle.lastStop);
                trip.setOrig(journey.getWaits().getLast().getCoord());
                trip.setRoute(ptVehicles.get(event.getVehicleId()).transitRouteId);
                trip.setStartTime(event.getTime());
                // check for the end of a transfer
                if (journey.getPossibleTransfer() != null) {
                    journey.getPossibleTransfer().setToTrip(trip);
                    journey.getPossibleTransfer().setEndTime(event.getTime());
                    journey.addTransfer(journey.getPossibleTransfer());
                    journey.setPossibleTransfer(null);
                }
            } else {
                // add the person to the map that keeps track of who drives what
                // vehicle
                driverIdFromVehicleId.put(event.getVehicleId(),
                        event.getPersonId());
            }
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        if (transitDriverIds.contains(event.getPersonId()))
            return;
        try {
            if (ptVehicles.keySet().contains(event.getVehicleId())) {
                TravellerChain chain = chains.get(event.getPersonId());
                chain.traveledVehicle = true;
                PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
                double stageDistance = vehicle.removePassenger(event
                        .getPersonId());
                Trip trip = chain.getJourneys().getLast().getTrips().getLast();
                trip.setDistance(stageDistance);
                trip.setAlightingStop(vehicle.lastStop);
            } else {
                driverIdFromVehicleId.remove(event.getVehicleId());
            }

        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        try {
            if (ptVehicles.keySet().contains(event.getVehicleId())) {
                PTVehicle ptVehicle = ptVehicles.get(event.getVehicleId());
                ptVehicle.in = true;
                ptVehicle.setLinkEnterTime(event.getTime());
            } else {
                chains.get(driverIdFromVehicleId.get(event.getVehicleId()))
                        .setLinkEnterTime(event.getTime());
            }

        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }

    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        try {
            if (ptVehicles.keySet().contains(event.getVehicleId())) {
                PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
                if (vehicle.in)
                    vehicle.in = false;
                vehicle.incDistance(network.getLinks().get(event.getLinkId())
                        .getLength());

            } else {
                TravellerChain chain = chains.get(driverIdFromVehicleId
                        .get(event.getVehicleId()));
                if (chain.inCar) {
                    Journey journey = chain.getJourneys().getLast();
                    journey.incrementCarDistance(network.getLinks()
                            .get(event.getLinkId()).getLength());

                }
            }
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }

    @Override
    public void handleEvent(TransitDriverStartsEvent event) {
        try {
            ptVehicles.put(
                    event.getVehicleId(),
                    new PTVehicle(event.getTransitLineId(), event
                            .getTransitRouteId()));
            transitDriverIds.add(event.getDriverId());
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
            ;
        }
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
        try {
            if (transitDriverIds.contains(event.getPersonId()))
                return;
            TravellerChain chain = chains.get(event.getPersonId());
            if (chain.traveledVehicle)
                chain.traveledVehicle = false;
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }

    @Override
    public void handleEvent(VehicleArrivesAtFacilityEvent event) {
        try {
            ptVehicles.get(event.getVehicleId()).lastStop = event
                    .getFacilityId();
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }

    // Methods
    @Override
    public void reset(int iteration) {
        chains = new HashMap<>();
        locations = new HashMap<>();
        ptVehicles = new HashMap<>();
        transitDriverIds = new HashSet<>();
        driverIdFromVehicleId = new HashMap<>();
    }

    public void writeSimulationResultsToTabSeparated(String path, String appendage) throws IOException {
        String actTableName;
        String journeyTableName;
        String transferTableName;
        String tripTableName;
        if (appendage.matches("[a-zA-Z0-9]*[_]*")) {
            actTableName = appendage + "matsim_activities.txt";
            journeyTableName = appendage + "matsim_journeys.txt";
            transferTableName = appendage + "matsim_transfers.txt";
            tripTableName = appendage + "matsim_trips.txt";
        } else {
            if (appendage.matches("[a-zA-Z0-9]*"))
                appendage = "_" + appendage;
            actTableName = "matsim_activities" + appendage + ".txt";
            journeyTableName = "matsim_journeys" + appendage + ".txt";
            transferTableName = "matsim_transfers" + appendage + ".txt";
            tripTableName = "matsim_trips" + appendage + ".txt";
        }
        BufferedWriter activityWriter =
                IOUtils.getBufferedWriter(path + "/" + actTableName);

        activityWriter.write("activity_id\tperson_id\tfacility_id\ttype\t" +
                "start_time\tend_time\tx\ty\tsample_selector\n");

        BufferedWriter journeyWriter =
                IOUtils.getBufferedWriter(path + "/" + journeyTableName);
        journeyWriter.write("journey_id\tperson_id\tstart_time\t" +
                "end_time\tdistance\tmain_mode\tfrom_act\tto_act\t" +
                "in_vehicle_distance\tin_vehicle_time\t" +
                "access_walk_distance\taccess_walk_time\taccess_wait_time\t" +
                "first_boarding_stop\tegress_walk_distance\t" +
                "egress_walk_time\tlast_alighting_stop\t" +
                "transfer_walk_distance\ttransfer_walk_time\t" +
                "transfer_wait_time\tsample_selector\n");

        BufferedWriter tripWriter =
                IOUtils.getBufferedWriter(path + "/" + tripTableName);
        tripWriter.write("trip_id\tjourney_id\tstart_time\tend_time\t" +
                "distance\tmode\tline\troute\tboarding_stop\t" +
                "alighting_stop\tsample_selector\n");

        BufferedWriter transferWriter =
                IOUtils.getBufferedWriter(path + "/" + transferTableName);
        transferWriter.write("transfer_id\tjourney_id\tstart_time\t" +
                "end_time\tfrom_trip\tto_trip\twalk_distance\t" +
                "walk_time\twait_time\tsample_selector\n");

        //read a static field that increments with every inheriting object constructed
        Counter counter = new Counter("Output lines written: ");
        for (Entry<Id, TravellerChain> entry : chains.entrySet()) {
            String pax_id = entry.getKey().toString();
            TravellerChain chain = entry.getValue();
            for (Activity act : chain.getActs()) {
                try {
                    activityWriter.write(String.format(
                            "%d\t%s\t%s\t%s\t%d\t%d\t%f\t%f\t%f\n",
                            act.getElementId(), pax_id,
                            act.getFacility(), act.getType(),
                            (int) act.getStartTime(),
                            (int) act.getEndTime(),
                            act.getCoord().getX(),
                            act.getCoord().getY(),
                            MatsimRandom.getRandom().nextDouble()
                    ));
                } catch (Exception e) {
                    System.out.println("Couldn't print activity chain!");
                }
            }
            for (Journey journey : chain.getJourneys()) {
                try {
                    journeyWriter.write(String.format(
                            "%d\t%s\t%d\t%d\t%.3f\t%s\t%d\t%d\t%.3f\t%d\t%.3f\t%d\t%d\t%s\t%.3f\t%d\t%s\t%.3f\t%d\t%d\t%f\n",
                            journey.getElementId(),
                            pax_id,
                            (int) journey.getStartTime(),
                            (int) journey.getEndTime(),
                            journey.getDistance(),
                            journey.getMainMode(),
                            journey.getFromAct().getElementId(),
                            journey.getToAct().getElementId(),
                            journey.getInVehDistance(),
                            (int) journey.getInVehTime(),
                            journey.getAccessWalkDistance(),
                            (int) journey.getAccessWalkTime(),
                            (int) journey.getAccessWaitTime(),
                            journey.getFirstBoardingStop(),
                            journey.getEgressWalkDistance(),
                            (int) journey.getEgressWalkTime(),
                            journey.getLastAlightingStop(),
                            journey.getTransferWalkDistance(),
                            (int) journey.getTransferWalkTime(),
                            (int) journey.getTransferWaitTime(),
                            MatsimRandom.getRandom().nextDouble()
                    ));
                    counter.incCounter();

                    if (!(journey.isCarJourney() || journey.isTeleportJourney())) {
                        for (Trip trip : journey.getTrips()) {
                            tripWriter.write(String.format(
                                    "%d\t%d\t%d\t%d\t%.3f\t%s\t%s\t%s\t%s\t%s\t%f\n",
                                    trip.getElementId(),
                                    journey.getElementId(),
                                    (int) trip.getStartTime(),
                                    (int) trip.getEndTime(),
                                    trip.getDistance(),
                                    trip.getMode(), trip.getLine(),
                                    trip.getRoute(), trip.getBoardingStop(),
                                    trip.getAlightingStop(),
                                    MatsimRandom.getRandom().nextDouble()
                            ));
                            counter.incCounter();
                        }
                        for (Transfer transfer : journey.getTransfers()) {
                            transferWriter.write(String.format(
                                    "%d\t%d\t%d\t%d\t%d\t%d\t%.3f\t%d\t%d\t%f\n",
                                    transfer.getElementId(),
                                    journey.getElementId(),
                                    (int) transfer.getStartTime(),
                                    (int) transfer.getEndTime(),
                                    transfer.getFromTrip()
                                            .getElementId(),
                                    transfer.getToTrip()
                                            .getElementId(),

                                    transfer.getWalkDistance(),
                                    (int) transfer.getWalkTime(),
                                    (int) transfer.getWaitTime(),
                                    MatsimRandom.getRandom().nextDouble()

                            ));
                            counter.incCounter();
                        }
                    } else {
                        for (Trip trip : journey.getTrips()) {

                            tripWriter.write(String.format(
                                    "%d\t%d\t%d\t%d\t%.3f\t%s\t%s\t%s\t%s\t%s\t%f\n",
                                    trip.getElementId(),
                                    journey.getElementId(),
                                    (int) trip.getStartTime(),
                                    (int) trip.getEndTime(),
                                    journey.isTeleportJourney() ? 0.000 :
                                            trip.getDistance(),
                                    trip.getMode(), "", "", "", "",
                                    MatsimRandom.getRandom().nextDouble()

                            ));
                            counter.incCounter();
                        }
                    }
                } catch (NullPointerException e) {
                    setStuck(getStuck() + 1);
                }
            }

        }

        activityWriter.close();
        journeyWriter.close();
        tripWriter.close();
        transferWriter.close();
        counter.printCounter();
    }

    public int getStuck() {
        return stuck;
    }

    void setStuck(int stuck) {
        this.stuck = stuck;
    }

    // Private classes
    private class PTVehicle {

        // Attributes
        private final Id transitLineId;
        private final Id transitRouteId;
        private final Map<Id, Double> passengers = new HashMap<>();
        boolean in = false;
        Id lastStop;
        private double distance;
        private double linkEnterTime = 0.0;

        // Constructors
        public PTVehicle(Id transitLineId, Id transitRouteId) {
            this.transitLineId = transitLineId;
            this.transitRouteId = transitRouteId;
        }

        // Methods
        public void incDistance(double linkDistance) {
            distance += linkDistance;
        }

        public void addPassenger(Id passengerId) {
            passengers.put(passengerId, distance);
        }

        public double removePassenger(Id passengerId) {
            return distance - passengers.remove(passengerId);
        }

        public double getLinkEnterTime() {
            return linkEnterTime;
        }

        public void setLinkEnterTime(double linkEnterTime) {
            this.linkEnterTime = linkEnterTime;
        }

    }

}
