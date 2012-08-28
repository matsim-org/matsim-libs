package playground.wrashid.artemis.checks;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.geotools.math.Statistics;
import org.matsim.contrib.parking.lib.DebugLib;

import playground.wrashid.artemis.lav.EnergyConsumptionRegressionModel;
import playground.wrashid.artemis.lav.EnergyConsumptionRegressionModel.EnergyConsumptionModelRow;
import playground.wrashid.artemis.lav.VehicleTypeLAV;

public class CompareEnergyConsumptionModels {

	public static void main(String[] args) {
		LinkedList<EnergyConsumptionModelRow> energyConsumptionRegressionModel = EnergyConsumptionRegressionModel
				.getEnergyConsumptionRegressionModel("H:/data/experiments/ARTEMIS/nov2011/inputs/regModel/250km range/regModel_freakyBatteries_2050_tryout.dat");

		Random rand = new Random();

		HashMap<Double, Statistics> hm = new HashMap<Double, Statistics>();
		for (EnergyConsumptionModelRow row : energyConsumptionRegressionModel) {
			Statistics statistics = hm.get(row.topSpeedInKmPerHour);

			if (statistics == null) {
				statistics = new Statistics();
				hm.put(row.topSpeedInKmPerHour, statistics);
			}

			double energyConsumptionInJoulePerMeter = row.getEnergyConsumptionInJoulePerMeter(0.9 * row.topSpeedInKmPerHour);
			
			if (energyConsumptionInJoulePerMeter>0){
				statistics.add(energyConsumptionInJoulePerMeter);
			}
			
			
			
			VehicleTypeLAV vehicleType = row.vehicleType;
			if (vehicleType.powerTrainClass==3){
				if (vehicleType.fuelClass==5){
					if (vehicleType.powerClass==3){
						if (vehicleType.massClass==3){
							DebugLib.emptyFunctionForSettingBreakPoint();
						}
					}
				}
			}
		}

		for (Double key: hm.keySet()) {
			Statistics stat=hm.get(key);
			System.out.println(key);
			System.out.println(stat.toString());
		}

	}

}
