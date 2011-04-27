package playground.wrashid.parkingChoice.strcChess;

import java.util.LinkedList;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.parkingChoice.ParkingManager;
import playground.wrashid.parkingChoice.ParkingScoring;
import playground.wrashid.parkingChoice.infrastructure.Parking;

public class ChessScenarioParking {

	public static void main(String[] args) {
		Config config=ConfigUtils.loadConfig("C:/data/workspace/playgrounds/wrashid/test/input/playground/wrashid/parkingChoice/chessConfig.xml");
		Controler controler=new Controler(config);
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		ParkingTimesPlugin parkingTimesPlugin=new ParkingTimesPlugin(controler);
		
		
		LinkedList<Parking> parkingCollection = new LinkedList<Parking>();

		for (int i=0;i<10;i++){
			for (int j=0;j<10;j++){
				Parking parking = new Parking(new CoordImpl(i*1000+500,j*1000+500));
				parking.setMaxCapacity(1);
				parkingCollection.add(parking);
			}
		}
		
		
		
		ParkingManager parkingManager = new ParkingManager(controler, parkingCollection);
		controler.addControlerListener(parkingManager);
		controler.addControlerListener(new ParkingScoring(parkingTimesPlugin,controler, parkingManager));
		
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		
		controler.setOverwriteFiles(true);
		
		controler.run();		
	}
	
}
