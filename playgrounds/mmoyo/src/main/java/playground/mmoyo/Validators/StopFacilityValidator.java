package playground.mmoyo.Validators;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.utils.DataLoader;

//validates a 1-1 relationship between stopFacility and link
public class StopFacilityValidator {
	
	public StopFacilityValidator(String config){
		ScenarioImpl scenario = new DataLoader ().loadScenarioWithTrSchedule(config);
		TransitSchedule trSchedule = scenario.getTransitSchedule();
		List<Id> LinkIdList = new ArrayList<Id>();
		for (TransitStopFacility transitStopFacility: trSchedule.getFacilities().values()){
			Id linkId = transitStopFacility.getLinkId();
		
			if (LinkIdList.contains(linkId)){
				System.out.println("error");
			}
		}
	}
	
	public static void main(String[] args) {
		String config = null;

		if (args.length==1){
			config = args[0];
		}else{
			config= "../playgrounds/mmoyo/output/trRoutVis/config.xml";
		}
		new StopFacilityValidator(config);
	}

}
