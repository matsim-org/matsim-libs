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
		return 	GlobalTESFParameters.evFixedCostPerDay + GlobalTESFParameters.evTaxationPerDay;
	}

	@Override
	public double getPerMeterTravelCost() {
		return GlobalTESFParameters.evDrivingCostPerMeter + GlobalTESFParameters.evTaxationPerMeter;
	}

	@Override
	public double getPaidTollCost() {
		return GlobalTESFParameters.tollPriceEV;
	}

}
