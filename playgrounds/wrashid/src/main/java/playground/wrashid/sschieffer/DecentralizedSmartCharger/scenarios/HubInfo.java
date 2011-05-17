package playground.wrashid.sschieffer.DecentralizedSmartCharger.scenarios;

public class HubInfo {

	private Integer id;
	private String freeLoadTxt;
	private double priceMax, priceMin;
	
	public HubInfo(Integer id, String freeLoadTxt, double priceMax, double priceMin){
		this.id=id;
		this.freeLoadTxt=freeLoadTxt;
		this.priceMax=priceMax;
		this.priceMin=priceMin;
		
	}
	
	public Integer getId(){
		return id;
	}
	
	public String getFreeLoadTxt(){
		return freeLoadTxt;
	}
	
	public double getPriceMax(){
		return priceMax;
	}
	
	public double getPriceMin(){
		return priceMin;
	}
	
}
