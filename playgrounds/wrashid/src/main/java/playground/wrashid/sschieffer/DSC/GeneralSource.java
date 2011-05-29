package playground.wrashid.sschieffer.DSC;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;

public class GeneralSource {

	private String inputLoad96Bins;
	private ArrayList<LoadDistributionInterval> hubSourceIntervals;
	
	private Id linkId;
	
	private String name;
	
	private Schedule loadSchedule;
	
	private double feedInCompensationPerKHW;
	
	
	
	public GeneralSource(String inputLoad96Bins, Id linkId, String name, double feedInCompensationPerKHW){
		this.inputLoad96Bins=inputLoad96Bins;
		this.linkId=linkId;
		this.name=name;
		this.feedInCompensationPerKHW=feedInCompensationPerKHW;
	}
	
	
	
	public GeneralSource( ArrayList<LoadDistributionInterval> hubSourceIntervals, Id linkId, String name, double feedInCompensationPerKHW){
		this.hubSourceIntervals=hubSourceIntervals;
		this.linkId=linkId;
		this.name=name;
		this.feedInCompensationPerKHW=feedInCompensationPerKHW;
	}
	
	
	public void setLoadSchedule(Schedule loadSchedule){
		this.loadSchedule= loadSchedule;
	}
	
	
	public Schedule getLoadSchedule(){
		return loadSchedule;
	}
	
	public double getFeedInCompensationPerKWH(){
		return feedInCompensationPerKHW;
	}
	
	
	public boolean isTxtLoad(){
		if (inputLoad96Bins!= null){
			return true;
		}else{return false;}
	}
	
	public String getInputLoad96Bins(){
		return inputLoad96Bins;
	}
	
	
	public ArrayList<LoadDistributionInterval> getInputHubSourceIntervals(){
		return hubSourceIntervals;
	}
	
	public String getName(){
		return name;
	}
	
	public Id getlinkId(){
		return linkId;
	}
}
