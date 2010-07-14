package playground.wrashid.parkingSearch.planLevel.occupancy;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.parkingSearch.planLevel.BaseScenario;
import playground.wrashid.parkingSearch.planLevel.init.InitializeParkings;

import junit.framework.TestCase;

public class ParkingOccupancyMaintainerTest extends TestCase implements ShutdownListener{
	
	ParkingBookKeeper parkingBookKeeper=null;
	
	public void testBasic(){
		Controler controler;
		String configFilePath="test/input/playground/wrashid/parkingSearch/planLevel/chessConfig3.xml";
		controler = new Controler(configFilePath);
		
		BaseScenario bs= new BaseScenario(controler);
		parkingBookKeeper=bs.parkingBookKeeper;
		
		controler.addControlerListener(this);
		
		controler.run();
	}

	/**
	 * add test just before shutdown of the system.
	 */
	public void notifyShutdown(ShutdownEvent event) {
		ParkingOccupancyBins pob=parkingBookKeeper.getParkingOccupancyMaintainer().getParkingOccupancyBins().get(new IdImpl(36));
		
		assertEquals(3, pob.getOccupancy(38000));
		
		pob=parkingBookKeeper.getParkingOccupancyMaintainer().getParkingOccupancyBins().get(new IdImpl(1));
		assertEquals(3, pob.getOccupancy(0));
	}

	
	
}
