package vrp.algorithms.ruinAndRecreate.ruin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

import vrp.algorithms.ruinAndRecreate.api.RuinStrategy;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.basics.Shipment;
import vrp.algorithms.ruinAndRecreate.basics.Solution;
import vrp.api.Customer;
import vrp.api.Node;
import vrp.api.VRP;
import vrp.basics.VrpUtils;



/**
 * In the random-ruin strategy, current solution is ruined randomly. I.e. customer are removed randomly from current solution.
 * 
 * @author stefan schroeder
 *
 */

public class RandomRuin implements RuinStrategy {
	
	private Logger logger = Logger.getLogger(RadialRuin.class);

	private VRP vrp;

	private double fractionOfAllNodes2beRuined;
	
	private List<Shipment> shipmentsWithoutService = new ArrayList<Shipment>();
	
	private List<Customer> customersWithoutDepot = new ArrayList<Customer>();
	
	public RandomRuin(VRP vrp) {
		super();
		this.vrp = vrp;
		makeCustomersWithoutDepot();
	}

	private void makeCustomersWithoutDepot() {
		for(Customer c : vrp.getCustomers().values()){
			if(!isDepot(c)){
				customersWithoutDepot.add(c);
			}
		}
		
	}

	private boolean isDepot(Customer c) {
		if(c == vrp.getDepot()){
			return true;
		}
		return false;
	}

	public void run(Solution initialSolution) {
		clear();
		int nOfCustomers2BeRemoved = selectNumberOfNearestNeighbors();
		List<Customer> customerList = copyList(customersWithoutDepot);
		List<TourAgent> agent2BeRemoved = new ArrayList<TourAgent>();
		for(int i=0;i<nOfCustomers2BeRemoved;i++){
			Customer customer = pickRandomCustomer(customerList);
			for(TourAgent agent : initialSolution.getTourAgents()){
				if(agent.hasCustomer(customer)){
					Shipment shipment = null;
					agent.removeCustomer(customer);
					customerList.remove(customer);
					if(customer.hasRelation()){
						Customer relatedCustomer = customer.getRelation().getCustomer();
						agent.removeCustomer(relatedCustomer);
						customerList.remove(relatedCustomer);
						shipment = makeShipment(customer, relatedCustomer);
					}
					else{
						shipment = makeShipment(customer);
					}
					shipmentsWithoutService.add(shipment);
					if(agent.getTourSize() < 3){
						agent2BeRemoved.add(agent);
					}
					break;
				}
			}
		}
		for(TourAgent vA : agent2BeRemoved){
			initialSolution.getTourAgents().remove(vA);
		}
	}

	private List<Customer> copyList(Collection<Customer> collection) {
		List<Customer> customerList = new ArrayList<Customer>(collection);
		return customerList;
	}

	public void setFractionOfAllNodes2beRuined(double fractionOfAllNodes2beRuined) {
		this.fractionOfAllNodes2beRuined = fractionOfAllNodes2beRuined;
	}
	
	private int selectNumberOfNearestNeighbors(){
		return (int)Math.round(customersWithoutDepot.size()*fractionOfAllNodes2beRuined);
	}
	
	private Customer pickRandomCustomer(List<Customer> customerList) {
		int totNuOfNodes = customerList.size();
		int randomIndex = MatsimRandom.getRandom().nextInt(totNuOfNodes);
		Customer customer = customerList.get(randomIndex);
		return customer;
	}
	
	private void clear() {
		shipmentsWithoutService.clear();
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

	public List<Shipment> getShipmentsWithoutService() {
		return shipmentsWithoutService;
	}

}
