package playground.wrashid.sschieffer.DecentralizedSmartCharger;

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
	
	//private boolean reschedule;
	
	public ContractTypeAgent(boolean up, 
			boolean down,
			double compensationRegulationUpPerKWH,
			double compensationRegulationDownPerKWH){
		regulationUp=up;
		regulationDown=down;
		
		this.compensationRegulationDownPerKWH=compensationRegulationDownPerKWH;
		this.compensationRegulationUpPerKWH=compensationRegulationUpPerKWH;
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
