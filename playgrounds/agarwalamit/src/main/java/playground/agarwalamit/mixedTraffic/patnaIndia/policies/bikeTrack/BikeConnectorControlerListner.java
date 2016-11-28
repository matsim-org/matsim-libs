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
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
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
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.linkVolume.FilteredLinkVolumeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.NetworkUtils;

/**
 * The idea is to first connect the proposed bike track to regular network by all possible connectors
 * and then start removing one by one connector.
 * <p>
 * Created by amit on 24/11/2016.
 */

public class BikeConnectorControlerListner implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {

    private static final Logger LOG = Logger.getLogger(PatnaBikeTrackConnectionControler.class);
    private static final List<String> allowedModes = Arrays.asList(TransportMode.bike);

    private final int numberOfBikeConnectorsRequired;
    private final int removeAConnectorAfterIteration;
    private final int initialStabilizationIterations = 100;

    private final List<Id<Link>> removedConnectorLinks = new ArrayList<>();
    private final FilteredLinkVolumeHandler handler = new FilteredLinkVolumeHandler(allowedModes);

    private final Map<Id<Link>, Double> linkId2Count = new HashMap<>();

    private final String bikeTrackFile;
    private final double reduceLinkLengthBy;

    private final List<Id<Link>> bikeConnectorLinks = new ArrayList<>(); // in total 500 links will be added to the list.
    private int totalPossibleConnectors = 0;

    @Inject
    Scenario scenario;
    private boolean terminateSimulation = false;
    private PlanAlgorithm router;

    public BikeConnectorControlerListner(final int numberOfConnectorsRequired, final int removeOneConnectorAfterIteration,
                                         final String bikeTrackFile, final double reduceLinkLengthBy) {
        this.numberOfBikeConnectorsRequired = numberOfConnectorsRequired;
        this.removeAConnectorAfterIteration = removeOneConnectorAfterIteration;
        this.bikeTrackFile = bikeTrackFile;
        this.reduceLinkLengthBy = reduceLinkLengthBy;
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        // dont want to do anything except removing isolated nodes,
        // that's why, not using core network simplifier or cleaner.
        NetworkUtils.removeIsolatedNodes(scenario.getNetwork());

        Network bikeNetwork = LoadMyScenarios.loadScenarioFromNetwork(this.bikeTrackFile).getNetwork();

        // first, add all possible connectors other wise, the nearest node will be one of the node of the link itself.
        LOG.info("========================== Adding all possible connectors to bike track first ...");
        for (Node bikeNode : bikeNetwork.getNodes().values()) {
            Coord cord = bikeNode.getCoord();

            Node n = org.matsim.core.network.NetworkUtils.getNearestNode(scenario.getNetwork(), cord);

            if (! scenario.getNetwork().getNodes().containsKey(bikeNode.getId())) {
                org.matsim.core.network.NetworkUtils.createAndAddNode(scenario.getNetwork(), bikeNode.getId(), cord);
            }

            addBikeConnectorLinksToScenario(scenario.getNetwork(), new Node[]{bikeNode, n});
        }
        this.totalPossibleConnectors = this.bikeConnectorLinks.size();

        LOG.info("========================== Adding links from proposed bike track to regular network.");
        for (Node n : bikeNetwork.getNodes().values()) {
            if (scenario.getNetwork().getNodes().containsKey(n.getId())) continue;
            org.matsim.core.network.NetworkUtils.createAndAddNode(scenario.getNetwork(), n.getId(), n.getCoord());
        }

        for (Link l : bikeNetwork.getLinks().values()) {
            if (scenario.getNetwork().getLinks().containsKey(l.getId())) continue;
            else {// link must be re-created so that node objects are same.
                Node fromNode = scenario.getNetwork().getNodes().get(l.getFromNode().getId());
                Node toNode = scenario.getNetwork().getNodes().get(l.getToNode().getId());
                Link lNew = org.matsim.core.network.NetworkUtils.createAndAddLink(scenario.getNetwork(), l.getId(), fromNode, toNode,
                        l.getLength() / reduceLinkLengthBy, l.getFreespeed(), l.getCapacity(), l.getNumberOfLanes());
                lNew.setAllowedModes(new HashSet<>(allowedModes));
            }
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if ( isRemovingBikeConnector(event.getIteration()) ) {

            int numberOfRemainingConnectors = this.bikeConnectorLinks.size() - this.removedConnectorLinks.size();
            if (numberOfRemainingConnectors > this.numberOfBikeConnectorsRequired) {

                Map<Id<Link>, Map<Integer, Double>> link2time2vol = this.handler.getLinkId2TimeSlot2LinkCount();
                this.linkId2Count.clear();

                for (Id<Link> linkId : this.bikeConnectorLinks) {
                    if (link2time2vol.containsKey(linkId)) {
                        this.linkId2Count.put(linkId, playground.agarwalamit.utils.MapUtils.doubleValueSum(link2time2vol.get(linkId)));
                    } else {
                        this.linkId2Count.put(linkId, 0.);
                    }
                }

                // sort based on the values (i.e. link count and not PCU)
                Comparator<Map.Entry<Id<Link>, Double>> byValue = Comparator.comparing(Map.Entry::getValue);

                Id<Link> connector2remove = linkId2Count.entrySet().stream().sorted(byValue).limit(1).iterator().next().getKey();
                this.removedConnectorLinks.add(connector2remove);
                this.bikeConnectorLinks.remove(connector2remove); // necessary, else, in the next round, same link can appear for removal.

                double aboutZeroFreeSpeed = 0.01;
//                addNetworkChangeEvent(connector2remove, aboutZeroFreeSpeed);
                // i think, i can simply change the speed, rather than using network change event because
//                 it is permanent change and I can observe this directly in network file.
                this.scenario.getNetwork().getLinks().get(connector2remove).setFreespeed(aboutZeroFreeSpeed);

                reRoutePlan(connector2remove); // only if this connector is present in the route of any leg.

                LOG.info("========================== The free speed on the link " + connector2remove + " is set to " + aboutZeroFreeSpeed + " m/s.");
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
                && iteration % this.removeAConnectorAfterIteration == 0;
    }

    private void addNetworkChangeEvent(final Id<Link> connector2remove, final double aboutZeroFreeSpeed) {
        double startTime = scenario.getConfig().qsim().getStartTime();

        final NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(startTime);
        networkChangeEvent.addLink(scenario.getNetwork().getLinks().get(connector2remove));
        NetworkChangeEvent.ChangeValue changeValue = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, aboutZeroFreeSpeed);
        networkChangeEvent.setFreespeedChange(changeValue);

        List<NetworkChangeEvent> networkChangeEventList = new ArrayList<>();
        networkChangeEventList.add(networkChangeEvent);
        // dont use addNetworkChangeEvent else it will throw exception about unequal network change events from VariableIntervalTimeVariantAttribute (line 70).
        // I think, the above method is meant for something else, probably, when adding all network change events one by one before starting the controler.
        org.matsim.core.network.NetworkUtils.setNetworkChangeEvents(scenario.getNetwork(), networkChangeEventList);
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (isRemovingBikeConnector(event.getIteration())) {
            handler.reset(event.getIteration());
            event.getServices().getEvents().addHandler(handler);
        }
    }

    private void addBikeConnectorLinksToScenario(final Network network, final Node[] nodes) {
        double dist = CoordUtils.calcEuclideanDistance(nodes[0].getCoord(), nodes[1].getCoord());
        double linkCapacity = 1500.;
        double linkSpeed = 40. / 3.6;
        {
            String id = "connector_link_" + scenario.getNetwork().getLinks().size();
            Id<Link> linkId = Id.createLinkId(id);
            Link l = org.matsim.core.network.NetworkUtils.createAndAddLink(scenario.getNetwork(), linkId, nodes[0],
                    nodes[1], dist / reduceLinkLengthBy, linkSpeed, linkCapacity, 1);
            l.setAllowedModes(new HashSet<>(allowedModes));
            this.bikeConnectorLinks.add(linkId);
        }
        {
            String id = "connector_link_" + scenario.getNetwork().getLinks().size();
            Id<Link> linkId = Id.createLinkId(id);
            Link l = org.matsim.core.network.NetworkUtils.createAndAddLink(scenario.getNetwork(), linkId, nodes[1],
                    nodes[0], dist / reduceLinkLengthBy, linkSpeed, linkCapacity, 1);
            l.setAllowedModes(new HashSet<>(allowedModes));
            this.bikeConnectorLinks.add(linkId);
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
        this.router = getRouter();
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
                if (needsReRoute) this.router.run(plan);
            }
        }
    }

    private PlanAlgorithm getRouter() {
        final TravelTime travelTime = new FreeSpeedTravelTimeForBike();
        TripRouterFactoryBuilderWithDefaults routerFactory = new TripRouterFactoryBuilderWithDefaults();
        routerFactory.setTravelTime(travelTime);
        routerFactory.setTravelDisutility(
                new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.bike, scenario.getConfig().planCalcScore()).createTravelDisutility(travelTime));

        final TripRouter tripRouter = routerFactory.build(this.scenario).get();
        PlanAlgorithm router = new PlanRouter(tripRouter);
        return router;
    }
}