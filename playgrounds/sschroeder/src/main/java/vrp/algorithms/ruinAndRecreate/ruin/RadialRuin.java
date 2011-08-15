package vrp.algorithms.ruinAndRecreate.ruin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;

import vrp.algorithms.ruinAndRecreate.api.RuinStrategy;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.basics.Shipment;
import vrp.algorithms.ruinAndRecreate.basics.Solution;
import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.VrpUtils;



/**
 * Radial ruin chooses a random customer and ruins current solution in its neighborhood. Thus, it supports local search for a better solution.
 * 
 * @author stefan schroeder
 *
 */

public class RadialRuin implements RuinStrategy {

	static class ReferencedCustomer {
		private Customer customer;
		private double distance;
		
		public ReferencedCustomer(Customer customer, double distance) {
			super();
			this.customer = customer;
			this.distance = distance;
		}

		public Customer getCustomer() {
			return customer;
		}

		public double getDistance() {
			return distance;
		}
	}
	
	private Logger logger = Logger.getLogger(RadialRuin.class);
	
	private VRP vrp;
	
	private double fractionOfAllNodes2beRuined;
	
	private Map<Id,TreeSet<ReferencedCustomer>> distanceNodeTree = new HashMap<Id,TreeSet<ReferencedCustomer>>();
	
	private List<Shipment> shipmentsWithoutService = new ArrayList<Shipment>();
	
	private List<Customer> customerWithoutDepot = new ArrayList<Customer>();
	
	public RadialRuin(VRP vrp) {
		super();
		this.vrp = vrp;
		makeNodeDataStructure();
	}

	public void setFractionOfAllNodes(double fractionOfAllNodes) {
		this.fractionOfAllNodes2beRuined = fractionOfAllNodes;
	}

	private void makeNodeDataStructure() {
		getCustomerWithoutDepot();
		for(Customer origin : customerWithoutDepot){
			TreeSet<ReferencedCustomer> treeSet = new TreeSet<ReferencedCustomer>(new Comparator<ReferencedCustomer>() {
				public int compare(ReferencedCustomer o1, ReferencedCustomer o2) {
					if(o1.getDistance() <= o2.getDistance()){
						return 1;
					}
					else{
						return -1;
					}
				}
			});
			distanceNodeTree.put(origin.getId(), treeSet);
			for(Customer destination : customerWithoutDepot){
				double distance = vrp.getCosts().getCost(origin.getLocation(), destination.getLocation());
				ReferencedCustomer refNode = new ReferencedCustomer(destination, distance);
				treeSet.add(refNode);
			}
		}
	}

	private void getCustomerWithoutDepot() {
		for(Customer c : vrp.getCustomers().values()){
			if(!isDepot(c)){
				customerWithoutDepot.add(c);
			}
		}
	}

	private boolean isDepot(Customer c) {
		if(vrp.getDepots().containsKey(c.getId())){
			return true;
		}
		return false;
	}

	public void run(Solution initialSolution) {
		clear();
		int nOfNodes2BeRemoved = selectNumberOfNearestNeighbors();
		if(nOfNodes2BeRemoved == 0){
			return;
		}
		Customer randomCustomer = pickRandomCustomer();
		logger.info("randCust: " + randomCustomer);
		TreeSet<ReferencedCustomer> tree = distanceNodeTree.get(randomCustomer.getId());
		Iterator<ReferencedCustomer> descendingIterator = tree.descendingIterator();
		int counter = 0;
		List<TourAgent> agent2BeRemoved = new ArrayList<TourAgent>();
		Set<Id> removedCustomers = new HashSet<Id>();
		while(descendingIterator.hasNext() && counter<nOfNodes2BeRemoved){
			ReferencedCustomer refNode = descendingIterator.next();
			Customer customer = refNode.getCustomer();
			logger.info("remCust: " + customer);
			if(removedCustomers.contains(customer.getId())){
				continue;
			}
			for(TourAgent agent : initialSolution.getTourAgents()){
				if(agent.hasCustomer(customer)){
					Shipment shipment = null;
					agent.removeCustomer(customer);
					removedCustomers.add(customer.getId());
					if(customer.hasRelation()){
						Customer relatedCustomer = customer.getRelation().getCustomer();
						agent.removeCustomer(relatedCustomer);
						removedCustomers.add(relatedCustomer.getId());
						shipment = makeShipment(customer, relatedCustomer);
					}
					else{
						shipment = makeShipment(customer);
					}
					shipmentsWithoutService.add(shipment);
					if(agent.getTourSize() < 3){
						agent2BeRemoved.add(agent);
					}
				}
			}
			counter++;
		}
		for(TourAgent vA : agent2BeRemoved){
			initialSolution.getTourAgents().remove(vA);
		}
	}
	
	private Shipment makeShipment(Customer customer, Customer relatedCustomer) {
		Shipment shipment = null;
		if(customer.getDemand() < 0){
			shipment = VrpUtils.createShipment(relatedCustomer, customer);
		}
		else{
			shipment = VrpUtils.createShipment(customer, relatedCustomer);
		}
		return shipment;
	}

	private Shipment makeShipment(Customer customer) {
		Shipment shipment = null;
		if(customer.getDemand() < 0){
			shipment = VrpUtils.createShipment(vrp.getDepot(), customer);
		}
		else{
			shipment = VrpUtils.createShipment(customer, vrp.getDepot());
		}
		return shipment;
	}

	private Customer pickRandomCustomer() {
		int totNuOfNodes = customerWithoutDepot.size();
		int randomIndex = MatsimRandom.getRandom().nextInt(totNuOfNodes);
		Customer customer = customerWithoutDepot.get(randomIndex);
		return customer;
	}

	private void clear() {
		shipmentsWithoutService.clear();
	}

	private int selectNumberOfNearestNeighbors(){
		return (int)Math.round(customerWithoutDepot.size()*fractionOfAllNodes2beRuined);
	}

	public List<Shipment> getShipmentsWithoutService() {
		return shipmentsWithoutService;
	}
}
