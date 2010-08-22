package playground.wrashid.parkingSearch.planLevel.strc2010;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.trafficmonitoring.PessimisticTravelTimeAggregator;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.RunLib;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.parkingActivityDuration.ParkingActivityDuration;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenario;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenarioOneLiner;
import playground.wrashid.parkingSearch.planLevel.scoring.ParkingDefaultScoringFunction;

/**
 * 
 * @author wrashid
 * 
 */
public class Run22 extends ParkingActivityDuration {
	public static void main(String[] args) {
		int runNumber = RunLib.getRunNumber(new Object() {
		}.getClass().getEnclosingClass());
		Controler controler = RunSeries.getControler(runNumber);

		ParkingRoot.setParkingActivityDuration(new Run22());
		GlobalRegistry.doPrintGraficDataToConsole=true;
		
		controler.run();
		
		
	}
	
	// TODO: need to do some experimentation on this
	public double getActivityDuration(Id parkingFacilityId, Id personId){
		if (Run27.isPartOfParkingSetCloseToHomeWithin4500Meters(parkingFacilityId) || Run27.isPartOfParkingSetCloseToWorkWithin4500Meters(parkingFacilityId)){
			return ParkingDefaultScoringFunction.oneValueForNormalizationInSeconds*2;
		}
		
		return 60;
	}

}
