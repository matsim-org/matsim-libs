package org.matsim.contrib.carsharing.manager.supply.costs;

import java.util.HashMap;
import java.util.Map;

import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
/** 
 * @author balac
 */
public class CostsCalculatorContainer {
	
	
	private Map<String, CompanyCosts> companyCostsMap = new HashMap<String, CompanyCosts>();
	
	
		
	public Map<String, CompanyCosts> getCompanyCostsMap() {
		return this.companyCostsMap ;
	}



	public double getCost(String company, String carsharingType, RentalInfo rentalInfo) {
		
		return this.companyCostsMap.get(company).calcCost(carsharingType, rentalInfo);
	}
	

}
