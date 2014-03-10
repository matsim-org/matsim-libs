package playground.wrashid.bsc.vbmh.vm_parking;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;

public class Park_Controler_Listener implements StartupListener, IterationEndsListener, IterationStartsListener{
	public Park_handler park_handler = new Park_handler();
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		
		//PH Writer beenden damit File geschlossen und geschrieben wird
		Park_History_Writer phwriter = new Park_History_Writer();
		phwriter.end();
		
		//Park Statistik Drucken und zuruecksetzen:
		this.park_handler.park_control.print_statistics();
		this.park_handler.park_control.reset_statistics();
	}
	
		

	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub
		
		event.getControler().getEvents().addHandler(park_handler);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// TODO Auto-generated method stub
		
		//Park History Writer Starten: 
		Park_History_Writer phwriter = new Park_History_Writer();
		phwriter.start("output/test_outputs/test_parkhistory.xml"); // !! Pro Iteration neues File
		
		//Parkplaetze zuruecksetzen 
		park_handler.park_control.parking_map.clear_spots();
		park_handler.park_control.parking_map.create_spots();
		
		
		//VM_Score_Keeper Zuruecksetzen:
		Map<Id, ? extends Person> population = event.getControler().getPopulation().getPersons();
		for (Person person : population.values()){
			person.getCustomAttributes().put("VM_Score_Keeper", null);
		}
		
		
		
	}

}
