package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.Shipment;
import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.Node;
import vrp.api.VRP;
import vrp.basics.CrowFlyDistance;
import vrp.basics.VrpImpl;
import vrp.basics.VrpUtils;

public class VrpBuilder {
	
	private Customer depotCustomer;
	
	private Collection<Customer> customers = new ArrayList<Customer>();
	
	private Map<Id, Node> nodes = new HashMap<Id, Node>();
	
	private Map<Id, Shipment> customer2ShipmentMap;
	
	private Network network;
	
	private Constraints constraints;
	
	private int customerIdCounter = 1;
	
	public VrpBuilder(Id depotId, Network network, Map<Id, Shipment> customer2ShipmentMap) {
		super();
		this.network = network;
		this.customer2ShipmentMap = customer2ShipmentMap;
		Node depotNode = createNode(depotId,network.getLinks().get(depotId).getCoord());
		depotCustomer = createCustomer(makeId(0),depotNode,0);
		nodes.put(depotId, depotNode);
		customers.add(depotCustomer);
	}

	private Customer createCustomer(Id id, Node node, int demand) {
		return VrpUtils.createCustomer(id, node, demand, 0.0, Double.MAX_VALUE, 0.0);
	}

	private Id makeId(int id) {
		return new IdImpl(id);
	}
	
	public void addShipment(Shipment shipment){
		Customer fromCustomer = null;
		Customer toCustomer = null;
		if(shipment.getFrom().equals(depotCustomer.getLocation().getId())){
			fromCustomer = depotCustomer;
			Node toNode = nodes.get(shipment.getTo());
			if(toNode == null){
				toNode = createNode(shipment.getTo(),network.getLinks().get(shipment.getTo()).getCoord());
				nodes.put(toNode.getId(), toNode);
			}
			toCustomer = VrpUtils.createCustomer(makeCustomerId(), toNode, shipment.getSize()*-1, shipment.getDeliveryTimeWindow().getStart(), 
					shipment.getDeliveryTimeWindow().getEnd(), 0.0);
			customers.add(toCustomer);
			customer2ShipmentMap.put(toCustomer.getId(), shipment);
		}
		else if(shipment.getTo().equals(depotCustomer.getLocation().getId())){
			toCustomer = depotCustomer;
			Node fromNode = nodes.get(shipment.getFrom());
			if(fromNode == null){
				fromNode = createNode(shipment.getFrom(), network.getLinks().get(shipment.getFrom()).getCoord());
				nodes.put(fromNode.getId(), fromNode);
			}
			fromCustomer = VrpUtils.createCustomer(makeCustomerId(), fromNode, shipment.getSize(), shipment.getPickupTimeWindow().getStart(), 
					shipment.getPickupTimeWindow().getEnd(), 0.0);
			customers.add(fromCustomer);
			customer2ShipmentMap.put(fromCustomer.getId(), shipment);
		}
		else {	//-> pickup and delivery
			Node fromNode = nodes.get(shipment.getFrom());
			if(fromNode == null){
				fromNode = createNode(shipment.getFrom(), network.getLinks().get(shipment.getFrom()).getCoord());
				nodes.put(fromNode.getId(), fromNode);
			}
			fromCustomer = VrpUtils.createCustomer(makeCustomerId(), fromNode, shipment.getSize(), shipment.getPickupTimeWindow().getStart(), 
					shipment.getPickupTimeWindow().getEnd(), 0.0);
			customers.add(fromCustomer);
			customer2ShipmentMap.put(fromCustomer.getId(), shipment);
			Node toNode = nodes.get(shipment.getTo());
			if(toNode == null){
				toNode = createNode(shipment.getTo(),network.getLinks().get(shipment.getTo()).getCoord());
				nodes.put(toNode.getId(), toNode);
			}
			toCustomer = VrpUtils.createCustomer(makeCustomerId(), toNode, shipment.getSize()*-1, shipment.getDeliveryTimeWindow().getStart(), 
					shipment.getDeliveryTimeWindow().getEnd(), 0.0);
			customers.add(toCustomer);
			customer2ShipmentMap.put(toCustomer.getId(), shipment);
			fromCustomer.setRelation(VrpUtils.createRelation(toCustomer));
			toCustomer.setRelation(VrpUtils.createRelation(fromCustomer));
		}
		
	}
	
	public void setConstraints(Constraints constraints){
		this.constraints = constraints;
	}
	
	public VRP buildVrp(){
		Costs costs = new CrowFlyDistance();
		VRP vrp = new VrpImpl(depotCustomer.getId(), customers, costs, constraints);
		return vrp;
	}
	
	private Node createNode(Id id, Coord coord){
		Node node = VrpUtils.createNode(id.toString());
		node.setCoord(coord);
		return node;
	}
	
	private Id makeCustomerId(){
		Id id = makeId(customerIdCounter);
		customerIdCounter++;
		return id;
	}

}
