package playground.wrashid.sschieffer.V2G;

/**
 * store agent preferences. according to different 
 * contract types for different agents certain functions of V2G can be turned on or off.
 * 
 * example: 
 * Risk averse agents (who want full flexibilty) might not want to provide
 * regulation up (= discharging) whereas every agent would probably like to profit from
 * cheap charging conditions during regulation down (=charging). 
 * 
 * @author Stella
 *
 */
public class ContractTypeAgent {

	
	private boolean regulationUp;
	
	private boolean regulationDown;
	
	private double compensationRegulationUpPerKWH, compensationRegulationDownPerKWH;
	
	private double compensationPERKWHFeedInVehicle;
	//private boolean reschedule;
	
	public ContractTypeAgent(boolean up, 
			boolean down,
			double compensationRegulationUpPerKWH,
			double compensationRegulationDownPerKWH,
			double compensationPERKWHFeedInVehicle){
		regulationUp=up;
		regulationDown=down;
		this.compensationPERKWHFeedInVehicle=compensationPERKWHFeedInVehicle;
		this.compensationRegulationDownPerKWH=compensationRegulationDownPerKWH;
		this.compensationRegulationUpPerKWH=compensationRegulationUpPerKWH;
	}
	
	
	public double compensationUpFeedIn(){
		return compensationPERKWHFeedInVehicle;
	}
	
	public double compensationUp(){
		return compensationRegulationUpPerKWH;
	}
	
	public double compensationDown(){
		return compensationRegulationDownPerKWH;
	}
	
	public boolean isUp(){
		return regulationUp;
	}
	
	public boolean isDown(){
		return regulationDown;
	}
	
	
	
}
