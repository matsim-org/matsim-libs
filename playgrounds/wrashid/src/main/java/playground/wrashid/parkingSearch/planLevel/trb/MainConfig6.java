package playground.wrashid.parkingSearch.planLevel.trb;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.trafficmonitoring.PessimisticTravelTimeAggregator;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenario;

/**
 * 
 * @author wrashid
 * 
 */
public class MainConfig6 {
	public static void main(String[] args) {
		Controler controler;
		String configFilePath = "test/input/playground/wrashid/parkingSearch/planLevel/chessConfig6.xml";
		controler = new Controler(configFilePath);

		new BaseControlerScenario(controler);

		initPersonGroupsForStatistics();
		GlobalRegistry.doPrintGraficDataToConsole=true;

		controler.run();
	}

	private static void initPersonGroupsForStatistics() {
		PersonGroups personGroupsForStatistics = new PersonGroups();

		for (int i = 0; i <= 99; i++) {
			personGroupsForStatistics.addPersonToGroup(
					"Group-" + Integer.toString(i / 50 + 1), new IdImpl(i));
		}

		ParkingRoot.setPersonGroupsForStatistics(personGroupsForStatistics);
	}

}
