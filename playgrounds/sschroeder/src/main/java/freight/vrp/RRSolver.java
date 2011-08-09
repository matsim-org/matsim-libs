package freight.vrp;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour;
import playground.mzilske.freight.TourBuilder;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreate;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreateFactory;
import vrp.algorithms.ruinAndRecreate.constraints.TimeAndCapacityPickupsDeliveriesSequenceConstraint;
import vrp.api.Constraints;
import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.CrowFlyDistance;
import vrp.basics.TourActivity;

public class RRSolver implements VRPSolver{
	
	private Id depotLocation;
	
	private int capacity;
	
	private VRPTransformation vrpTransformation;
	
	private Network network;
	
	private Collection<Shipment> shipments;
	
	private Collection<CarrierVehicle> vehicles;

	public RRSolver(Collection<Shipment> shipments, Collection<CarrierVehicle> vehicles, Network network) {
		super();
		this.shipments = shipments;
		this.vehicles = vehicles;
		CarrierVehicle vehicle = vehicles.iterator().next();
		this.depotLocation = vehicle.getLocation();
		this.capacity = vehicle.getCapacity();
		this.network = network;
		this.vrpTransformation = makeVRPTransformation();
	}


	@Override
	public void solve(Collection<Contract> contracts,CarrierVehicle carrierVehicle) {
		
	}

	@Override
	public Collection<Tour> solve() {
		RuinAndRecreate ruinAndRecreate = makeAlgorithm();
		ruinAndRecreate.run();
		Collection<Tour> tours = makeVehicleTours(ruinAndRecreate.getSolution());
		return tours;
	}


	private Collection<Tour> makeVehicleTours(Collection<vrp.basics.Tour> vrpSolution) {
		Collection<Tour> tours = new ArrayList<Tour>();
		for(vrp.basics.Tour tour : vrpSolution){
			TourBuilder tourBuilder = new TourBuilder();
			boolean tourStarted = false;
			for(TourActivity act : tour.getActivities()){
				Shipment shipment = getShipment(act.getCustomer());
				if(act instanceof vrp.basics.OtherDepotActivity){
					if(tourStarted){
						tourBuilder.scheduleEnd(act.getCustomer().getLocation().getId());
					}
					else{
						tourStarted = true;
						tourBuilder.scheduleStart(act.getCustomer().getLocation().getId());
					}
				}
				if(act instanceof vrp.basics.EnRouteDelivery){
					tourBuilder.scheduleDelivery(shipment);
				}
				if(act instanceof vrp.basics.EnRoutePickup){
					tourBuilder.schedulePickup(shipment);
				}
			}
			Tour vehicleTour = tourBuilder.build();
			tours.add(vehicleTour);
		}
		return tours;
	}

	private VRPTransformation makeVRPTransformation() {
		return new VRPTransformation(new Locations(){

			@Override
			public Coord getCoord(Id id) {
				return network.getLinks().get(id).getCoord();
			}
			
		});
	}
	
	private Shipment getShipment(Customer customer) {
		return vrpTransformation.getShipment(customer.getId());
	}

	private RuinAndRecreate makeAlgorithm() {
		VrpBuilder vrpBuilder = new VrpBuilder(depotLocation);
		CrowFlyDistance costs = new CrowFlyDistance();
		costs.speed = 25;
		vrpBuilder.setCosts(costs);
		Constraints constraints = new TimeAndCapacityPickupsDeliveriesSequenceConstraint(capacity,8*3600,costs);
		vrpBuilder.setConstraints(constraints);
		for(Shipment s : shipments){
			vrpTransformation.addShipment(s);
		}
		vrpBuilder.setVrpTransformation(vrpTransformation);
		VRP vrp = vrpBuilder.buildVrp();
		RuinAndRecreateFactory rrFactory = new RuinAndRecreateFactory();
		rrFactory.setWarmUp(4);
		rrFactory.setIterations(20);
		Collection<vrp.basics.Tour> initialSolution = new TrivialInitialSolutionFactory(vrp).createInitialSolution();
		RuinAndRecreate ruinAndRecreateAlgo = rrFactory.createStandardAlgo(vrp, initialSolution, capacity);
		return ruinAndRecreateAlgo;
	}

}
