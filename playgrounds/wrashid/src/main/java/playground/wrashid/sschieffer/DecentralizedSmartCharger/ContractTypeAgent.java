package playground.wrashid.sschieffer.DecentralizedSmartCharger;

public class ContractTypeAgent {

	
	private boolean regulationUp;
	
	private boolean regulationDown;
	
	//private boolean reschedule;
	
	public ContractTypeAgent(boolean up, 
			boolean down,
			boolean reschedule){
		regulationUp=up;
		regulationDown=down;
		//this.reschedule=reschedule;
	}
	
	
	
	public boolean isUp(){
		return regulationUp;
	}
	
	public boolean isDown(){
		return regulationDown;
	}
	
	
	
}
