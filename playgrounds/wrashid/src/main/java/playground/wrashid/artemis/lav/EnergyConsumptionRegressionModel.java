package playground.wrashid.artemis.lav;

import java.util.HashMap;
import java.util.LinkedList;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.obj.StringMatrix;

public class EnergyConsumptionRegressionModel {

	private LinkedList<EnergyConsumptionModelRow> energyConsumptionRegressionModel;
	private LinkedList<Double> availableMaxSpeeds=new LinkedList<Double>();

	public LinkedList<EnergyConsumptionModelRow> getEnergyConsumptionRegressionModelRows(){
		return energyConsumptionRegressionModel;
	}
	
	public EnergyConsumptionRegressionModel(String fileName) {
		energyConsumptionRegressionModel = getEnergyConsumptionRegressionModel(fileName);
		availableMaxSpeeds.add(30.0);
		availableMaxSpeeds.add(50.0);
		availableMaxSpeeds.add(60.0);
		availableMaxSpeeds.add(90.0);
		availableMaxSpeeds.add(120.0);
	}

	public static void main(String[] args) {
		String energyConsumptionModelFile = "C:/data/My Dropbox/ETH/Projekte/ARTEMIS/simulationen aug 2011/update data 16. Aug. 2011/artemis_2.dat";
		EnergyConsumptionRegressionModel energyConsumptionRegressionModel = new EnergyConsumptionRegressionModel(
				energyConsumptionModelFile);
		
		VehicleTypeLAV vehicleType=new VehicleTypeLAV();
		vehicleType.powerTrainClass=1;
		vehicleType.fuelClass=1;
		vehicleType.powerClass=1;
		vehicleType.massClass=1;
		EnergyConsumptionModelRow vehicleEnergyConsumptionModel = energyConsumptionRegressionModel.getVehicleEnergyConsumptionModel(vehicleType,30.0);
		
		double energyConsumptionInJoulePerMeter = vehicleEnergyConsumptionModel.getEnergyConsumptionInJoulePerMeter(23.0);
		System.out.println(energyConsumptionInJoulePerMeter);
		
	}

	// if this is too slow, simply use hashmap (code parts of vehicle type into 4 digit number used as key
	// in hashmap
	public EnergyConsumptionModelRow getVehicleEnergyConsumptionModel(VehicleTypeLAV vehicleType, double maxSpeedInKmPerHour){
		maxSpeedInKmPerHour=getBestMatchForMaxSpeed(maxSpeedInKmPerHour);
		
		for (EnergyConsumptionModelRow consumptionModel:energyConsumptionRegressionModel){
			VehicleTypeLAV rowVehicleType=consumptionModel.vehicleType;
			if (rowVehicleType.equals(vehicleType) && consumptionModel.topSpeedInKmPerHour==maxSpeedInKmPerHour){
				return consumptionModel;
			}
		}
		
		vehicleType.print();
		DebugLib.stopSystemAndReportInconsistency("maxSpeedInKmPerHour:" + maxSpeedInKmPerHour);
		
		return null;
	}
	
	private double getBestMatchForMaxSpeed(double maxSpeedInKmPerHour) {
		double difference=Double.MAX_VALUE;
		int indexOfLeastDifferenceSpeed=-1;
		
		int i=0;
		for (Double curSpeed:availableMaxSpeeds){
			double currentDifference=Math.abs(maxSpeedInKmPerHour-curSpeed);
			if (currentDifference<difference){
				indexOfLeastDifferenceSpeed=i;
			}
			i++;
		}
		
		return availableMaxSpeeds.get(indexOfLeastDifferenceSpeed);
	}

	public static LinkedList<EnergyConsumptionModelRow> getEnergyConsumptionRegressionModel(String fileName) {
		LinkedList<EnergyConsumptionModelRow> list = new LinkedList<EnergyConsumptionModelRow>();
		StringMatrix modelFile = LAVLib.readLAVModelFile(fileName, true);

		for (int i = 0; i < modelFile.getNumberOfRows(); i++) {
			EnergyConsumptionModelRow energyConsumptionModelRow = new EnergyConsumptionModelRow();
			VehicleTypeLAV vehicleType = new VehicleTypeLAV();
			vehicleType.powerTrainClass = modelFile.getInteger(i, 0);
			vehicleType.fuelClass = modelFile.getInteger(i, 1);
			vehicleType.powerClass = modelFile.getInteger(i, 2);
			vehicleType.massClass = modelFile.getInteger(i, 3);
			// only 50% of battery capacity available for simulation
			energyConsumptionModelRow.batteryCapacity = modelFile.getDouble(i, 4) / 2;
			energyConsumptionModelRow.topSpeedInKmPerHour = modelFile.getInteger(i, 6);
			energyConsumptionModelRow.a1 = modelFile.getDouble(i, 8);
			energyConsumptionModelRow.a2 = modelFile.getDouble(i, 9);
			energyConsumptionModelRow.a3 = modelFile.getDouble(i, 10);
			energyConsumptionModelRow.a4 = modelFile.getDouble(i, 11);
			energyConsumptionModelRow.a5 = modelFile.getDouble(i, 12);
			energyConsumptionModelRow.a6 = modelFile.getDouble(i, 13);
			energyConsumptionModelRow.a7 = modelFile.getDouble(i, 14);
			energyConsumptionModelRow.a8 = modelFile.getDouble(i, 15);

			energyConsumptionModelRow.vehicleType = vehicleType;
			
			list.add(energyConsumptionModelRow);
		}

		return list;
	}

	public static class EnergyConsumptionModelRow {
		public VehicleTypeLAV vehicleType;
		private double batteryCapacity;
		public double topSpeedInKmPerHour;
		public double a1;
		public double a2;
		public double a3;
		public double a4;
		public double a5;
		public double a6;
		public double a7;
		public double a8;

		public double getBatteryCapacityInJoule(){
			double OneWhInJoules=3600;
			double voltageInVolt=400;
			return batteryCapacity*voltageInVolt*OneWhInJoules;
		}
		
		public double getEnergyConsumptionInJoulePerMeter(
				double averageSpeedDrivenOnLinkInMeterPerSecond) {
			double vMaxSquare = Math.pow(topSpeedInKmPerHour*1000/3600, 2);
			double vAverageSquare = Math.pow(averageSpeedDrivenOnLinkInMeterPerSecond, 2);

			double fRoad = a1;
			double fAero = a2 + a3 * vAverageSquare;
			double fAcc = (vAverageSquare - vMaxSquare) * a4 + vAverageSquare * a5;

			double effPowerTrain = vAverageSquare * a6 + a7 * averageSpeedDrivenOnLinkInMeterPerSecond + a8;

			double energyConsumptionInJoulePerMeter = (fRoad + fAero + fAcc) / effPowerTrain;
			
			if (energyConsumptionInJoulePerMeter<0){
				DebugLib.emptyFunctionForSettingBreakPoint();
			}
			
			return energyConsumptionInJoulePerMeter;
		}
	}

}
