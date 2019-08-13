package org.matsim.contrib.accidents.computation;

public class AccidentCostBVWPtoFrequencyConverter {
	
	public static double convertAccidentFrequencyDependingOnActualSpeed (double accidentCostBVWP, double actualSpeed) {
		double costPerAccident = 0;
		double convertedAccidentFrequencyBVWP = 0;
		if (actualSpeed <= 8.34){
			costPerAccident = 10425.; // 30% to 40% less deaths and severe injuries if speed under 30km/h
		}else costPerAccident = 13510.;
		convertedAccidentFrequencyBVWP = accidentCostBVWP/costPerAccident;
		return convertedAccidentFrequencyBVWP;
	}
}
