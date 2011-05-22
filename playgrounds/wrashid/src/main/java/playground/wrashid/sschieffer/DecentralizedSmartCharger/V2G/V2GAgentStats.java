package playground.wrashid.sschieffer.DecentralizedSmartCharger.V2G;

public class V2GAgentStats {

	private double revenueV2G;
	
	private double revenueFeedIn;
	private double totalJouleFeedIn;
	
	private double totalJouleUp;
	private double totalJouleDown;
	
	public V2GAgentStats(){
		revenueV2G=0;
		totalJouleUp=0;
		totalJouleDown=0;
		revenueFeedIn=0;
		totalJouleFeedIn=0;
	}
	
	
	public void addRevenueFeedIn(double money){
		revenueFeedIn+=money;
	}
	
	public void addJoulesFeedIn(double joules){
		totalJouleFeedIn+=joules;
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
	
	public double getRevenueFeedIn(){
		return revenueFeedIn;
	}
	
	public double getTotalJoulesFeedIn(){
		return totalJouleFeedIn;
	}
	
	public double getTotalJoulesUp(){
		return totalJouleUp;
	}
	
	public double getTotalJoulesDown(){
		return totalJouleDown;
	}
	
	
	
}
