package playground.wrashid.bsc.vbmh.vmParking;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * Sets up event handlers and does some restes after and before each.
 * 
 * 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class ParkControlerListener implements StartupListener, IterationEndsListener, IterationStartsListener{
	private ParkHandler parkHandler = new ParkHandler();

	public ParkHandler getParkHandler() {
		return parkHandler;
	}



	public void setParkHandler(ParkHandler parkHandler) {
		this.parkHandler = parkHandler;
	}



	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		
		//PH Writer beenden damit File geschlossen und geschrieben wird
		ParkHistoryWriter phwriter = new ParkHistoryWriter();
		phwriter.end();
		
		//Park Statistik Drucken und zuruecksetzen:
		this.getParkHandler().parkControl.printStatistics();
		this.getParkHandler().parkControl.resetStatistics();
	}
	
		

	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub
		
		event.getControler().getEvents().addHandler(getParkHandler());
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// TODO Auto-generated method stub
		
		//Park History Writer Starten: 
		ParkHistoryWriter phwriter = new ParkHistoryWriter();
		phwriter.start("output/test_outputs/test_parkhistory.xml"); // !! Pro Iteration neues File
		
		//Parkplaetze zuruecksetzen 
		getParkHandler().parkControl.parkingMap.clearSpots();
		getParkHandler().parkControl.parkingMap.createSpots();
		
		
		//VM_Score_Keeper Zuruecksetzen:
		Map<Id, ? extends Person> population = event.getControler().getPopulation().getPersons();
		for (Person person : population.values()){
			//person.getCustomAttributes().put("VMScoreKeeper", null);
			person.getCustomAttributes().remove("VMScoreKeeper");
		}
		
		
		
	}

}
