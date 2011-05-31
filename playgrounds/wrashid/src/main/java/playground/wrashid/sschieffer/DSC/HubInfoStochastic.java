package playground.wrashid.sschieffer.DSC;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;

/**
 * the hub info is the information package relevant to set up a hub
 * you can define deterministic and stochastic inputs for the hub
 * 
 * <li> free deterministic load 
 * @author Stella
 *
 */
public class HubInfoStochastic {

	private Integer id;
	
	/*
	 * And Stochastic loads
	 */
	private String stochasticGeneralLoadTxt;
	private ArrayList<LoadDistributionInterval> stochasticGeneralLoadIntervals;
	
	private ArrayList<GeneralSource> stochasticGeneralSources;
	
	private HashMap <Id, String> stochasticVehicleLoadTxt;
	private HashMap <Id, ArrayList<LoadDistributionInterval>> stochasticVehicleLoadIntervals;
	
	
	public  HubInfoStochastic( Integer id
			){
		this.id=id;
	}
	
	/**
	 * All text initializer for stochastic hub  id
	 * <li> txt file input format for stochastic general load
	 * <li> ArrayList of GeneralSouces for hubSources
	 * <li> text input file for stochastic vehicle loads
	 * @param id
	 * @param freeLoadTxt
	 */
	public  HubInfoStochastic( Integer id,
			String stochasticGeneralLoadTxt, 
			ArrayList<GeneralSource> stochasticGeneralSources, 
			HashMap <Id, String> stochasticVehicleLoadTxt){
		this.id=id;
		this.stochasticGeneralLoadTxt=stochasticGeneralLoadTxt;
		this.stochasticGeneralSources=stochasticGeneralSources;
		this.stochasticVehicleLoadTxt=stochasticVehicleLoadTxt;
	}
	
	
	/**
	 * All load intervals initializer for stochastic loads
	 * @param id
	 * @param stochasticGeneralLoadIntervals
	 * @param stochasticGeneralSources
	 * @param stochasticVehicleLoadIntervals
	 */
	public  HubInfoStochastic(  Integer id,
			ArrayList<LoadDistributionInterval> stochasticGeneralLoadIntervals, 
			ArrayList<GeneralSource> stochasticGeneralSources, 
			HashMap <Id, ArrayList<LoadDistributionInterval>> stochasticVehicleLoadIntervals){
		this.id=id;
		this.stochasticGeneralLoadIntervals=stochasticGeneralLoadIntervals;
		this.stochasticGeneralSources=stochasticGeneralSources;
		this.stochasticVehicleLoadIntervals=stochasticVehicleLoadIntervals;
		
	}
	
	
	public HubInfoStochastic(Integer id,String stochasticGeneralLoadTxt){
		this.id=id;
		this.stochasticGeneralLoadTxt=stochasticGeneralLoadTxt;
	}
	
	
	public  HubInfoStochastic(Integer id, ArrayList<LoadDistributionInterval> stochasticGeneralLoadIntervals 
			){
		this.id=id;
		this.stochasticGeneralLoadIntervals=stochasticGeneralLoadIntervals;
	}
	
	public void setStochasticVehicleSourcesTxt(HashMap <Id, String> stochasticVehicleLoadTxt){
		this.stochasticVehicleLoadTxt=stochasticVehicleLoadTxt;
	}
	
	public void setStochasticGeneralSources(ArrayList<GeneralSource> stochasticGeneralSources){
		this.stochasticGeneralSources=stochasticGeneralSources;
	}
	
	
	/**
	 * initializer for stochastic hub load
	 * @param id
	 * @param freeLoadTxt
	 */
	public void setGeneralStochasticLoadIntervals(ArrayList<LoadDistributionInterval> stochasticGeneralLoadIntervals){
		
		this.stochasticGeneralLoadIntervals=stochasticGeneralLoadIntervals;
	}
	
	
	public void setStochasticVehicleSourcesIntervals(HashMap <Id, ArrayList<LoadDistributionInterval>> stochasticVehicleLoadIntervals){
		this.stochasticVehicleLoadIntervals=stochasticVehicleLoadIntervals;
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
	
	
	public ArrayList<LoadDistributionInterval> getGeneralStochasticLoadIntervals(){
		
		return stochasticGeneralLoadIntervals;
	}
	
	
	public HashMap <Id, ArrayList<LoadDistributionInterval>> getStochasticVehicleSourcesIntervals(){
		return stochasticVehicleLoadIntervals;
	}
	
	
	public Integer getId(){
		return id;
	}
	
	
	
	public boolean hasVehicleStochasticLoad(){
		if(stochasticVehicleLoadTxt!=null && stochasticVehicleLoadIntervals!=null){
			return true;
		}else{return false;}
	}
	
	public boolean hasGeneralHubSourcesStochasticLoad(){
		if(stochasticGeneralSources!=null){
			return true;
		}else{return false;}
	}
	
	public boolean isTxtGeneralStochastic(){
		if (stochasticGeneralLoadTxt!=null){
			return true;
		}else{return false;}
	}
	
	public boolean isTxtVehicleStochastic(){
		if (stochasticVehicleLoadTxt!=null){
			return true;
		}else{return false;}
	}
	
	
	
	
}
