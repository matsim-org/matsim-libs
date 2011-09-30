package freight.vrp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.CarrierShipment;
import playground.mzilske.freight.carrier.CarrierVehicle;
import playground.mzilske.freight.carrier.Tour;
import playground.mzilske.freight.carrier.TourBuilder;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreate;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreateFactory;
import vrp.algorithms.ruinAndRecreate.constraints.TimeAndCapacityPickupsDeliveriesSequenceConstraint;
import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.CrowFlyDistance;
import vrp.basics.InitialSolutionFactory;
import vrp.basics.MultipleDepotsInitialSolutionFactory;
import vrp.basics.TourActivity;
import vrp.basics.VehicleType;

public class RRPDSolver implements VRPSolver{
	
	private static Logger logger = Logger.getLogger(RRPDSolver.class);
	
	private int capacity;
	
	private VRPTransformation vrpTransformation;
	
	private Network network;
	
	private Collection<CarrierShipment> shipments;
	
	private Collection<CarrierVehicle> vehicles;
	
	private Collection<Id> depots;
	
	private Map<Id,CarrierVehicle> depotCarrierVehicleMap;;

	private InitialSolutionFactory iniSolutionFactory = new MultipleDepotsInitialSolutionFactory();
	
	private Constraints constraints;
	
	private Costs costs;
	
	private int nOfIterations = 20;
	
	private int nOfWarmupIterations = 4;

	public void setConstraints(Constraints constraints) {
		this.constraints = constraints;
	}

	public void setCosts(Costs costs) {
		this.costs = costs;
	}

	public void setnOfIterations(int nOfIterations) {
		this.nOfIterations = nOfIterations;
	}

	public void setnOfWarmupIterations(int nOfWarmupIterations) {
		this.nOfWarmupIterations = nOfWarmupIterations;
	}

	private int depotCounter = 0;
	
	public void setIniSolutionFactory(InitialSolutionFactory iniSolutionFactory) {
		this.iniSolutionFactory = iniSolutionFactory;
	}

	public RRPDSolver(Collection<CarrierShipment> shipments, Collection<CarrierVehicle> vehicles, Network network) {
		super();
		this.shipments = shipments;
		this.vehicles = vehicles;
		this.depots = makeDepots(vehicles);
		this.network = network;
		this.vrpTransformation = makeVRPTransformation();
		costs = new CrowFlyDistance();
		((CrowFlyDistance)costs).speed = 17;
		((CrowFlyDistance)costs).detourFactor = 1.3;
		constraints = new TimeAndCapacityPickupsDeliveriesSequenceConstraint(capacity,8*3600,costs);
	}

	private Collection<Id> makeDepots(Collection<CarrierVehicle> vehicles) {
		depotCarrierVehicleMap = new HashMap<Id, CarrierVehicle>();
		Set<Id> depots = new HashSet<Id>();
		for(CarrierVehicle v : vehicles){
			if(!depots.contains(v.getLocation())){
				depots.add(v.getLocation());
				depotCarrierVehicleMap.put(v.getLocation(), v);
			}
		}
		return depots;
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
			logger.info(tour);
			TourBuilder tourBuilder = new TourBuilder();
			boolean tourStarted = false;
			for(TourActivity act : tour.getActivities()){
				CarrierShipment shipment = getShipment(act.getCustomer());
				if(act instanceof vrp.basics.OtherDepotActivity){
					if(tourStarted){
						tourBuilder.scheduleEnd(makeId(act.getCustomer().getLocation().getId()));
					}
					else{
						tourStarted = true;
						tourBuilder.scheduleStart(makeId(act.getCustomer().getLocation().getId()));
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

	private Id makeId(String id) {
		return new IdImpl(id);
	}

	private VRPTransformation makeVRPTransformation() {
		return new VRPTransformation(new Locations(){

			@Override
			public Coord getCoord(Id id) {
				return network.getLinks().get(id).getCoord();
			}
			
		});
	}
	
	private CarrierShipment getShipment(Customer customer) {
		return vrpTransformation.getShipment(makeId(customer.getId()));
	}

	private RuinAndRecreate makeAlgorithm() {
		VRPWithMultipleDepotsBuilder vrpBuilder = new VRPWithMultipleDepotsBuilder();
		for(Id depotLocation : depots){
			Id depotId = makeDepotId();
			vrpBuilder.addDepot(depotId, depotLocation);
			vrpBuilder.assignVehicleType(depotId, getVehicleType(depotCarrierVehicleMap.get(depotLocation)));
		}
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(constraints);
		for(CarrierShipment s : shipments){
			vrpTransformation.addShipment(s);
		}
		vrpBuilder.setVRPTransformation(vrpTransformation);
		VRP vrp = vrpBuilder.buildVRP();
		RuinAndRecreateFactory rrFactory = new RuinAndRecreateFactory();
		rrFactory.setWarmUp(nOfWarmupIterations);
		rrFactory.setIterations(nOfIterations);
		Collection<vrp.basics.Tour> initialSolution = iniSolutionFactory.createInitialSolution(vrp);
		RuinAndRecreate ruinAndRecreateAlgo = rrFactory.createStandardAlgo(vrp, initialSolution, capacity);
		return ruinAndRecreateAlgo;
	}

	private VehicleType getVehicleType(CarrierVehicle carrierVehicle) {
		return new VehicleType(carrierVehicle.getCapacity());
	}

	private Id makeDepotId() {
		depotCounter++;
		return new IdImpl("depot_" + depotCounter);
	}

	@Override
	public void solve(Collection<CarrierContract> contracts,
			CarrierVehicle carrierVehicle) {
		// TODO Auto-generated method stub
		
	}

}
