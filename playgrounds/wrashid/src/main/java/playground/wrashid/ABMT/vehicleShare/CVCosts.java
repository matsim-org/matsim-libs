package playground.wrashid.ABMT.vehicleShare;

public class CVCosts implements VehicleCosts {

	@Override
	public double getInitialInvestmentCost() {
		// import zölle
		
		// singapore - dänemark, norwegen (future work: priority lanes for evs, e.g. bus lanes).
		// future work: close certain, areas/roads for cv driving
		
		return 	GlobalTESFParameters.cvPurchaseCostPerDay + GlobalTESFParameters.cvInsuranceCostPerDay + GlobalTESFParameters.cvTariffCostPerDay;
	}

	@Override
	public double getPerMeterTravelCost() {
		return GlobalTESFParameters.cvMaintainanceCostPerMeter + GlobalTESFParameters.cvDrivingCostPerMeter + GlobalTESFParameters.cvTaxationPerMeter + GlobalTESFParameters.cvSubsidyPerMeter;
	}

	@Override
	public double getPaidTollCost() {
		return GlobalTESFParameters.tollPriceCV;
	}

}
