package playground.mzilske.freight;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class HyperNetworkBuilder {
	
	private TSPCapabilities tspCapabilities;
	private Collection<Id> shipmentLocations;
	private Network network;
	private Coord nullCoord = new CoordImpl(0,0);
	private Carriers carriers;
	
	public HyperNetworkBuilder(TSPCapabilities tspCapabilities, Collection<Id> shipmentLocations, Carriers carriers) {
		this.tspCapabilities = tspCapabilities;
		this.shipmentLocations = shipmentLocations;
		this.carriers = carriers;
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
				for (CarrierImpl carrier : carriers.getCarriers()) {
					addLink(transhipmentCenter, otherTranshipmentCenter, carrier);
				}
			}
			for (Id shipmentLocationId : shipmentLocations) {
				Id shipmentLocation = shipmentLocation(shipmentLocationId);
				for (CarrierImpl carrier : carriers.getCarriers()) {
					addLink(transhipmentCenter, shipmentLocation, carrier);
				}
			}
		}
		for (Id shipmentLocationId : shipmentLocations) {
			Id shipmentLocation = shipmentLocation(shipmentLocationId);
			for (Id otherShipmentLocationId : shipmentLocations) {
				Id otherShipmentLocation = shipmentLocation(otherShipmentLocationId);
				if (shipmentLocation.equals(otherShipmentLocation)) continue;
				for (CarrierImpl carrier : carriers.getCarriers()) {
					addLink(shipmentLocation, otherShipmentLocation, carrier);
				}
			}
			for (Id transhipmentCenterId : tspCapabilities.getTransshipmentCentres()) {
				Id transhipmentCenter = transhipmentCenter(transhipmentCenterId);
				for (CarrierImpl carrier : carriers.getCarriers()) {
					addLink(shipmentLocation, transhipmentCenter, carrier);
				}
			}
		}
	}

	private void addLink(Id transhipmentCenter, Id otherTranshipmentCenter, CarrierImpl carrier) {
		Id intermediateNode = intermediateNode(transhipmentCenter, carrier, otherTranshipmentCenter);
		Node node = network.getFactory().createNode(intermediateNode, nullCoord);
		network.addNode(node);
		network.addLink(network.getFactory().createLink(link(transhipmentCenter, intermediateNode), 
				network.getNodes().get(transhipmentCenter), 
				node));
		network.addLink(network.getFactory().createLink(link(intermediateNode, otherTranshipmentCenter), 
				node, 
				network.getNodes().get(otherTranshipmentCenter)));
		System.out.println(network.getNodes().size() + "  " + network.getLinks().size());
	}
	
	private Id intermediateNode(Id transhipmentCenter, CarrierImpl carrier, Id otherTranshipmentCenter) {
		return new IdImpl("("+transhipmentCenter.toString()+","+carrier.getId().toString()+","+otherTranshipmentCenter.toString());
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

	private class MyNetwork extends NetworkImpl {
		
	}

}
