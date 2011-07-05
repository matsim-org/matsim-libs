package freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.Offer;
import playground.mzilske.freight.OfferMaker;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour.TourElement;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreate;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreateFactory;
import vrp.algorithms.ruinAndRecreate.constraints.CapacityConstraint;
import vrp.api.Costs;
import vrp.api.VRP;
import vrp.basics.Tour;
import city2000w.VrpBuilder;

public class RuinAndRecreateOfferMaker implements OfferMaker{

	private CarrierImpl carrier;
	
	private Costs costs;
	
	private CarrierVehicle carrierVehicle;
	
	private Network network;
	
	public RuinAndRecreateOfferMaker(CarrierImpl carrier, Network network) {
		super();
		this.carrier = carrier;
		carrierVehicle = carrier.getCarrierCapabilities().getCarrierVehicles().iterator().next();
		this.network = network;
	}
	
	public void setCosts(Costs costs) {
		this.costs = costs;
	}

	
	
	@Override
	public Offer requestOffer(Id linkId, Id linkId2, int shipmentSize, Double memorizedPrice) {
		return null;
	}

	@Override
	public Offer requestOffer(Id from, Id to, int size, Double startPickup, Double endPickup, Double startDelivery, Double endDelivery, double memorizedPrice) {
		VrpBuilder vrpBuilder = new VrpBuilder(carrierVehicle.getLocation(), network);
		vrpBuilder.setConstraints(new CapacityConstraint(carrierVehicle.getCapacity()));
		VRPTransformation vrpTrafo = new VRPTransformation(network);
		for(Contract c : carrier.getContracts()){
			Shipment s = c.getShipment();
			vrpTrafo.addShipment(s);
		}
		Shipment requestedShipment = CarrierUtils.createShipment(from, to, size, startPickup, endPickup, startDelivery, endDelivery);
		vrpTrafo.addShipment(requestedShipment);
		VRP vrp = vrpBuilder.buildVrp();
		RuinAndRecreateFactory rrFactory = new RuinAndRecreateFactory();
		Collection<vrp.basics.Tour> initialSolution = createInitialSolution(carrier.getSelectedPlan(), requestedShipment);
		RuinAndRecreate ruinAndRecreateAlgo = rrFactory.createStandardAlgo(vrp, initialSolution, carrierVehicle.getCapacity());
		ruinAndRecreateAlgo.setWarmUpIterations(0);
		ruinAndRecreateAlgo.run();
		return null;
	}

	private Collection<Tour> createInitialSolution(CarrierPlan selectedPlan, Shipment requestedShipment) {
		Collection<Tour> tours = new ArrayList<Tour>();
		for(ScheduledTour t : selectedPlan.getScheduledTours()){
			tours.add(makeTour(t.getTour()));
		}
		return tours;
	}

	private Tour makeTour(playground.mzilske.freight.Tour tour) {
		for(TourElement tE : tour.getTourElements()){
			
		}
		return null;
	}
}
