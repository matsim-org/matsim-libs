package vrp.algorithms.ruinAndRecreate.recreation;

import java.util.ArrayList;
import java.util.Collection;
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
import vrp.basics.TourActivity;
import vrp.basics.VrpUtils;


/**
 * Simplest recreation strategy. All removed customers are inserted where insertion costs are minimal. I.e. each tour-agent is asked for
 * minimal marginal insertion costs. The tour-agent offering the lowest marginal insertion costs gets the customer/shipment.
 * 
 * @author stefan schroeder
 *
 */

public class ParallelBestInsertion implements RecreationStrategy{

	private Logger logger = Logger.getLogger(ParallelBestInsertion.class);
	
	private VRP vrp;
	
	private int newVehicleCapacity;
	
	private Customer depot;
	
	private TourAgentFactory tourAgentFactory;
	
	private Collection<RecreationListener> recreationListeners = new ArrayList<RecreationListener>();
	
	public Collection<RecreationListener> getRecreationListener() {
		return recreationListeners;
	}

	public ParallelBestInsertion(VRP vrp) {
		super();
		this.vrp = vrp;
		this.depot = vrp.getDepot();
	}
	
	public void setTourAgentFactory(TourAgentFactory tourAgentFactory) {
		this.tourAgentFactory = tourAgentFactory;
	}

	public void run(Solution tentativeSolution, List<Shipment> shipmentsWithoutService) {
		Collections.shuffle(shipmentsWithoutService, MatsimRandom.getRandom());
		for(Shipment shipmentWithoutService : shipmentsWithoutService){
			doCalculation();
			List<Offer> rejectedOffers = new ArrayList<Offer>();
			Offer bestOffer = null;
			TourAgent bestAgent = null;
			Collection<Thread> threads = new ArrayList<Thread>();
			for(TourAgent agent : tentativeSolution.getTourAgents()){
//				assertAgentDoesNotHaveThisShipment(agent,shipmentWithoutService);
				agent.setNewShipment(VrpUtils.createShipment(shipmentWithoutService.getFrom(), shipmentWithoutService.getTo()));
				Thread agentThread = new Thread(agent);
				threads.add(agentThread);
				agentThread.start();
//				logger.info("thread started");
			}
			for(Thread agentThread : threads){
				try {
					agentThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
					logger.error(e);
					System.exit(1);
				}
			}
			for(TourAgent agent : tentativeSolution.getTourAgents()){
				Offer offer = agent.getOpenOffer();
				if(offer == null){
					continue;
				}
				if(bestOffer == null){
					bestOffer = offer;
					bestAgent = agent;
				}
				else if(offer.getPrice() < bestOffer.getPrice()){
					rejectedOffers.add(bestOffer);
					bestOffer = offer;
					bestAgent = agent;
				}
				else{
					rejectedOffers.add(offer);
				}
				
			}
			for(Offer o : rejectedOffers){
				o.getServiceProvider().offerRejected(o);
				
			}
			if(bestOffer != null){
				logger.debug("offer granted " + bestOffer.getServiceProvider() + " " + bestOffer + " " + shipmentWithoutService);
				bestAgent.offerGranted(shipmentWithoutService);
				informListeners(shipmentWithoutService,bestOffer.getPrice());
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

	private synchronized void doCalculation() {
		
		
	}

	private void assertAgentDoesNotHaveThisShipment(TourAgent agent, Shipment shipmentWithoutService) {
		Customer from = shipmentWithoutService.getFrom();
		Customer to = shipmentWithoutService.getTo();
		for(TourActivity tA : agent.getTourActivities()){
			if(tA.getCustomer().getId().equals(from.getId()) && !isDepot(from)){
				throw new IllegalStateException("this cannot happen. " + agent.getTour() + "; " + shipmentWithoutService);
			}
			if(tA.getCustomer().getId().equals(to.getId()) && !isDepot(to)){
				throw new IllegalStateException("this cannot happen. " + agent.getTour() + "; " + shipmentWithoutService);
			}
		}
	}

	private void informListeners(Shipment shipment, double cost) {
		for(RecreationListener l : recreationListeners){
			l.inform(new RecreationEvent(shipment,cost));
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

	private boolean isDepot(Customer customer) {
		if(customer.getId().equals(vrp.getDepotId())){
			return true;
		}
		return false;
	}

	public void setNewVehicleCapacity(int newVehicleCapacity) {
		this.newVehicleCapacity = newVehicleCapacity;
	}

	@Override
	public void addListener(RecreationListener l) {
		recreationListeners.add(l);	
	}

}
