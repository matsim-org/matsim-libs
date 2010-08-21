package playground.wrashid.parkingSearch.planLevel.strc2010;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.trafficmonitoring.PessimisticTravelTimeAggregator;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.parkingActivityDuration.ParkingActivityDuration;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenario;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenarioOneLiner;

/**
 * 
 * @author wrashid
 * 
 */
public class Run22 extends ParkingActivityDuration {
	public static void main(String[] args) {
		Controler controler;
		String configFilePath = "H:/data/experiments/STRC2010/input/config22.xml";
		controler = new Controler(configFilePath);

		new BaseControlerScenario(controler);

		ParkingRoot.setParkingActivityDuration(new Run22());
		GlobalRegistry.doPrintGraficDataToConsole=true;
		
		controler.run();
		
		
	}
	
	public double getActivityDuration(Id parkingFacility, Id personId){
		if (parkingFacility.toString().equalsIgnoreCase("1")){
			// programm this rightly!!!
		}
		
		return 60;
	}

}
