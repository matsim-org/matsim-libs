package vrp.algorithms.ruinAndRecreate.recreation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

import vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer;
import vrp.algorithms.ruinAndRecreate.api.RecreationStrategy;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.api.TourAgentFactory;
import vrp.algorithms.ruinAndRecreate.basics.Shipment;
import vrp.algorithms.ruinAndRecreate.basics.Solution;
import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.Tour;
import vrp.basics.VrpUtils;


/**
 * Simplest recreation strategy. All removed customers are inserted where insertion costs are minimal. I.e. each tour-agent is asked for
 * minimal marginal insertion costs. The tour-agent offering the lowest marginal insertion costs gets the customer.
 * 
 * @author stefan schroeder
 *
 */

public class BestInsertion implements RecreationStrategy{

	private Logger logger = Logger.getLogger(BestInsertion.class);
	
	private VRP vrp;
	
	private int newVehicleCapacity;
	
	private Customer depot;
	
	private TourAgentFactory tourAgentFactory;
	
	public BestInsertion(VRP vrp) {
		super();
		this.vrp = vrp;
		this.depot = vrp.getDepot();
	}
	
	public void setTourAgentFactory(TourAgentFactory tourAgentFactory) {
		this.tourAgentFactory = tourAgentFactory;
	}

	public void run(Solution tentativeSolution, List<Shipment> shipmentsWithoutService) {
//		List<Customer> customersWithoutService = getCustomers(shipmentsWithoutService);
		Collections.shuffle(shipmentsWithoutService, MatsimRandom.getRandom());
		for(Shipment shipmentWithoutService : shipmentsWithoutService){
			Offer bestOffer = null;
			for(TourAgent agent : tentativeSolution.getTourAgents()){
				Offer offer = agent.requestService(shipmentWithoutService);
//				logger.debug(offer);
				if(offer == null){
					continue;
				}
				if(bestOffer == null){
					bestOffer = offer;
				}
				else if(offer.getCost() < bestOffer.getCost()){
					bestOffer = offer;
				}
			}
			if(bestOffer != null){
				logger.debug("offer granted " + bestOffer.getAgent() + " " + bestOffer + " " + shipmentWithoutService);
				bestOffer.getAgent().offerGranted(shipmentWithoutService);
			}
			else{
				TourAgent newTourAgent = createTourAgent(shipmentWithoutService);
				if(newTourAgent.tourIsValid()){
					tentativeSolution.getTourAgents().add(newTourAgent);
				}
				else{
					throw new IllegalStateException("could not create a valid round-tour" + newTourAgent);
				}
				
			}
		}
	}

	private TourAgent createTourAgent(Shipment shipmentWithoutService) {
		Tour tour = null;
		if(isDepot(shipmentWithoutService.getFrom())){
			tour = tourAgentFactory.createRoundTour(depot, shipmentWithoutService.getTo());
		}
		else if(isDepot(shipmentWithoutService.getTo())){
			tour = tourAgentFactory.createRoundTour(depot, shipmentWithoutService.getFrom());
		}
		else{
			tour = tourAgentFactory.createRoundTour(depot, shipmentWithoutService.getFrom(), shipmentWithoutService.getTo());
		}
		TourAgent agent = tourAgentFactory.createTourAgent(vrp, tour, VrpUtils.createVehicle(newVehicleCapacity));
		return agent;
	}

	private List<Customer> getCustomers(List<Shipment> shipmentsWithoutService) {
		List<Customer> customersWithoutService = new ArrayList<Customer>();
		for(Shipment s : shipmentsWithoutService){
			if(isDepot(s.getFrom())){
				customersWithoutService.add(s.getTo());
			}
			else{
				customersWithoutService.add(s.getFrom());
			}
		}
		return customersWithoutService;
	}

	private boolean isDepot(Customer customer) {
		if(customer.getId().equals(vrp.getDepotId())){
			return true;
		}
		return false;
	}

	public void setNewVehicleCapacity(int newVehicleCapacity) {
		this.newVehicleCapacity = newVehicleCapacity;
	}

	private TourAgent createVehicleAgent(Customer n) {
		Tour tour = tourAgentFactory.createRoundTour(depot,n);
		TourAgent agent = tourAgentFactory.createTourAgent(vrp, tour, VrpUtils.createVehicle(newVehicleCapacity));
		return agent;
	}

}
