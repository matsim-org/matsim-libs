package freight.offermaker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.CarrierCostFunction;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierPlanBuilder;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.CarrierOffer;
import playground.mzilske.freight.OfferMaker;
import playground.mzilske.freight.Shipment;
import city2000w.RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder;
import city2000w.RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder.Schedule;
import freight.CarrierUtils;
import freight.vrp.Locations;
import freight.vrp.RuinAndRecreateSolver;
import freight.vrp.VRPTransformation;

public class RuinAndRecreateAverageMarginalCostOM implements OfferMaker{

	private CarrierImpl carrier;
	
	private CarrierVehicle carrierVehicle;

	private Locations locations;
	
	private Network network;
	
	private CarrierCostFunction carrierCostCalculator;
	
	private CarrierPlanBuilder carrierPlanBuilder;
	
	public void setCarrierCostFunction(CarrierCostFunction carrierCostCalculator) {
		this.carrierCostCalculator = carrierCostCalculator;
	}


	public void setNetwork(Network network) {
		this.network = network;
	}


	public RuinAndRecreateAverageMarginalCostOM(CarrierImpl carrier, Locations locations) {
		super();
		this.carrier = carrier;
		carrierVehicle = carrier.getCarrierCapabilities().getCarrierVehicles().iterator().next();
		this.locations = locations;
	}

	
	@Override
	public CarrierOffer requestOffer(Id linkId, Id linkId2, int shipmentSize, Double memorizedPrice) {
		return null;
	}

	@Override
	public CarrierOffer requestOffer(Id from, Id to, int size, Double startPickup, Double endPickup, Double startDelivery, Double endDelivery, Double memorizedPrice) {
		if(memorizedPrice != null){
//			if(MatsimRandom.getRandom().nextDouble() < 0.8){
				CarrierOffer offer = new CarrierOffer();
				offer.setId(carrier.getId());
				offer.setPrice(memorizedPrice);
				return offer;
//			}
		}
		VRPTransformation vrpTransformation = new VRPTransformation(locations);
		for(Contract c : carrier.getContracts()){
			Shipment s = c.getShipment();
			vrpTransformation.addShipment(s);
		}
		Shipment requestedShipment = CarrierUtils.createShipment(from, to, size, startPickup, endPickup, startDelivery, endDelivery);
		vrpTransformation.addShipment(requestedShipment);
		double tourCost = 0.0;
		List<Contract> carrierContracts = new ArrayList<Contract>(carrier.getContracts());
		carrierContracts.add(CarrierUtils.createContract(requestedShipment, new CarrierOffer()));
//		CarrierPlan plan = carrierPlanBuilder.buildPlan(carrier.getCarrierCapabilities(), carrierContracts);
//		plan.getScore();
//		
		List<Schedule> schedules = getSchedules(carrierContracts);
		for(Schedule schedule : schedules){
			Collection<Contract> contracts = schedule.getContracts();
			if(contracts.isEmpty()){
				continue;
			}
			Collection<vrp.basics.Tour> vrpSolution = new ArrayList<vrp.basics.Tour>();
			RuinAndRecreateSolver ruinAndRecreateSolver = new RuinAndRecreateSolver(vrpSolution, vrpTransformation);
			ruinAndRecreateSolver.setNumberOfWarmUpIterations(2);
			ruinAndRecreateSolver.setNofIterations(10);
			ruinAndRecreateSolver.solve(contracts, carrierVehicle);
			tourCost += ruinAndRecreateSolver.getVrpSolution().getTransportCosts();
		}
		double avgMCInMeters=new AverageMarginalCostOM.AvgMarginalCostCalculator(carrierContracts, network, carrierVehicle.getLocation()).calculateShareOfDistance(requestedShipment, tourCost);
//		double avgMCInSeconds=new AverageMarginalCostOM.AvgMarginalCostCalculator(carrierContracts, network, carrierVehicle.getLocation()).calculateShareOfTime(requestedShipment, tourCost);
		//		assertNotHigherThan(200000, avgMCInMeters);
		double mcCost = carrierCostCalculator.calculateCost(carrierVehicle, avgMCInMeters, 0.0);
		CarrierOffer offer = new CarrierOffer();
		offer.setId(carrier.getId());
		offer.setPrice(mcCost);
		return offer;
	}
	
	private void assertNotHigherThan(int i, double avgMC) {
		if(avgMC > i){
			throw new IllegalStateException("strange");
		}
		return;
	}

	private List<Schedule> getSchedules(Collection<Contract> carrierContracts) {
		List<Schedule> schedules = new ArrayList<RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder.Schedule>();
		List<Contract> forenoon = new ArrayList<Contract>();
		List<Contract> afternoon = new ArrayList<Contract>();
		for(Contract c : carrierContracts){
			if(c.getShipment().getPickupTimeWindow().getStart() < 24*3600){
				forenoon.add(c);
			}
			else{
				afternoon.add(c);
			}
		}
		schedules.add(new Schedule(forenoon,0.0));
		schedules.add(new Schedule(afternoon,24*3600));
		return schedules;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
}
