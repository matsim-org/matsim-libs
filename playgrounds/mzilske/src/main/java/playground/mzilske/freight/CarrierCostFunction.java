package playground.mzilske.freight;

public interface CarrierCostFunction {

	void init(CarrierImpl carrier);
	
	double calculateCost(CarrierVehicle carrierVehicle, double distance);

}
