package playground.wrashid.parkingSearch.planLevel.strc2010;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.trafficmonitoring.PessimisticTravelTimeAggregator;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.RunLib;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenario;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenarioOneLiner;

/**
 * 
 * @author wrashid
 * 
 */
public class Run15 {
	public static void main(String[] args) {
		int runNumber=RunLib.getRunNumber(new Object() { }.getClass().getEnclosingClass());
		RunSeries.getControler(runNumber).run();
	}

}
