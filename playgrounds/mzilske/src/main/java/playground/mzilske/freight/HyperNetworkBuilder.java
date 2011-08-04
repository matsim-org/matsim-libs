package playground.mzilske.freight;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordImpl;

public class HyperNetworkBuilder {

	private TSPCapabilities tspCapabilities;
	private Collection<Id> shipmentLocations;
	private Network network;
	private Map<Link, Double> travelTimes = new HashMap<Link, Double>();
	private TravelTime travelTime = new TravelTime() {

		@Override
		public double getLinkTravelTime(Link link, double time) {
			return travelTimes.get(link);
		}
		
	};
	private Coord nullCoord = new CoordImpl(0,0);
	private CarrierAgentTracker carrierAgentTracker;

	public HyperNetworkBuilder(TSPCapabilities tspCapabilities, Collection<Id> shipmentLocations, CarrierAgentTracker carrierAgentTracker) {
		this.tspCapabilities = tspCapabilities;
		this.shipmentLocations = shipmentLocations;
		this.carrierAgentTracker = carrierAgentTracker;
		createCompleteGraph();
	}

	private void createCompleteGraph() {
		network = new MyNetwork();
		for (Id transhipmentCenterId : tspCapabilities.getTransshipmentCentres()) {
			network.addNode(network.getFactory().createNode(transhipmentCenter(transhipmentCenterId), nullCoord));
		}
		for (Id shipmentLocation : shipmentLocations) {
			network.addNode(network.getFactory().createNode(shipmentLocation(shipmentLocation), nullCoord));
		}
		for (Id transhipmentCenterId : tspCapabilities.getTransshipmentCentres()) {
			Id transhipmentCenter = transhipmentCenter(transhipmentCenterId);
			for (Id otherTranshipmentCenterId : tspCapabilities.getTransshipmentCentres()) {
				Id otherTranshipmentCenter = transhipmentCenter(otherTranshipmentCenterId);
				if (transhipmentCenter.equals(otherTranshipmentCenter)) continue;
				addLinks(transhipmentCenter, otherTranshipmentCenter);
			}
			for (Id shipmentLocationId : shipmentLocations) {
				Id shipmentLocation = shipmentLocation(shipmentLocationId);
				addLinks(transhipmentCenter, shipmentLocation);
			}
		}
		for (Id shipmentLocationId : shipmentLocations) {
			Id shipmentLocation = shipmentLocation(shipmentLocationId);
			for (Id otherShipmentLocationId : shipmentLocations) {
				Id otherShipmentLocation = shipmentLocation(otherShipmentLocationId);
				if (shipmentLocation.equals(otherShipmentLocation)) continue;
				addLinks(shipmentLocation, otherShipmentLocation);
			}
			for (Id transhipmentCenterId : tspCapabilities.getTransshipmentCentres()) {
				Id transhipmentCenter = transhipmentCenter(transhipmentCenterId);
				addLinks(shipmentLocation, transhipmentCenter);
			}
		}
	}

	private void addLinks(Id fromNode, Id toNode) {
		int shipmentSize = 1;
		Collection<CarrierOffer> offers = carrierAgentTracker.getOffers(linkId(fromNode), linkId(toNode), shipmentSize);
		for (CarrierOffer offer : offers) {
			Id intermediateNode = intermediateNode(fromNode, offer.getId(), toNode);
			Node node = network.getFactory().createNode(intermediateNode, nullCoord);
			network.addNode(node);
			Link link = network.getFactory().createLink(link(fromNode, intermediateNode), 
					network.getNodes().get(fromNode), 
					node);
			travelTimes.put(link, offer.getDuration());
			network.addLink(link);
			network.addLink(network.getFactory().createLink(link(intermediateNode, toNode), 
					node, 
					network.getNodes().get(toNode)));
			System.out.println(network.getNodes().size() + "  " + network.getLinks().size());
		}
	}

	private Id intermediateNode(Id transhipmentCenter, Id carrierId, Id otherTranshipmentCenter) {
		return new IdImpl("("+transhipmentCenter.toString()+","+carrierId.toString()+","+otherTranshipmentCenter.toString());
	}

	private Id link(Id transhipmentCenter, Id transhipmentCenter2) {
		return new IdImpl(transhipmentCenter.toString() + "_" + transhipmentCenter2.toString());
	}

	private Id shipmentLocation(Id shipmentLocation) {
		return new IdImpl("shipmentLocation_" + shipmentLocation.toString());
	}

	private Id transhipmentCenter(Id transhipmentCenterId) {
		return new IdImpl("transhipmentCenter_" + transhipmentCenterId.toString());
	}

	private Id linkId(Id hyperNetworkNodeId) {
		return new IdImpl(hyperNetworkNodeId.toString().split("_")[1]);
	}

	private class MyNetwork extends NetworkImpl {

	}

}
