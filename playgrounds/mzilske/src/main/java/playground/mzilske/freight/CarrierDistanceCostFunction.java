package playground.mzilske.freight;

public class CarrierDistanceCostFunction implements CarrierCostFunction{

	private CarrierImpl carrier;
	
	@Override
	public void init(CarrierImpl carrier) {
		this.carrier = carrier;
		
	}

	@Override
	public double calculateCost(CarrierVehicle carrierVehicle, double distance) {
		return distance;
	}

	@Override
	public double calculateCost(CarrierVehicle carrierVehicle, double distance, double time) {
		return distance;
	}

}
