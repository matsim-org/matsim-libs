/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
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
package vrp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.basics.RRTourAgentFactory;
import vrp.algorithms.ruinAndRecreate.basics.Solution;
import vrp.algorithms.ruinAndRecreate.constraints.CapacityPickupsDeliveriesSequenceConstraint;
import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.Node;
import vrp.api.SingleDepotVRP;
import vrp.api.VRP;
import vrp.basics.Coordinate;
import vrp.basics.CustomerImpl;
import vrp.basics.ManhattanDistance;
import vrp.basics.NodeImpl;
import vrp.basics.Nodes;
import vrp.basics.Relation;
import vrp.basics.SingleDepotSolutionFactoryImpl;
import vrp.basics.SingleDepotVRPBuilder;
import vrp.basics.Tour;
import vrp.basics.TourActivityFactory;
import vrp.basics.VehicleType;
import vrp.basics.VrpUtils;

public class VRPTestCase extends TestCase{
	
	public Tour tour;
	
	public Costs costs;
	
	public Nodes nodes;
	
	public List<Customer> customers;
	
	public Map<String,Customer> customerMap;
	
	public Constraints constraints;
	
	public boolean init = false;
	
	
	/*
	 * 	|
	 * 10	|
	 * 9|
	 * 8|
	 * 7|
	 * 6|
	 * 5|
	 * 4|
	 * 3|
	 * 2|
	 * 1|_________________________________
	 *   1 2 3 4 5 6 7 8 9 10
	 * depotNode=0,0
	 * node(1,0)=1,0 --> 0+1=1=ungerade => delivery => demand=-1
	 * node(1,1)=1,1 --> 1+1=2=gerade => pickup => demand=+1
	 *   
	 */
	
	protected void initCustomersInPlainCoordinateSystem(){
		costs = new ManhattanDistance();
		nodes = new Nodes();
		customers = new ArrayList<Customer>();
		customerMap = new HashMap<String, Customer>();
		createNodesAndCustomer();
		constraints = new CapacityPickupsDeliveriesSequenceConstraint(20);
		init = true;
		
	}
	
	protected SingleDepotVRP getVRP(int capacity){
		if(!init){
			initCustomersInPlainCoordinateSystem();
			init = true;
		}
		Customer depot1 = customerMap.get(makeId(0,0));
		depot1.setDemand(0);
		SingleDepotVRPBuilder vrpBuilder = new SingleDepotVRPBuilder();
		vrpBuilder.addCustomer(depot1);
		vrpBuilder.setDepot(depot1);
		vrpBuilder.addCustomer(customerMap.get(makeId(0,10)));
		vrpBuilder.addCustomer(customerMap.get(makeId(10,10)));
		Customer c1 = customerMap.get(makeId(1,4));
		vrpBuilder.addCustomer(c1);
		Customer c2 = customerMap.get(makeId(1,5));
		vrpBuilder.addCustomer(c2);
		setRelation(c1,c2);
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(constraints);
		vrpBuilder.setVehicleType(new VehicleType(capacity));
		return vrpBuilder.buildVRP();
	}
	
	protected Solution getInitialSolution(SingleDepotVRP vrp){
		Collection<Tour> tours = new SingleDepotSolutionFactoryImpl().createInitialSolution(vrp);
		Collection<TourAgent> agents = new ArrayList<TourAgent>();
		for(Tour t : tours){
			VehicleType type = vrp.getVehicleType();
			TourAgent a = new RRTourAgentFactory(vrp).createTourAgent(t, VrpUtils.createVehicle(type));
			agents.add(a);
		}
		return new Solution(agents);
	}
	
	protected Customer getDepot(){
		return customerMap.get(makeId(0,0));
	}
	
	protected void setRelation(Customer c1, Customer c2){
		c1.setRelation(new Relation(c2));
		c2.setRelation(new Relation(c1));
	}
	
	protected TourAgent getTourAgent(VRP vrp, Tour tour1, VehicleType type1) {
		return new RRTourAgentFactory(vrp).createTourAgent(tour1, VrpUtils.createVehicle(type1));
	}
	
	protected Tour makeTour(Collection<Customer> tourSequence){
		Tour tour = new Tour();
		TourActivityFactory activityFactory = new TourActivityFactory();
		for(Customer c : tourSequence){
			tour.getActivities().add(activityFactory.createTourActivity(c));
		}
		return tour;
	}

	private void createNodesAndCustomer() {
		for(int i=0;i<11;i++){
			for(int j=0;j<11;j++){
				String nodeId = makeId(i,j);
				Node node = makeNode(nodeId);
				node.setCoord(makeCoord(i,j));
				nodes.getNodes().put(nodeId, node);
				Customer customer = makeCustomer(node);
				if(i == 0 && j == 0){
					customer.setDemand(0);
				}
				else if((i+j) % 2 == 0){
					customer.setDemand(1);
				}
				else{
					customer.setDemand(-1);
				}
				customers.add(customer);
				customerMap.put(customer.getId(), customer);
			}
		}
	}

	private Customer makeCustomer(Node node) {
		Customer customer = new CustomerImpl(node.getId(),node);
		return customer;
	}

	private Coordinate makeCoord(int i, int j) {
		return new Coordinate(i,j);
	}

	private Node makeNode(String nodeId) {
		return new NodeImpl(nodeId);
	}

	protected String makeId(int i, int j) {
		return i + "," + j;
	}

}
