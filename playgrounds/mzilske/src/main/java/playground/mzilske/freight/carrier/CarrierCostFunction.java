package playground.mzilske.freight.carrier;


public interface CarrierCostFunction {

	void init(Carrier carrier);
	
	double calculateCost(CarrierVehicle carrierVehicle, double distance);
	
	double calculateCost(CarrierVehicle carrierVehicle, double distance, double time);

}
