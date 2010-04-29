package playground.wrashid.parkingSearch.planLevel;

import org.matsim.core.controler.Controler;

import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.parkingSearch.ReplanParkingSearchRoute;

public class Main {
	public static void main(String[] args) {
		Controler controler;
		String configFilePath="test/input/playground/wrashid/parkingSearch/planLevel/chessConfig2.xml";
		controler = new Controler(configFilePath);
		
		controler.setOverwriteFiles(true);
		
		controler.run();
	}
}
