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

import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.Node;
import vrp.basics.CustomerImpl;
import vrp.basics.ManhattanDistance;
import vrp.basics.NodeImpl;
import vrp.basics.Nodes;
import vrp.basics.Tour;
import vrp.basics.TourActivityFactory;

public class VRPTestCase extends TestCase{
	
	public Tour tour;
	
	public Costs costs;
	
	public Nodes nodes;
	
	public List<Customer> customers;
	
	public Map<Id,Customer> customerMap;
	
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
	}
	
	protected Customer getDepot(){
		return customerMap.get(makeId(0,0));
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
