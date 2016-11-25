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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.linkVolume.FilteredLinkVolumeHandler;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.NetworkUtils;

/**
 * The idea is to first connect the proposed bike track to regular network by all possible connectors
 * and then start removing one by one connector.
 *
 * Created by amit on 24/11/2016.
 */


public class BikeConnectorControlerListner implements StartupListener, IterationStartsListener, IterationEndsListener, TerminationCriterion, ShutdownListener {

    private static final Logger LOG = Logger.getLogger(PatnaBikeTrackConnectionControler.class);

    private final int numberOfConnectorsRequired ;
    private final int removeOneConnectorAfterIteration;

    private final List<String> consideredModesOnTrack = Arrays.asList("bike");
    private FilteredLinkVolumeHandler handler = new FilteredLinkVolumeHandler(consideredModesOnTrack);

    private final SortedMap<Id<Link>,Double> linkId2Count = new TreeMap<>();
    private final List<Id<Link>> removedConnectorLinks = new ArrayList<>();

    private final String bikeTrack ;
    private final int reduceLinkLengthBy;

    private int numberOfConnectors = 0;
    private boolean terminateSimulation = false;

    @Inject
    Scenario scenario;

    public BikeConnectorControlerListner(final int numberOfConnectorsRequired, final int removeOneConnectorAfterIteration,
                                         final String bikeTrackFile, final int reduceLinkLengthBy) {
        this.numberOfConnectorsRequired = numberOfConnectorsRequired;
        this.removeOneConnectorAfterIteration = removeOneConnectorAfterIteration;
        this.bikeTrack = bikeTrackFile;
        this.reduceLinkLengthBy = reduceLinkLengthBy;
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        // dont want to do anything except removing isolated nodes,
        // that's why, not using core network simplifier or cleaner.
        NetworkUtils.removeIsolatedNodes(scenario.getNetwork());

        // time dependent network for network change events
        scenario.getConfig().network().setTimeVariantNetwork(true);

        Network bikeNetwork = LoadMyScenarios.loadScenarioFromNetwork(this.bikeTrack).getNetwork();

        LOG.info("========================== Adding links from proposed bike track to regular network.");
        for(Node n : bikeNetwork.getNodes().values()) {
            if(scenario.getNetwork().getNodes().containsKey(n.getId())) continue;
            org.matsim.core.network.NetworkUtils.createAndAddNode(scenario.getNetwork(),n.getId(),n.getCoord());
        }

        for(Link l : bikeNetwork.getLinks().values()){
            if (scenario.getNetwork().getLinks().containsKey(l.getId()) ) continue;
            else{// link must be re-created so that node objects are same.
                Node fromNode = scenario.getNetwork().getNodes().get(l.getFromNode().getId());
                Node toNode = scenario.getNetwork().getNodes().get(l.getToNode().getId());
                Link lNew = org.matsim.core.network.NetworkUtils.createAndAddLink(scenario.getNetwork(), l.getId(), fromNode, toNode,
                        l.getLength()/reduceLinkLengthBy, l.getFreespeed(), l.getCapacity(), l.getNumberOfLanes());
                lNew.setAllowedModes(new HashSet<>(consideredModesOnTrack));
            }
        }

        // now add all possible connectors
        LOG.info("========================== Adding all possible connectors to bike track...");
        for(Node bikeNode : bikeNetwork.getNodes().values()) {
            Coord cord = bikeNode.getCoord();

            Node n = org.matsim.core.network.NetworkUtils.getNearestNode(scenario.getNetwork(), cord);
            addLinksToScenario(scenario.getNetwork(), new Node [] {bikeNode, n}); // it will add 2 links in both directions
            this.numberOfConnectors += 2;
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (event.getIteration() % this.removeOneConnectorAfterIteration == 0) {

            int numberOfRemainingConnectors = this.numberOfConnectors - this.removedConnectorLinks.size();
            if( numberOfRemainingConnectors > this.numberOfConnectorsRequired) {

                // sort based on the values (i.e. link volume)
                Comparator<Map.Entry<Id<Link>, Double>> byValue = (entry1, entry2) -> entry1.getValue().compareTo(
                        entry2.getValue());

                Id<Link> connector2remove = linkId2Count.entrySet().stream().sorted(byValue).limit(1).iterator().next().getKey();
                this.removedConnectorLinks.add(connector2remove);

                double startTime = scenario.getConfig().qsim().getStartTime();

                NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(startTime);
                networkChangeEvent.addLink(scenario.getNetwork().getLinks().get(connector2remove));
                NetworkChangeEvent.ChangeValue changeValue = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, 0.01);
                networkChangeEvent.setFreespeedChange(changeValue);
                LOG.info("========================== The free speed on the link " + connector2remove + " is set to " + changeValue.getValue() + "m/s.");

                org.matsim.core.network.NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(), networkChangeEvent);
            } else {
                //terminate the simulation after removeOneConnectorAfterIteration.
                terminateSimulation = true;
            }
        }
        event.getServices().getEvents().removeHandler(this.handler);
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        handler.reset(event.getIteration());
        event.getServices().getEvents().addHandler(handler);
    }

    private void addLinksToScenario(final Network network, final Node[] nodes) {
        double dist = CoordUtils.calcEuclideanDistance(nodes[0].getCoord(), nodes[1].getCoord());
        double linkCapacity = 1500.;
        double linkSpeed = 40./3.6;
        {
            String id = "connecter_link_"+ scenario.getNetwork().getLinks().size();
            Id<Link> linkId = Id.createLinkId(id);
            Link l = org.matsim.core.network.NetworkUtils.createAndAddLink(scenario.getNetwork(), linkId, nodes[0],
                    nodes[1], dist/reduceLinkLengthBy, linkSpeed, linkCapacity, 1);
        }
        {
            String id = "connecter_link_"+ scenario.getNetwork().getLinks().size();
            Id<Link> linkId = Id.createLinkId(id);
            Link l = org.matsim.core.network.NetworkUtils.createAndAddLink(scenario.getNetwork(), linkId, nodes[1],
                    nodes[0], dist/reduceLinkLengthBy, linkSpeed, linkCapacity, 1);
        }
    }

    @Override
    public boolean continueIterations(int iteration) {
        return (!terminateSimulation);
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        String outFile = event.getServices().getConfig().controler().getOutputDirectory()+"/removed_connectorLinks.txt";
        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
        this.removedConnectorLinks.forEach(link -> {
            try {
                writer.write(link.toString());
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
}
