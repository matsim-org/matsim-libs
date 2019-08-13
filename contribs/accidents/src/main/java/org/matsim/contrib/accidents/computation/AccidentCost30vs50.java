package org.matsim.contrib.accidents.computation;

/**
* @author mmayobre
*/

public class AccidentCost30vs50 {
	
	public static double giveAccidentCostDependingOnActualSpeed (double actualSpeed) {
		double costPerAccident = 0;
		if (actualSpeed <= 8.34){
			costPerAccident = 10425.; // 30% to 40% less deaths and severe injuries if speed under 30km/h
		}else costPerAccident = 13510.;
		return costPerAccident;
	}
}
