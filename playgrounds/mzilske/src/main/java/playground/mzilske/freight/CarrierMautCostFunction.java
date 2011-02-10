package playground.mzilske.freight;

public class CarrierMautCostFunction implements CarrierCostFunction {
	
	private CarrierImpl carrier;
	
	@Override
	public double calculateCost(CarrierVehicle carrierVehicle, double distance) {
		double cost = 0.0;
		cost += distance;
		if (carrierVehicle.getCapacity() >= 10) {
			cost += 10000;
		}
		return cost;
	}


	@Override
	public void init(CarrierImpl carrier) {
		this.carrier = carrier;		
	}

}
