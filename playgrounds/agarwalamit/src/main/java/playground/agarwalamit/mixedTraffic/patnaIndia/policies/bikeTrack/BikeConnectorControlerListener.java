/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.bikeTrack;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.linkVolume.ModeFilterLinkVolumeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * The idea is to first connect the proposed bike track to regular network by all possible connectors
 * and then start removing one by one connector.
 * <p>
 * Created by amit on 24/11/2016.
 */

class BikeConnectorControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {

    private static final Logger LOG = Logger.getLogger(PatnaBikeTrackConnectionControler.class);
    private static final List<String> allowedModes = Arrays.asList(TransportMode.bike);

    private final int numberOfBikeConnectorsRequired;
    private final int removeConnectorAfterIteration;
    private final int initialStabilizationIterations ;

    private final List<Id<Link>> removedConnectorLinks = new ArrayList<>();
    private final ModeFilterLinkVolumeHandler handler = new ModeFilterLinkVolumeHandler(allowedModes);

    private final List<Id<Link>> bikeConnectorLinks = new ArrayList<>(); // in total 500 links will be added to the list.
    private int totalPossibleConnectors = 0;

    @Inject
    private Scenario scenario;

    @Inject
    private Map<String, TravelTime> travelTimes;

    @Inject
    private Map<String, TravelDisutilityFactory> travelDisutilityFactories;


    private boolean terminateSimulation = false;

    public BikeConnectorControlerListener(final int numberOfConnectorsRequired, final int removeOneConnectorAfterIteration, final int initialStabilizationIterations) {
        this.numberOfBikeConnectorsRequired = numberOfConnectorsRequired;
        this.removeConnectorAfterIteration = removeOneConnectorAfterIteration;
        this.initialStabilizationIterations = initialStabilizationIterations;
    }

    @Override
    public void notifyStartup(StartupEvent event) {

        // get the list of connector links.
        this.bikeConnectorLinks.clear();
        this.bikeConnectorLinks.addAll(
                this.scenario.getNetwork().getLinks().keySet().stream().filter(
                linkId -> linkId.toString().startsWith(PatnaUtils.BIKE_TRACK_CONNECTOR_PREFIX)).collect(
                        Collectors.toList())
        );

        this.totalPossibleConnectors = this.bikeConnectorLinks.size();
//        this.router = new PlanRouter(event.getServices().getTripRouterProvider().get());
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if ( isRemovingBikeConnector(event.getIteration()) ) {

            int numberOfRemainingConnectors = this.totalPossibleConnectors - this.removedConnectorLinks.size();
            if (numberOfRemainingConnectors > this.numberOfBikeConnectorsRequired) {

                Map<Id<Link>, Map<Integer, Double>> link2time2vol = this.handler.getLinkId2TimeSlot2LinkCount();
                Map<Id<Link>, Double> linkId2Count = new HashMap<>();

                for (Id<Link> linkId : this.bikeConnectorLinks) {
                    if (link2time2vol.containsKey(linkId)) {
                        linkId2Count.put(linkId, playground.agarwalamit.utils.MapUtils.doubleValueSum(link2time2vol.get(linkId)));
                    } else {
                        linkId2Count.put(linkId, 0.);
                    }
                }

                // sort based on the values (i.e. link count and not PCU)
                Comparator<Map.Entry<Id<Link>, Double>> byValue = Comparator.comparing(Map.Entry::getValue);

                Id<Link> connector2remove = linkId2Count.entrySet().stream().sorted(byValue).limit(1).iterator().next().getKey();
                this.removedConnectorLinks.add(connector2remove);
                this.bikeConnectorLinks.remove(connector2remove); // necessary, else, in the next round, same link can appear for removal.

                double aboutZeroFreeSpeed = 0.01;
                Link link2Modify = this.scenario.getNetwork().getLinks().get(connector2remove);
                // i think, i can simply change the speed (and lenght, capacity), rather than using network change event because
                // it is permanent change and I can observe this directly in network file.
                link2Modify.setFreespeed(aboutZeroFreeSpeed);
                link2Modify.setLength(100.* link2Modify.getLength());
                link2Modify.setCapacity(0.001* link2Modify.getCapacity());

                reRoutePlan(connector2remove); // only if this connector is present in the route of any leg.

                LOG.info("========================== The free speed on the link " + connector2remove + " is set to " + aboutZeroFreeSpeed + " m/s. The count on this link is "+linkId2Count.get(connector2remove));
                LOG.info("========================== Effectively, number of bike track connectors are " + this.bikeConnectorLinks.size());

                String outNetworkFile = event.getServices().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/" + event.getIteration() + ".network.xml.gz";
                new NetworkWriter(scenario.getNetwork()).write(outNetworkFile);
            } else {
                terminateSimulation = true;
            }
            event.getServices().getEvents().removeHandler(this.handler);
        }
    }

    private boolean isRemovingBikeConnector(int iteration) {
        return iteration >= this.scenario.getConfig().controler().getFirstIteration() + initialStabilizationIterations // let most persons use bike track.
                && iteration % this.removeConnectorAfterIteration == 0;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (isRemovingBikeConnector(event.getIteration())) {
            handler.reset(event.getIteration());
            event.getServices().getEvents().addHandler(handler);
        }
    }

    public boolean isTerminating() {
        if (this.terminateSimulation) {
            LOG.info("========================== Total possible connectors are " + this.totalPossibleConnectors);
            LOG.info("========================== The number of required connectors are " + this.numberOfBikeConnectorsRequired);
            LOG.info("========================== The number of removed connectors are " + this.removedConnectorLinks.size());
            LOG.info("========================== Terminating the solution.");
        }
        return this.terminateSimulation;
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        String outFile = event.getServices().getConfig().controler().getOutputDirectory() + "/removed_connectorLinks.txt";
        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
        this.removedConnectorLinks.forEach(link -> {
            try {
                writer.write(link.toString() + "\n");
            } catch (IOException e) {
                throw new RuntimeException("Data is not written/read. Reason : " + e);
            }
        });
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    private void reRoutePlan(final Id<Link> linkId) {
        PlanAlgorithm router = getRouter();
        for (Person p : this.scenario.getPopulation().getPersons().values()) {
            for (Plan plan : p.getPlans()) {
                boolean needsReRoute = false;
                for (PlanElement pe : plan.getPlanElements()) {
                    if (pe instanceof Leg && ((Leg) pe).getRoute() instanceof NetworkRoute) {
                        NetworkRoute route = (NetworkRoute) ((Leg) pe).getRoute();
                        if (route.getLinkIds().contains(linkId)) {
                            ((Leg) pe).setRoute(null);
                            needsReRoute = true;
                        }
                    }
                }
                if (needsReRoute) router.run(plan);
            }
        }
    }

    private PlanAlgorithm getRouter() {
//        final TravelTime travelTime = new FreeSpeedTravelTimeForBike();
        final TravelTime travelTime = this.travelTimes.get(TransportMode.bike);
        TripRouterFactoryBuilderWithDefaults routerFactory = new TripRouterFactoryBuilderWithDefaults();
        routerFactory.setTravelTime(travelTime);
        routerFactory.setTravelDisutility( this.travelDisutilityFactories.get(TransportMode.bike).createTravelDisutility(travelTime) );
//                new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.bike, scenario.getConfig().planCalcScore()).createTravelDisutility(travelTime));

        final TripRouter tripRouter = routerFactory.build(this.scenario).get();
        return new PlanRouter(tripRouter);
    }
}