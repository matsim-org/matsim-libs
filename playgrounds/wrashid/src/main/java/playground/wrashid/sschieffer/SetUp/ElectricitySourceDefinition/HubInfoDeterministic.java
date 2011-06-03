package playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;

import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.LoadDistributionInterval;

/**
 * the hub info is the information package relevant to set up a hub
 * you can define deterministic and stochastic inputs for the hub
 * 
 * <li> free deterministic load 
 * @author Stella
 *
 */
public class HubInfoDeterministic {

	private Integer id;
	/*
	 * Deterministic loads
	 */
	// either with txt input files
	private String freeDeterministicLoadTxt;
	// or with load intervals// i.e. discrete load intervals - i.e. intermittent energy
	private ArrayList<LoadDistributionInterval> freeDeterministicLoadIntervals;
	private double priceMax, priceMin; // and the minimumm and maximum price for charging energy
	
	
	
	
	/**
	 * TO only set the deterministic required input with load intervals
	 * @param id
	 * @param freeLoadTxt
	 * @param priceMax
	 * @param priceMin
	 */
	public HubInfoDeterministic(Integer id,  ArrayList<LoadDistributionInterval> freeDeterministicLoadIntervals, double priceMax, double priceMin){
		this.id=id;
		this.freeDeterministicLoadIntervals=freeDeterministicLoadIntervals;
		this.priceMax=priceMax;
		this.priceMin=priceMin;
		
	}
	
	/**
	 * TO only set the deterministic required input with a text file
	 * @param id
	 * @param freeLoadTxt
	 * @param priceMax
	 * @param priceMin
	 */
	public HubInfoDeterministic(Integer id, String freeLoadTxt, double priceMax, double priceMin){
		this.id=id;
		this.freeDeterministicLoadTxt=freeLoadTxt;
		this.priceMax=priceMax;
		this.priceMin=priceMin;
		
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
