package playground.wrashid.parkingSearch.planLevel.strc2010;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.trafficmonitoring.PessimisticTravelTimeAggregator;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenario;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenarioOneLiner;

/**
 * 
 * @author wrashid
 * 
 */
public class Run1 {
	public static void main(String[] args) {
		new BaseControlerScenarioOneLiner("H:/data/experiments/STRC2010/input/config1.xml").run();
		GlobalRegistry.doPrintGraficDataToConsole=true;
	}

}
