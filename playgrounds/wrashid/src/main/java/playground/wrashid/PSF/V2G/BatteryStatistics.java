package playground.wrashid.PSF.V2G;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF.lib.PSFGeneralLib;
import playground.wrashid.PSF.parking.ParkLog;
import playground.wrashid.PSF.parking.ParkingTimes;
import playground.wrashid.lib.GeneralLib;

public class BatteryStatistics {
	
	
	private static final int numberOfTimeBins = 96;
	
	/**
	 * This method, gives back the energy within the batteries, which are connected to the electric grid.
	 * @param chargingTimes
	 * @param parkingTimes
	 * @param numberOfHubs
	 * @return
	 */
	public static double[][] getGridConnectedEnergy(HashMap<Id, ChargingTimes> chargingTimes, HashMap<Id, ParkingTimes> parkingTimes){
		double[][] gridConnectedEnergy=new double[numberOfTimeBins][ParametersPSF.getNumberOfHubs()];
		double[][] curGridConnectedEnergy;
		
		
		for (Id personId : parkingTimes.keySet()) {
			ParkingTimes curParkingTimes = parkingTimes.get(personId);
			ChargingTimes curChargingTimes=chargingTimes.get(personId);
			
			curGridConnectedEnergy=getGridConnectedEnergy(curChargingTimes,curParkingTimes);
			
			// add chargable energy
			for (int i=0;i<numberOfTimeBins;i++){
				for (int j=0;j<ParametersPSF.getNumberOfHubs();j++){
					gridConnectedEnergy[i][j]+=curGridConnectedEnergy[i][j];
				}
			}
		}
			
		
		return gridConnectedEnergy;
	}
	/**
	 * Statistics of one car: How much energy could this car provide to the grid (only times considered, when car is connected to the grid).
	 * @param chargingTimes
	 * @param parkingTimes
	 * @param numberOfHubs
	 * @return
	 */
	public static double[][] getGridConnectedEnergy(ChargingTimes chargingTimes, ParkingTimes parkingTimes){
		boolean[][] parkedAtHub=parkingTimes.wasParkedAtHub();
		double[][] gridConnectedEnergy = chargingTimes.getSOCWithoutEnergyConsumption();
		
		for (int i=0;i<numberOfTimeBins;i++){
			for (int j=0;j<ParametersPSF.getNumberOfHubs();j++){
				if (!parkedAtHub[i][j]){
					gridConnectedEnergy[i][j]+=0.0;
				}
			}
		}
		
		return gridConnectedEnergy;
	}
	
	/**
	 * How much energy can be supplied to the grid from the cars which are connected to the grid. Assumption of this method is,
	 * that all cars connected to the grid are electric cars. If this assumption is wrong, then this method needs to be adapted.
	 * 
	 * vehicle2GridPower: How much energy can be supplied from on connected vehicle to the grid.
	 */
	public static double[][] getGridConnectedPower(HashMap<Id, ParkingTimes> parkingTimes, double vehicle2GridPower){
		double[][] gridConnectedPower=new double[numberOfTimeBins][ParametersPSF.getNumberOfHubs()];
		
		//removed for performance reasons (not needed)
		//gridConnectedPower=GeneralLib.initializeMatrix(gridConnectedPower);
		
		for (Id personId : parkingTimes.keySet()) {
			ParkingTimes curParkingTimes = parkingTimes.get(personId);
			boolean[][] parkedAtHub=curParkingTimes.wasParkedAtHub();
			
			// add electric power of the vehicle
			for (int i=0;i<numberOfTimeBins;i++){
				for (int j=0;j<ParametersPSF.getNumberOfHubs();j++){
					if (parkedAtHub[i][j]){
						gridConnectedPower[i][j]+=vehicle2GridPower;
					}
				}
			}
		}
		
			
		return gridConnectedPower;
	}
	
	
	
	public static void writeGridConnectedPower(String fileName, double[][] gridConnectedPower) {
		GeneralLib.writeGraphic(fileName, GeneralLib.scaleMatrix(gridConnectedPower, 1.0/1000) ,"Power Available From Connected Vehicles", "Time of Day [s]","Power [kW]");
	}
	
	public static void writeGridConnectedPowerData(String fileName, double[][] gridConnectedPower) {
		PSFGeneralLib.writeEnergyUsageStatisticsData(fileName, gridConnectedPower);
	}
	
	public static void writeGridConnectedEnergy(String fileName, double[][] gridConnectedPower) {
		GeneralLib.writeGraphic(fileName, GeneralLib.scaleMatrix(gridConnectedPower, 1.0/1000/3600) ,"Energy Available From Connected Vehicles", "Time of Day [s]","Energy [kWh]");
	}
	
	public static void writeGridConnectedEnergyData(String fileName, double[][] gridConnectedPower) {
		PSFGeneralLib.writeEnergyUsageStatisticsData(fileName, gridConnectedPower);
	}
	
}
