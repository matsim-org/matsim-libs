package playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;


public class HubInfo {

	private Integer id;
	private String freeDeterministicLoadTxt;
	private String stochasticGeneralLoadTxt;
	private ArrayList<GeneralSource> stochasticGeneralSources;
	private HashMap <Id, String> stochasticVehicleLoadTxt;
	
	private double priceMax, priceMin;
	
	
	
	/**
	 * initializer for stochastic hub load
	 * @param id
	 * @param freeLoadTxt
	 */
	public HubInfo(Integer id, 
			String stochasticGeneralLoadTxt, 
			ArrayList<GeneralSource> stochasticGeneralSources, 
			HashMap <Id, String> stochasticVehicleLoadTxt){
		this.id=id;
		this.stochasticGeneralLoadTxt=stochasticGeneralLoadTxt;
		this.stochasticGeneralSources=stochasticGeneralSources;
		this.stochasticVehicleLoadTxt=stochasticVehicleLoadTxt;
	}
	
	
	/**
	 * initializer for stochastic hub load
	 * @param id
	 * @param freeLoadTxt
	 */
	public HubInfo(Integer id, 
			String stochasticGeneralLoadTxt
		){
		this.id=id;
		this.stochasticGeneralLoadTxt=stochasticGeneralLoadTxt;
	}
	
	
	public void setStochasticVehicleSources(HashMap <Id, String> stochasticVehicleLoadTxt){
		this.stochasticVehicleLoadTxt=stochasticVehicleLoadTxt;
	}
	
	public void setStochasticGeneralSources(ArrayList<GeneralSource> stochasticGeneralSources){
		this.stochasticGeneralSources=stochasticGeneralSources;
	}
	
	
	/**
	 * initializer for detemrinistic hub load
	 * @param id
	 * @param freeLoadTxt
	 * @param priceMax
	 * @param priceMin
	 */
	public HubInfo(Integer id, String freeLoadTxt, double priceMax, double priceMin){
		this.id=id;
		this.freeDeterministicLoadTxt=freeLoadTxt;
		this.priceMax=priceMax;
		this.priceMin=priceMin;
		
	}
	
	public String geStochasticGeneralLoadTxt(){
		return stochasticGeneralLoadTxt;
	}
	
	
	public ArrayList<GeneralSource> getStochasticGeneralSources(){
		return stochasticGeneralSources;
	}
	
	public HashMap<Id, String> geStochasticVehicleLoadTxt(){
		return stochasticVehicleLoadTxt;
	}
	
	
	public Integer getId(){
		return id;
	}
	
	public String getDeterministicFreeLoadTxt(){
		return freeDeterministicLoadTxt;
	}
	
	public double getPriceMax(){
		return priceMax;
	}
	
	public double getPriceMin(){
		return priceMin;
	}
	
}
