/*******************************************************************************
 * Copyright (C) 2011 Stefan Schršder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package vrp.algorithms.ruinAndRecreate.ruin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import vrp.algorithms.ruinAndRecreate.api.RuinStrategy;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.basics.Shipment;
import vrp.algorithms.ruinAndRecreate.basics.Solution;
import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.RandomNumberGeneration;
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
	
	private Map<String,TreeSet<ReferencedCustomer>> distanceNodeTree = new HashMap<String,TreeSet<ReferencedCustomer>>();
	
	private List<Shipment> shipmentsWithoutService = new ArrayList<Shipment>();
	
	private List<Customer> customerWithoutDepot = new ArrayList<Customer>();
	
	private Random random = RandomNumberGeneration.getRandom();
	
	public void setRandom(Random random) {
		this.random = random;
	}

	public RadialRuin(VRP vrp) {
		super();
		this.vrp = vrp;
		logger.info("intialise radial ruin");
		makeNodeDataStructure();
		logger.info("done");
	}

	public void setFractionOfAllNodes(double fractionOfAllNodes) {
		this.fractionOfAllNodes2beRuined = fractionOfAllNodes;
	}

	private void makeNodeDataStructure() {
		getCustomerWithoutDepot();
		for(Customer origin : customerWithoutDepot){
			TreeSet<ReferencedCustomer> treeSet = new TreeSet<ReferencedCustomer>(new Comparator<ReferencedCustomer>() {
				@Override
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

	@Override
	public void run(Solution initialSolution) {
		clear();
		int nOfNodes2BeRemoved = selectNumberOfNearestNeighbors();
		if(nOfNodes2BeRemoved == 0){
			return;
		}
		Customer randomCustomer = pickRandomCustomer();
		logger.debug("randCust: " + randomCustomer);
		TreeSet<ReferencedCustomer> tree = distanceNodeTree.get(randomCustomer.getId());
		Iterator<ReferencedCustomer> descendingIterator = tree.descendingIterator();
		int counter = 0;
		List<TourAgent> agent2BeRemoved = new ArrayList<TourAgent>();
		Set<String> removedCustomers = new HashSet<String>();
		while(descendingIterator.hasNext() && counter<nOfNodes2BeRemoved){
			ReferencedCustomer refNode = descendingIterator.next();
			Customer customer = refNode.getCustomer();
			logger.debug("remCust: " + customer);
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
		int randomIndex = random.nextInt(totNuOfNodes);
		Customer customer = customerWithoutDepot.get(randomIndex);
		return customer;
	}

	private void clear() {
		shipmentsWithoutService.clear();
	}

	private int selectNumberOfNearestNeighbors(){
		return (int)Math.round(customerWithoutDepot.size()*fractionOfAllNodes2beRuined);
	}

	@Override
	public List<Shipment> getShipmentsWithoutService() {
		return shipmentsWithoutService;
	}
}
