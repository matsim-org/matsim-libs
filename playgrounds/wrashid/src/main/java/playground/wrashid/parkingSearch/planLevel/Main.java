package playground.wrashid.parkingSearch.planLevel;

import org.matsim.core.controler.Controler;

import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.parkingSearch.ReplanParkingSearchRoute;

public class Main {
	public static void main(String[] args) {
		Controler controler;
		String configFilePath="../playgrounds/wrashid/test/input/playground/wrashid/parkingSearch/planLevel/chessConfig2.xml";
		controler = new Controler(configFilePath);
		
		controler.setOverwriteFiles(true);
		
		EventHandlerAtStartupAdder eventHandlerAdder=new EventHandlerAtStartupAdder();
		eventHandlerAdder.addEventHandler(new ParkingBookKeeper(controler));
		controler.addControlerListener(eventHandlerAdder);
		
		
		
		controler.run();
	}
}
