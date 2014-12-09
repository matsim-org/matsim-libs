package playground.wrashid.ABMT.vehicleShare;

public class EVCosts implements VehicleCosts {

/*Total public investment?
 *Effectiveness of measures? 
 *Umverteilung als Nullsumme?
 *
 *
 *-> change toll prices
 *	-> change in traffic?
 *
 *
 *-> analyse: co2 emissions, green house gas emissions.
 *-> ev share, mode share.
 *-> total travel split by mode/vehicle type.
 *-> 
 *
 *
 *
 * 
 *
 *-> future: noise emissions. 
 *
 */
	
	
	
	@Override
	public double getInitialInvestmentCost() {
		
		/*Anschaffung Batterie
		 * Importzölle
		 * 
		 */
		return 	GlobalTESFParameters.evPurchaseCostPerDay + GlobalTESFParameters.evInsuranceCostPerDay + GlobalTESFParameters.evBatteryCostPerDay + GlobalTESFParameters.evTariffCostPerDay;
	}

	@Override
	public double getPerMeterTravelCost() {
		return GlobalTESFParameters.evMaintainanceCostPerMeter + GlobalTESFParameters.evDrivingCostPerMeter + GlobalTESFParameters.evTaxationPerMeter + GlobalTESFParameters.evSubsidyPerMeter;
	}

	@Override
	public double getPaidTollCost() {
		return GlobalTESFParameters.tollPriceEV;
	}

}
