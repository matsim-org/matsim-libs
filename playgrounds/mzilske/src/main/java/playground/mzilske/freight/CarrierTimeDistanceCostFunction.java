package playground.mzilske.freight;

import org.apache.log4j.Logger;

public class CarrierTimeDistanceCostFunction implements CarrierCostFunction {

	public static double COST_PER_VEHICLEHOUR = 25;
	
	public static double COST_PER_VEHICLEKM = 1;
	
	public static double CITY_TOLL = 0;
	
	private static Logger logger = Logger.getLogger(CarrierTimeDistanceCostFunction.class);
	
	private CarrierImpl carrier;

	@Override
	public void init(CarrierImpl carrier) {
		this.carrier = carrier;
	}

	@Override
	public double calculateCost(CarrierVehicle carrierVehicle, double distance) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double calculateCost(CarrierVehicle carrierVehicle, double distance, double time) {
		double cost = distance/1000*COST_PER_VEHICLEKM + time/3600*25;
		if(carrierVehicle.getCapacity() == 20){
			logger.info(carrierVehicle.getVehicleId() + " pays maut");
			cost += CITY_TOLL;
		}
		return cost;
	}

}
