package freight.offermaker;

import freight.vrp.Locations;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.*;
import playground.mzilske.freight.OfferMaker;

import java.util.ArrayList;
import java.util.List;

public class RuinAndRecreateMarginalCostOM implements OfferMaker{

	private Carrier carrier;
	
	private CarrierVehicle carrierVehicle;

	private Locations locations;
	
	private CarrierCostFunction carrierCostCalculator;
	
	private CarrierPlanBuilder carrierPlanBuilder;
	
	private static Logger logger = Logger.getLogger(RuinAndRecreateMarginalCostOM.class);
	
	
	public void setCarrierPlanBuilder(CarrierPlanBuilder carrierPlanBuilder) {
		this.carrierPlanBuilder = carrierPlanBuilder;
	}


	public void setCarrierCostCalculator(CarrierCostFunction carrierCostCalculator) {
		this.carrierCostCalculator = carrierCostCalculator;
	}


	public RuinAndRecreateMarginalCostOM(Carrier carrier, Locations locations) {
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

		CarrierShipment requestedShipment = CarrierUtils.createShipment(from, to, size, startPickup, endPickup, startDelivery, endDelivery);
		List<CarrierContract> carrierContracts = new ArrayList<CarrierContract>(carrier.getContracts());
		double scoreWithoutRequestedShipment = 0.0;
		double mcCost;

		if(!carrierContracts.isEmpty()){
			CarrierPlan plan = carrierPlanBuilder.buildPlan(carrier.getCarrierCapabilities(), carrierContracts);
			scoreWithoutRequestedShipment = plan.getScore();
		}

		carrierContracts.add(CarrierUtils.createContract(requestedShipment, new CarrierOffer()));
		CarrierPlan newPlan = carrierPlanBuilder.buildPlan(carrier.getCarrierCapabilities(), carrierContracts);
		double scoreWithRequestedShipment = newPlan.getScore();
		double marginalCostInMeters;
		marginalCostInMeters = scoreWithRequestedShipment - scoreWithoutRequestedShipment;
		if(marginalCostInMeters < 0){
			logger.warn("marginal costs lower than 0");
			marginalCostInMeters = 0;
		}
		mcCost = carrierCostCalculator.calculateCost(carrierVehicle, marginalCostInMeters, 0.0);
		
		CarrierOffer offer = new CarrierOffer();
		offer.setId(carrier.getId());
		offer.setPrice(mcCost);
		return offer;
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
