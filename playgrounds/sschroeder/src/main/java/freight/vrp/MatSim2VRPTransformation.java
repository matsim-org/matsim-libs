package freight.vrp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.carrier.CarrierShipment;
import playground.mzilske.freight.carrier.Shipment;
import vrp.api.Customer;
import vrp.api.Node;
import vrp.basics.Coordinate;
import vrp.basics.VrpUtils;

/**
 * Vehicle Routing Algorithms used here require a customer/node related data structure. Thus, VRPTransformation transforms carrier-shipments into vrp-customers. 
 * For each shipment, the according origin- and destination-customer are stored. Vice versa, for each customer the related shipment is memorized.
 * 
 * @author stefan schroeder
 *
 */

public class MatSim2VRPTransformation {
	
	private HashMap<CarrierShipment,Id> fromCustomers = new HashMap<CarrierShipment,Id>();
	
	private HashMap<CarrierShipment,Id> toCustomers = new HashMap<CarrierShipment,Id>();
	
	private HashMap<Id,CarrierShipment> shipments = new HashMap<Id,CarrierShipment>();
	
	private HashMap<Id,Customer> customers = new HashMap<Id,Customer>();
	
	private HashMap<Id,Node> nodes = new HashMap<Id,Node>();
	
	private Integer customerCounter = 0;

	private Locations locations;
	
	/**
	 * Locations contains all required customer locations, which basically are the coordinates of customer locations.
	 * It was determined that customers live
	 * @param locations
	 */
	
	public MatSim2VRPTransformation(Locations locations) {
		super();
		this.locations = locations;
	}
	
	void addAndCreateCustomer(String customerId, Id linkId, int demand, double startTime, double endTime, double serviceTime){
		addAndCreateCustomer(makeId(customerId), linkId, demand, startTime, endTime, serviceTime);
	}
	
	void addAndCreateCustomer(Id customerId, Id linkId, int demand, double startTime, double endTime, double serviceTime){
		Id nodeId = linkId;
		Node node = makeNode(nodeId);
		Customer customer = VrpUtils.createCustomer(customerId.toString(), node, demand, startTime, endTime, serviceTime);
		nodes.put(nodeId, node);
		customers.put(customerId, customer);
	}

	void addEnRoutePickupAndDeliveryShipment(CarrierShipment shipment){
		Customer fromCustomer = makeFromCustomer(shipment);
		Customer toCustomer = makeToCustomer(shipment);
		fromCustomer.setRelation(VrpUtils.createRelation(toCustomer));
		toCustomer.setRelation(VrpUtils.createRelation(fromCustomer));
		updateMaps(shipment, fromCustomer, toCustomer);
	}
	
	void addDeliveryFromDepotShipment(CarrierShipment shipment){
		Customer toCustomer = makeToCustomer(shipment);
		customers.put(makeId(toCustomer.getId()),toCustomer);
		toCustomers.put(shipment, makeId(toCustomer.getId()));
		shipments.put(makeId(toCustomer.getId()), shipment);	
	}
	
	void addPickupForDepotShipment(CarrierShipment shipment){
		Customer fromCustomer = makeFromCustomer(shipment);
		customers.put(makeId(fromCustomer.getId()),fromCustomer);
		fromCustomers.put(shipment, makeId(fromCustomer.getId()));
		shipments.put(makeId(fromCustomer.getId()), shipment);
	}
	
	void removeShipment(Shipment shipment){
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

	public CarrierShipment getShipment(Id customerId){
		return shipments.get(customerId);
	}

	public Collection<Customer> getCustomers() {
		return Collections.unmodifiableCollection(customers.values());
	}
	
	public Customer getCustomer(Id customerId){
		return customers.get(customerId);
	}

	void clear(){
		fromCustomers.clear();
		toCustomers.clear();
		shipments.clear();
		customers.clear();
		nodes.clear();
	}

	private void updateMaps(CarrierShipment shipment, Customer fromCustomer, Customer toCustomer) {
		customers.put(makeId(fromCustomer.getId()),fromCustomer);
		customers.put(makeId(toCustomer.getId()),toCustomer);
		toCustomers.put(shipment, makeId(toCustomer.getId()));
		fromCustomers.put(shipment, makeId(fromCustomer.getId()));
		shipments.put(makeId(fromCustomer.getId()), shipment);
		shipments.put(makeId(toCustomer.getId()), shipment);
	}

	String createCustomerId(){
		customerCounter++;
		return customerCounter.toString();
	}
	
	private Customer makeToCustomer(Shipment shipment) {
		Id id = shipment.getTo();
		Node node = makeNode(id);
		Customer customer = VrpUtils.createCustomer(createCustomerId(), node, shipment.getSize()*-1, 
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
			if(locations.getCoord(id) != null){
				node.setCoord(makeCoordinate(locations.getCoord(id)));
			}
			nodes.put(makeId(node.getId()), node);
		}
		return node;
	}

	private Coordinate makeCoordinate(Coord coord) {
		return new Coordinate(coord.getX(),coord.getY());
	}

	private Id makeId(String customerCounter) {
		return new IdImpl(customerCounter);
	}

	private Customer makeFromCustomer(Shipment shipment) {
		Id id = shipment.getFrom();
		Node node = makeNode(id);
		Customer customer = VrpUtils.createCustomer(createCustomerId(), node, shipment.getSize(), 
				shipment.getPickupTimeWindow().getStart(), shipment.getPickupTimeWindow().getEnd(), 0.0);	
		return customer;
	}
}
