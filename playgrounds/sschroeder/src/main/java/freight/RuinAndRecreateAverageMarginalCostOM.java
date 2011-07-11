package freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.Offer;
import playground.mzilske.freight.OfferMaker;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour.Delivery;
import playground.mzilske.freight.Tour.Pickup;
import playground.mzilske.freight.Tour.TourElement;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreate;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreateFactory;
import vrp.algorithms.ruinAndRecreate.constraints.CapacityConstraint;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.Tour;
import vrp.basics.VrpUtils;
import city2000w.MarginalCostListener;
import city2000w.VrpBuilder;

public class RuinAndRecreateAverageMarginalCostOM implements OfferMaker{

	private CarrierImpl carrier;
	
	private CarrierVehicle carrierVehicle;

	private Locations locations;
	
	private MarginalCostListener marginalCostListener;
	
	public RuinAndRecreateAverageMarginalCostOM(CarrierImpl carrier, Locations locations) {
		super();
		this.carrier = carrier;
		carrierVehicle = carrier.getCarrierCapabilities().getCarrierVehicles().iterator().next();
		this.locations = locations;
	}
	
	public void setCosts(Costs costs) {
	}
	
	@Override
	public Offer requestOffer(Id linkId, Id linkId2, int shipmentSize, Double memorizedPrice) {
		return null;
	}

	@Override
	public Offer requestOffer(Id from, Id to, int size, Double startPickup, Double endPickup, Double startDelivery, Double endDelivery, Double memorizedPrice) {
		VRPTransformation vrpTransformation = new VRPTransformation(locations);
		Id depotId = carrierVehicle.getLocation();
		VrpBuilder vrpBuilder = new VrpBuilder(depotId);
		vrpBuilder.setConstraints(new CapacityConstraint(carrierVehicle.getCapacity()));
		for(Contract c : carrier.getContracts()){
			Shipment s = c.getShipment();
			vrpTransformation.addShipment(s);
		}
		Shipment requestedShipment = CarrierUtils.createShipment(from, to, size, startPickup, endPickup, startDelivery, endDelivery);
		vrpTransformation.addShipment(requestedShipment);
		vrpBuilder.setVrpTransformation(vrpTransformation);
		VRP vrp = vrpBuilder.buildVrp();
		RuinAndRecreateFactory rrFactory = new RuinAndRecreateFactory();
		MarginalCostListener marginalCostListener = new MarginalCostListener();
		rrFactory.addRecreationListener(marginalCostListener);
		Collection<Tour> initialSolution = createInitialSolution(carrier.getSelectedPlan(), requestedShipment, vrpTransformation);
		RuinAndRecreate ruinAndRecreateAlgo = rrFactory.createStandardAlgo(vrp, initialSolution, carrierVehicle.getCapacity());
		ruinAndRecreateAlgo.run();
		double totalTransportCost = ruinAndRecreateAlgo.getVrpSolution().getTransportCosts();
		double costOfRequestedShipment = getAverageMC(requestedShipment,marginalCostListener,totalTransportCost,vrpTransformation);
		if(carrier.getId().toString().equals("daxlanden-carrier")){
			breakpoint();
		}
		Offer offer = new Offer();
		offer.setCarrierId(carrier.getId());
		offer.setPrice(costOfRequestedShipment);
		return offer;
	}

	private void breakpoint() {
		// TODO Auto-generated method stub
		
	}

	private double getAverageMC(Shipment requestedShipment, MarginalCostListener marginalCostListener, double totalTransportCost, VRPTransformation vrpTransformation) {
		Customer toCustomer = vrpTransformation.getToCustomer(requestedShipment);
		Map<Customer,Double> shares = new HashMap<Customer, Double>();
		double sOfAvgMC = 0.0;
		int relationCounter = 0;
		for(Customer to : marginalCostListener.getCostRecorder().keySet()){
			relationCounter++;
			sOfAvgMC += marginalCostListener.getCostRecorder().get(to).avgMc;
		}
		if(relationCounter == 0){
			return totalTransportCost;
		}
		if(sOfAvgMC == 0.0){
			return totalTransportCost/(double)relationCounter;
		}
		for(Customer to : marginalCostListener.getCostRecorder().keySet()){
			double share = marginalCostListener.getCostRecorder().get(to).avgMc/sOfAvgMC;
			shares.put(to, share);
		}
		return totalTransportCost*shares.get(toCustomer);
	}

	/**
	 * transformation tourFromSelectedPlan to vrp-tour
	 * @param selectedPlan
	 * @param requestedShipment
	 * @param vrpTrafo 
	 * @return
	 */
	private Collection<Tour> createInitialSolution(CarrierPlan selectedPlan, Shipment requestedShipment, VRPTransformation vrpTrafo) {
		Collection<Tour> tours = new ArrayList<Tour>();
		//startLink --> vrpCustomer
		//annahme: startLink = "depot"
		if(selectedPlan != null){
			createVrpRoundToursFromExistingPlan(selectedPlan, vrpTrafo, tours);
		}
		createVrpRoundToursForRequestedShipment(requestedShipment,vrpTrafo,tours);
		return tours;
	}

	private void createVrpRoundToursForRequestedShipment(Shipment requestedShipment, VRPTransformation vrpTrafo, Collection<Tour> tours) {
		Customer depot = vrpTrafo.getCustomer(makeId("depot"));
		Customer from = vrpTrafo.getFromCustomer(requestedShipment);
		Customer to = vrpTrafo.getToCustomer(requestedShipment);
		Tour tour = VrpUtils.createRoundTour(depot, from, to);
		tours.add(tour);
	}

	private void createVrpRoundToursFromExistingPlan(CarrierPlan selectedPlan,
			VRPTransformation vrpTrafo, Collection<Tour> tours) {
		for(ScheduledTour t : selectedPlan.getScheduledTours()){
			tours.add(makeTour(t.getTour(),vrpTrafo));
		}
	}

	private Tour makeTour(playground.mzilske.freight.Tour tour, VRPTransformation vrpTrafo) {
		Tour vrpTour = new Tour();
		Customer depotCustomer = vrpTrafo.getCustomer(makeId("depot"));
		vrpTour.getActivities().add(VrpUtils.createTourActivity(depotCustomer));
		for(TourElement tE : tour.getTourElements()){
			if(tE instanceof Pickup){
				Customer c = vrpTrafo.getFromCustomer(tE.getShipment());
				vrpTour.getActivities().add(VrpUtils.createTourActivity(c));
			}
			if(tE instanceof Delivery){
				Customer c = vrpTrafo.getToCustomer(tE.getShipment());
				vrpTour.getActivities().add(VrpUtils.createTourActivity(c));
			}
		}
		vrpTour.getActivities().add(VrpUtils.createTourActivity(depotCustomer));
		return vrpTour;
	}

	private Id makeId(String string) {
		return new IdImpl(string);
	}
}
