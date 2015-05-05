package playground.wrashid.PSF.PSS;

import java.util.ArrayList;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;

public class EventReadControler extends Controler {

	private String pathToEventsFile;
	private static ArrayList<Event> buffer;

	public EventReadControler(String configFilename, String pathToEventsFile) {
		
		super(configFilename);
		this.pathToEventsFile=pathToEventsFile;
		// TODO Auto-generated constructor stub
		throw new RuntimeException( Gbl.RUN_MOB_SIM_NO_LONGER_POSSIBLE ) ;
	}
	
//	@Override
//	public void runMobSim(){
//		//if (buffer == null){
//			// the processing happens during the reading process
//			BufferedEventsReaderTXTv1 reader = new BufferedEventsReaderTXTv1(getEvents());
//			reader.readFile(pathToEventsFile);
//			buffer=reader.getBuffer();
//			/*
//		} else {
//			// if events buffered, process events directly
//			for (int i=0;i<buffer.size();i++){
//				events.processEvent(buffer.get(i));
//			}
//		}
//		*/
//	}
	
	

	
	
}
