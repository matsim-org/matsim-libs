package playground.wrashid.ABMT.vehicleShare;

public class CVCosts implements VehicleCosts {

	@Override
	public double getInitialInvestmentCost() {
		// import zölle
		
		// singapore - dänemark, norwegen (future work: priority lanes for evs, e.g. bus lanes).
		// future work: close certain, areas/roads for cv driving
		
		return 	GlobalTESFParameters.cvFixedCostPerDay + GlobalTESFParameters.cvTaxationPerDay;
	}

	@Override
	public double getPerMeterTravelCost() {
		return GlobalTESFParameters.cvDrivingCostPerMeter + GlobalTESFParameters.cvTaxationPerMeter;
	}

	@Override
	public double getPaidTollCost() {
		return GlobalTESFParameters.tollPriceCV;
	}

}
