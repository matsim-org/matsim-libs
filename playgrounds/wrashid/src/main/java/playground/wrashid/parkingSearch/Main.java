package playground.wrashid.parkingSearch;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.EventHandlerAtStartupAdder;

public class Main {
	public static void main(String[] args) {
		Controler controler;
		String configFilePath="C:\\data\\workspace\\matsim\\test\\scenarios\\equil\\config_plans1.xml";
		controler = new Controler(configFilePath);
		
		controler.setOverwriteFiles(true);
		
		EventHandlerAtStartupAdder eventHandlerAdder=new EventHandlerAtStartupAdder();
		eventHandlerAdder.addEventHandler(new ReplanParkingSearchRoute(controler));
		controler.addControlerListener(eventHandlerAdder);
		
		controler.run();
		
	}
}
