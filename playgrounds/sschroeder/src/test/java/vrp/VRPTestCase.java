package vrp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.basics.RRTourAgentFactory;
import vrp.algorithms.ruinAndRecreate.basics.Solution;
import vrp.algorithms.ruinAndRecreate.constraints.CapacityPickupsDeliveriesSequenceConstraint;
import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.Node;
import vrp.api.VRP;
import vrp.basics.CustomerImpl;
import vrp.basics.ManhattanDistance;
import vrp.basics.MultipleDepotsInitialSolutionFactory;
import vrp.basics.NodeImpl;
import vrp.basics.Nodes;
import vrp.basics.Relation;
import vrp.basics.Tour;
import vrp.basics.TourActivityFactory;
import vrp.basics.VRPBuilder;
import vrp.basics.VehicleType;
import vrp.basics.VrpUtils;

public class VRPTestCase extends TestCase{
	
	public Tour tour;
	
	public Costs costs;
	
	public Nodes nodes;
	
	public List<Customer> customers;
	
	public Map<Id,Customer> customerMap;
	
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
	
	protected void init(){
		costs = new ManhattanDistance();
		nodes = new Nodes();
		customers = new ArrayList<Customer>();
		customerMap = new HashMap<Id, Customer>();
		createNodesAndCustomer();
		constraints = new CapacityPickupsDeliveriesSequenceConstraint(20);
		init = true;
		
	}
	
	protected VRP getVRP(){
		if(!init){
			init();
			init = true;
		}
		Customer depot1 = customerMap.get(makeId(0,0));
		depot1.setDemand(0);
		Customer depot2 = customerMap.get(makeId(10,0));
		depot2.setDemand(0);
		VRPBuilder vrpBuilder = new VRPBuilder();
		vrpBuilder.addCustomer(depot1, true);
		vrpBuilder.addCustomer(depot2, true);
		vrpBuilder.addCustomer(customerMap.get(makeId(0,10)), false);
		vrpBuilder.addCustomer(customerMap.get(makeId(10,10)), false);
		Customer c1 = customerMap.get(makeId(1,4));
		vrpBuilder.addCustomer(c1, false);
		Customer c2 = customerMap.get(makeId(1,5));
		vrpBuilder.addCustomer(c2, false);
		setRelation(c1,c2);
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(constraints);
		vrpBuilder.assignVehicleType(depot1.getId(), new VehicleType(2));
		vrpBuilder.assignVehicleType(depot2.getId(), new VehicleType(2));
		return vrpBuilder.buildVRP();
	}
	
	protected Solution getInitialSolution(VRP vrp){
		Collection<Tour> tours = new MultipleDepotsInitialSolutionFactory().createInitialSolution(vrp);
		Collection<TourAgent> agents = new ArrayList<TourAgent>();
		for(Tour t : tours){
			VehicleType type = vrp.getVehicleType(t.getActivities().get(0).getCustomer().getId());
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
				Id nodeId = makeId(i,j);
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

	private Coord makeCoord(int i, int j) {
		return new CoordImpl(i,j);
	}

	private Node makeNode(Id nodeId) {
		return new NodeImpl(nodeId);
	}

	protected Id makeId(int i, int j) {
		return new IdImpl(i + "," + j);
	}

}
