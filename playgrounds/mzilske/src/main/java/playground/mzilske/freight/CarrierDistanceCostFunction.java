package playground.mzilske.freight;

public class CarrierDistanceCostFunction implements CarrierCostFunction{

	private Carrier carrier;
	
	@Override
	public void init(Carrier carrier) {
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
