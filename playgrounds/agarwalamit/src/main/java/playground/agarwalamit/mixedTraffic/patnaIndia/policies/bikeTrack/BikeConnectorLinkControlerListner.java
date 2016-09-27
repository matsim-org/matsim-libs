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

import java.util.*;
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.algorithms.PersonPrepareForSim;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import playground.agarwalamit.analysis.linkVolume.FilteredLinkVolumeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForTruck;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.MapUtils;

/**
 * Created by amit on 25/09/16.
 */


public class BikeConnectorLinkControlerListner implements StartupListener, IterationStartsListener, IterationEndsListener {

    private static final Logger LOG = Logger.getLogger(BikeConnectorLinkControlerListner.class);
    private final List<String> modes = Arrays.asList("bike");
    private final Set<String> allowedModes = new HashSet<>(modes);
    private final double blendFactor = 0.95;

    private final String initialNetwork = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/network.xml.gz";
    private final String bikeTrack = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/bikeTrack.xml.gz";

    private final int numberOfConnectors ;
    private final int updateConnectorsAfterIteration;

    private FilteredLinkVolumeHandler handler = new FilteredLinkVolumeHandler(modes);

    private final Map<Id<Link>,Link> linkIds = new HashMap<>();
    private final SortedMap<Id<Link>,Double> linkId2Count = new TreeMap<>();

    public BikeConnectorLinkControlerListner (final int numberOfConnectorLinks, final int updateConnectorsAfterIteration ) {
        this.numberOfConnectors = numberOfConnectorLinks;
        LOG.info(numberOfConnectorLinks + " connector links will be added to the network based on the maximum bike volume.");
        this.updateConnectorsAfterIteration = updateConnectorsAfterIteration;
        LOG.info("The bike volume on the possible connector link is updated after every "+ updateConnectorsAfterIteration+" iterations.");
        LOG.info("The blend factor for old counts is "+ blendFactor);
    }

    @Inject Scenario scenario;

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if(event.getIteration()==scenario.getConfig().controler().getFirstIteration()) {
            Map<Id<Link>, Map<Integer, Double>> link2time2vol = handler.getLinkId2TimeSlot2LinkCount();
            for(Id<Link> linkId : linkIds.keySet()) {
                if(! link2time2vol.containsKey(linkId)) continue;
                double count = MapUtils.doubleValueSum( link2time2vol.get(linkId) );
                linkId2Count.put(linkId,count);
            }
        } else if(event.getIteration() % updateConnectorsAfterIteration == 0){
            Map<Id<Link>, Map<Integer, Double>> link2time2vol = handler.getLinkId2TimeSlot2LinkCount();
            for(Id<Link> linkId : linkIds.keySet()) {
                if (! linkId2Count.containsKey(linkId) ) continue;

                double oldCount = linkId2Count.get(linkId);
                double count = link2time2vol.containsKey(linkId) ? MapUtils.doubleValueSum( link2time2vol.get(linkId) ) : 0.0;
                linkId2Count.put(linkId,  count * (1-blendFactor) +  blendFactor * oldCount);
            }

            // sort based on the values (i.e. link volume)
            Comparator<Map.Entry<Id<Link>, Double>> byValue = (entry1, entry2) -> entry1.getValue().compareTo(
                    entry2.getValue());

            // take only pre-decided number of links.
            Iterator<Map.Entry<Id<Link>, Double>> iterator = linkId2Count.entrySet().stream().sorted(byValue.reversed()).limit(numberOfConnectors).iterator();
            while (iterator.hasNext()) {
                Link l = linkIds.get(iterator.next().getKey());
                l.setAllowedModes(allowedModes);
                scenario.getNetwork().addLink(l);
                LOG.info("Connector "+ l.getId()+" is added to the network,");
            }

            // remove links in the routes from the plans of all persons
            reassignRoutes();

            // remove if connector links exists
            for  (Id<Link> lId : linkIds.keySet()) {
                scenario.getNetwork().removeLink(lId);
            }
        }
        event.getServices().getEvents().removeHandler(handler);
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        // add handler for analysis
        handler.reset(event.getIteration());
        event.getServices().getEvents().addHandler(handler);
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        // get connector links
        BikeTrackConnectionIdentifier bikeTrackConnector = new BikeTrackConnectionIdentifier(this.initialNetwork, this.bikeTrack);
        bikeTrackConnector.run();
        linkIds.clear();
        linkIds.putAll(bikeTrackConnector.getConnectedLinks());

        // add nodes first, cant add net.addNode(bikeNode) directly due to information attached to the node.
        List<Node> bikeTrackNodes = bikeTrackConnector.getBikeTrackNodes();

        for(Node n : bikeTrackNodes) { // i think keeping some unused nodes is better than first finding which nodes to add and then add then again.
            if(scenario.getNetwork().getNodes().containsKey(n.getId())) continue;
            NetworkUtils.createAndAddNode(scenario.getNetwork(),n.getId(),n.getCoord());
        }

        // add all connector links to the network
        for (Id<Link> lId : linkIds.keySet()) {
            Link l = linkIds.get(lId);
            l.setAllowedModes(allowedModes);
            scenario.getNetwork().addLink(l);
        }
        LOG.info("All possible connector links between bike track and network are added.");
    }

    private void reassignRoutes() {
        LOG.info("Routes of all bike plans will be re-assinged.");

        // following is required to get the routes (or links) from base network and not from the running scenario network.
        Scenario scNetwork = LoadMyScenarios.loadScenarioFromNetwork(this.initialNetwork);

        // create routers for all modes, since this step is manual assignment of routes
        Map<String, PersonPrepareForSim> mode2routers = new HashMap<>();
        for(String mode : scenario.getConfig().qsim().getMainModes()){
            TravelTime tt ;
            if (mode.equals("bike")) tt = new FreeSpeedTravelTimeForBike();
            else if(mode.equals("truck")) tt = new FreeSpeedTravelTimeForTruck();
            else tt = new FreeSpeedTravelTime();

            TripRouterFactoryBuilderWithDefaults routerFactory = new TripRouterFactoryBuilderWithDefaults();
            routerFactory.setTravelTime(tt);
            routerFactory.setTravelDisutility( new RandomizingTimeDistanceTravelDisutilityFactory(mode,scenario.getConfig().planCalcScore()).createTravelDisutility(tt) );

            final TripRouter tripRouter = routerFactory.build(scNetwork).get();
            PlanAlgorithm router = new PlanRouter( tripRouter );

            PersonPrepareForSim pp4s = new PersonPrepareForSim(router, (MutableScenario) scNetwork);
            mode2routers.put(mode,pp4s);
        }

        // first remove routes from legs of bike mode
        for (Person p : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : p.getPlans()) {
                List<PlanElement> pes = plan.getPlanElements();
                for (PlanElement pe : pes) {
                    if(pe instanceof  Activity) {
                        Activity act = ((Activity)pe);
                        Id<Link> linkId = act.getLinkId();
                        Coord cord = act.getCoord();

                        if(linkId==null) { // activity should have at least one of link id or coord
                            if(cord==null) throw new RuntimeException("Activity "+act.toString()+" do not have either of link id or coord. Aborting...");
                            else {/*nothing to do; cord is assigned*/ }
                        } else if (cord==null) { // if cord is null, get it from
                            if(scNetwork.getNetwork().getLinks().containsKey(linkId)) {
                                cord = scNetwork.getNetwork().getLinks().get(linkId).getCoord();
                                act.setLinkId(null);
                                act.setCoord(cord);
                            } else throw new RuntimeException("Activity "+act.toString()+" do not have cord and link id is not present in network. Aborting...");
                        }
                    }
                    else if (pe instanceof Leg) {
                        Leg leg = (Leg) pe;
                        if (modes.contains(leg.getMode())) leg.setRoute(null);
                    }
                }
            }
        }

        for (Person p : scenario.getPopulation().getPersons().values()){
            // since all trips have same mode, following is safe.
            String mode = ((Leg)p.getSelectedPlan().getPlanElements().get(1)).getMode();
            mode2routers.get(mode).run(p);
        }
    }
}
