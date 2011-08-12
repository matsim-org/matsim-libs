package freight.vrp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;


import playground.mzilske.freight.Shipment;
import vrp.api.Customer;
import vrp.api.Node;
import vrp.basics.VrpUtils;

/**
 * Vehicle Routing Algorithms used here require a customer/node related data structure. Thus, VRPTransformation transforms carrier-shipments into vrp-customers. 
 * For each shipment, the according origin- and destination-customer are stored. Vice versa, for each customer the related shipment is memorized.
 * 
 * @author stefan schroeder
 *
 */

public class VRPTransformation {
	
	private HashMap<Shipment,Id> fromCustomers = new HashMap<Shipment, Id>();
	
	private HashMap<Shipment,Id> toCustomers = new HashMap<Shipment, Id>();
	
	private HashMap<Id, Shipment> shipments = new HashMap<Id, Shipment>();
	
	private HashMap<Id,Customer> customers = new HashMap<Id,Customer>();
	
	private HashMap<Id, Node> nodes = new HashMap<Id, Node>();
	
	private Integer customerCounter = 0;

	private Locations locations;
	
	public VRPTransformation(Locations locations) {
		super();
		this.locations = locations;
	}
	
	public void addAndCreateCustomer(String customerId, Id linkId, int demand, double startTime, double endTime, double serviceTime){
		addAndCreateCustomer(makeId(customerId), linkId, demand, startTime, endTime, serviceTime);
	}
	
	public void addAndCreateCustomer(Id customerId, Id linkId, int demand, double startTime, double endTime, double serviceTime){
		Node node = makeNode(linkId);
		Customer customer = VrpUtils.createCustomer(customerId, node, demand, startTime, endTime, serviceTime);
		nodes.put(node.getId(), node);
		customers.put(customer.getId(), customer);
	}

	public void addShipments(Collection<Shipment> shipments){
		for(Shipment s : shipments){
			addShipment(s);
		}
	}

	public void addShipment(Shipment shipment){
		addNewShipment(shipment);
	}
	
	private void addNewShipment(Shipment shipment) {
		Customer fromCustomer = makeFromCustomer(shipment);
		Customer toCustomer = makeToCustomer(shipment);
		fromCustomer.setRelation(VrpUtils.createRelation(toCustomer));
		toCustomer.setRelation(VrpUtils.createRelation(fromCustomer));
		updateMaps(shipment, fromCustomer, toCustomer);
	}

	public void removeShipments(Collection<Shipment> shipments){
		for(Shipment s : shipments){
			removeShipment(s);
		}
	}
	
	public void removeShipment(Shipment shipment){
		Id fromCustomerId = fromCustomers.get(shipment);
		Id toCustomerId = toCustomers.get(shipment);
		shipments.remove(toCustomerId);
		shipments.remove(fromCustomerId);
		fromCustomers.remove(shipment);
		toCustomers.remove(shipment);
		customers.remove(toCustomerId);
		customers.remove(fromCustomerId);
	}

	public Customer getFromCustomer(Shipment shipment){
		Id id = fromCustomers.get(shipment);
		return customers.get(id);
	}

	public Customer getToCustomer(Shipment shipment){
		Id id = toCustomers.get(shipment);
		return customers.get(id);
	}

	public Shipment getShipment(Id customerId){
		return shipments.get(customerId);
	}

	public Collection<Customer> getCustomers() {
		return Collections.unmodifiableCollection(customers.values());
	}
	
	public Customer getCustomer(Id customerId){
		return customers.get(customerId);
	}

	public void clear(){
		fromCustomers.clear();
		toCustomers.clear();
		shipments.clear();
		customers.clear();
		nodes.clear();
	}

	private void updateMaps(Shipment shipment, Customer fromCustomer,
			Customer toCustomer) {
		customers.put(fromCustomer.getId(),fromCustomer);
		customers.put(toCustomer.getId(),toCustomer);
		toCustomers.put(shipment, toCustomer.getId());
		fromCustomers.put(shipment, fromCustomer.getId());
		shipments.put(fromCustomer.getId(), shipment);
		shipments.put(toCustomer.getId(), shipment);
	}

	private Customer makeToCustomer(Shipment shipment) {
		customerCounter++;
		Id id = shipment.getTo();
		Node node = makeNode(id);
		Customer customer = VrpUtils.createCustomer(makeId(customerCounter.toString()), node, shipment.getSize()*-1, 
				shipment.getDeliveryTimeWindow().getStart(), shipment.getDeliveryTimeWindow().getEnd(), 0.0);
		return customer;
	}

	private Node makeNode(Id id) {
		Node node = null;
		if(nodes.containsKey(id)){
			node = nodes.get(id);
		}
		else{
			node = VrpUtils.createNode(id.toString());
			node.setCoord(locations.getCoord(id));
			nodes.put(node.getId(), node);
		}
		return node;
	}

	private Id makeId(String customerCounter) {
		return new IdImpl(customerCounter);
	}

	private Customer makeFromCustomer(Shipment shipment) {
		customerCounter++;
		Id id = shipment.getFrom();
		Node node = null;
		if(nodes.containsKey(id)){
			node = nodes.get(id);
		}
		else{
			node = VrpUtils.createNode(id.toString());
			node.setCoord(locations.getCoord(id));
			nodes.put(node.getId(), node);
		}
		Customer customer = VrpUtils.createCustomer(makeId(customerCounter.toString()), node, shipment.getSize(), 
				shipment.getPickupTimeWindow().getStart(), shipment.getPickupTimeWindow().getEnd(), 0.0);	
		return customer;
	}
}
