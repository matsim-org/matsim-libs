package playground.wrashid.sschieffer.DecentralizedSmartCharger;


/**
 *  * class that specifies on battery type for a vehicle; parameters to specify are:
 * <ul>
 * <li> batterySize in Joules;
 * e.g. common size is 24kWh = 24kWh*3600s/h*1000W/kW = 24*3600*1000Ws= 24*3600*1000J
 * 
 * <li> minSOC
 * <li> maxSOC
 * - minimum level of state of charge, avoid going below this SOC= batteryMin
		 * (0.1=10%)
		 * - maximum level of state of charge, avoid going above = batteryMax
		 * (0.9=90%)
 * <li> name
 * </ul>
 * @author Stella
 * @author Stella
 *
 */
public class Battery {
	
	private double batterySize;
	
	private double minSOC, maxSOC;
	
	
	/**
	 * 
	 * @param batterySize
	 * @param minSOC
	 * @param maxSOC
	 */
	public Battery(double batterySize, double minSOC, double maxSOC){
		this.batterySize=batterySize;
		this.minSOC=minSOC;
		this.maxSOC=maxSOC;
	}
	
	
	public double getBatterySize(){
		return batterySize;
	}
	
	public double getMinSOC(){
		return minSOC;
	}
	
	
	public double getMaxSOC(){
		return maxSOC;
	}
	
}
