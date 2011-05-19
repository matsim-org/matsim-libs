package playground.wrashid.sschieffer.DecentralizedSmartCharger.V2G;

public class V2GAgentStats {

	private double revenueV2G;
	private double totalJouleUp;
	private double totalJouleDown;
	
	public V2GAgentStats(){
		revenueV2G=0;
		totalJouleUp=0;
		totalJouleDown=0;
	}
	
	
	public void addRevenueV2G(double money){
		revenueV2G+=money;
	}
	
	public void addJoulesUp(double joules){
		totalJouleUp+=joules;
	}
	
	public void addJoulesDown(double joules){
		totalJouleDown+=joules;
	}
	
	public double getRevenueV2G(){
		return revenueV2G;
	}
	
	public double getTotalJoulesUp(){
		return totalJouleUp;
	}
	
	public double getTotalJoulesDown(){
		return totalJouleDown;
	}
	
	
	
}
