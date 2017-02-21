package playground.clruch.dispatcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.AbstractRequestSelector;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.AbstractVirtualNodeDest;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

import playground.clruch.netdata.*;

public class ConsensusDispatcher extends PartitionedDispatcher {
    public static final int REBALANCING_PERIOD = 5 * 60; // TODO
    final AbstractVirtualNodeDest abstractVirtualNodeDest;
    final AbstractRequestSelector abstractRequestSelector;
    final AbstractVehicleDestMatcher abstractVehicleDestMatcher;
    final Map<VirtualLink, Double> linkWeights;
    Map<VirtualLink, Double> rebalanceFloating;


    public ConsensusDispatcher( //
                                AVDispatcherConfig config, //
                                TravelTime travelTime, //
                                ParallelLeastCostPathCalculator router, //
                                EventsManager eventsManager, //
                                VirtualNetwork virtualNetwork, //
                                AbstractVirtualNodeDest abstractVirtualNodeDest, //
                                AbstractRequestSelector abstractRequestSelector, //
                                AbstractVehicleDestMatcher abstractVehicleDestMatcher, //
                                File linkWeightFile
    )

    {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        this.abstractVirtualNodeDest = abstractVirtualNodeDest;
        this.abstractRequestSelector = abstractRequestSelector;
        this.abstractVehicleDestMatcher = abstractVehicleDestMatcher;
        this.rebalanceFloating = new HashMap<>();
        this.linkWeights = new HashMap<>();
        this.fillLinkWeights(linkWeightFile);
    }

    @Override
    public void redispatch(double now) {
        // match requests and vehicles if they are at t
        // he same link
        int seconds = (int) Math.round(now);
        {
            // TODO also send vehicles within node in a smart way... or is this done later in 2.4?
            Map<Link, Queue<AVVehicle>> map = getStayVehicles(); // link -> stayed vehicles
            Collection<AVRequest> collection = getAVRequests(); // all requests
            if (!map.isEmpty() && !collection.isEmpty()) { // has stay vehicles and has requests
                // System.out.println(now + " @ " + map.size() + " <-> " + collection.size());
                for (AVRequest avRequest : collection) {
                    Link link = avRequest.getFromLink();
                    if (map.containsKey(link)) {
                        Queue<AVVehicle> queue = map.get(link);
                        if (queue.isEmpty()) {
                            // unmatched.add(link);
                        } else {
                            AVVehicle avVehicle = queue.poll();
                            setAcceptRequest(avVehicle, avRequest); // PICKUP+DRIVE+DROPOFF
                        }
                    }
                }
            }
        }

        // for available vhicles, perform a rebalancing computation after REBALANCING_PERIOD seconds.
        if (seconds % REBALANCING_PERIOD == 0) {
            // 0 get available vehicles and requests per virtual node
            Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeAvailableVehicles();
            Map<VirtualNode, List<AVRequest>> requests = getVirtualNodeRequests();

            // 1 Calculate the rebalancing action for every virtual link
            Map<VirtualLink, Integer> rebalanceCount = new HashMap<>();
            {
                for (VirtualLink vlink : virtualNetwork.getVirtualLinks()) {
                    //compute imbalance on nodes of link
                    int imbalanceFrom = requests.get(vlink.getFrom()).size() - availableVehicles.get(vlink.getFrom()).size();
                    int imbalanceTo = requests.get(vlink.getTo()).size() - availableVehicles.get(vlink.getTo()).size();

                    // compute the rebalancing vehicles
                    double vehicles_From_to_To =  //
                            REBALANCING_PERIOD * linkWeights.get(vlink) * ((double) imbalanceTo - (double) imbalanceFrom) +  //
                                    rebalanceFloating.get(vlink);

                    // assign integer number to rebalance vehicles and store float for next iteration
                    // only consider the results which are >= 0. This assumes an undirected graph.
                    if (vehicles_From_to_To >= 0) {
                        rebalanceCount.put(vlink, (int) Math.floor(vehicles_From_to_To));
                        rebalanceFloating.put(vlink, vehicles_From_to_To - (int) Math.floor(vehicles_From_to_To));

                    } else {
                        rebalanceCount.put(vlink, 0);
                    }
                }
            }


            // 2 generate routing instructions for vehicles
            // 2.1 gather the destination links
            Map<VirtualNode, List<Link>> destinationLinks = new HashMap<>();
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
                destinationLinks.put(virtualNode, new ArrayList<>());

            // 2.2 fill rebalancing destinations

            // TODO size negative?
            for (Entry<VirtualLink, Integer> entry : rebalanceCount.entrySet()) {
                final VirtualLink virtualLink = entry.getKey();
                final int size = entry.getValue();
                final VirtualNode fromNode = virtualLink.getFrom();
                // Link origin = fromNode.getLinks().iterator().next(); //
                VirtualNode toNode = virtualLink.getTo();

                Set<Link> rebalanceTargets = abstractVirtualNodeDest.selectLinkSet(toNode, size);

                destinationLinks.get(fromNode).addAll(rebalanceTargets);
            }

            // 2.3 consistency check: rebalancing destination links must not exceed available vehicles in virtual node
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
                int sizeV = availableVehicles.get(virtualNode).size();
                int sizeL = destinationLinks.get(virtualNode).size();
                if (sizeL > sizeV)
                    throw new RuntimeException("rebalancing inconsistent " + sizeL + " > " + sizeV);
            }

            // fill request destinations
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
                // number of vehicles that can be matched to requests
                int size = Math.min( //
                        availableVehicles.get(virtualNode).size() - destinationLinks.get(virtualNode).size(), //
                        requests.get(virtualNode).size());

                Collection<AVRequest> collection = abstractRequestSelector.selectRequests( //
                        availableVehicles.get(virtualNode), //
                        requests.get(virtualNode), //
                        size);

                // TODO

                destinationLinks.get(virtualNode).addAll( // stores from links
                        collection.stream().map(AVRequest::getFromLink).collect(Collectors.toList()));
            }

            // 2.4 fill extra destinations for left over vehicles
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
                // number of vehicles that can be matched to requests
                int size = availableVehicles.get(virtualNode).size() - destinationLinks.get(virtualNode).size(); //

                // TODO maybe not final API; may cause excessive diving
                Set<Link> localTargets = abstractVirtualNodeDest.selectLinkSet(virtualNode, size);
                destinationLinks.get(virtualNode).addAll(localTargets);

            }

            // 2.5 assign destinations to the available vehicles
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {

                Map<VehicleLinkPair, Link> map = abstractVehicleDestMatcher.match(availableVehicles.get(virtualNode), destinationLinks.get(virtualNode));
                for (Entry<VehicleLinkPair, Link> entry : map.entrySet()) {
                    VehicleLinkPair vehicleLinkPair = entry.getKey();
                    Link link = entry.getValue();
                    setVehicleDiversion(vehicleLinkPair, link);
                }
            }
        }
    }

    private void fillLinkWeights(File file) {
        // open the linkWeightFile and parse the parameter values
        SAXBuilder builder = new SAXBuilder();
        try {
            Document document = (Document) builder.build(file);
            Element rootNode = document.getRootElement();
            Element virtualNodesXML = rootNode.getChild("weights");
            List<Element> virtualLinkXML = virtualNodesXML.getChildren("virtuallink");
            for (Element vLinkelem : virtualLinkXML) {
                String vlinkID = vLinkelem.getAttributeValue("id");
                Double weight = Double.parseDouble(vLinkelem.getAttributeValue("weight"));


                // find the virtual link with the corresponding ID and assign the weight to it.
                linkWeights.put((virtualNetwork.getVirtualLinks()).stream()
                                .filter(vl -> vl.getId().toString().equals(vlinkID))
                                .findFirst()
                                .get(),
                        weight);
            }


        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
