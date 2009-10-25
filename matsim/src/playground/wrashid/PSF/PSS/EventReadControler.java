package playground.wrashid.PSF.PSS;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsReaderTXTv1;

public class EventReadControler extends Controler {

	private String pathToEventsFile;

	public EventReadControler(String configFilename, String pathToEventsFile) {
		
		super(configFilename);
		this.pathToEventsFile=pathToEventsFile;
		// TODO Auto-generated constructor stub
	}
	
	public void runMobSim(){
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		reader.readFile(pathToEventsFile);
	}
	
	

	
	
}
