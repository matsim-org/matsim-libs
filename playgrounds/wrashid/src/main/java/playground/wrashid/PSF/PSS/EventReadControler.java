package playground.wrashid.PSF.PSS;

import java.util.ArrayList;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.Mobsim;

public class EventReadControler {

	private final Controler controler;
	private String pathToEventsFile;
	private static ArrayList<Event> buffer;

	public EventReadControler(Config configFilename, String pathToEventsFile) {
		
		controler = new Controler(configFilename);
		this.pathToEventsFile=pathToEventsFile;
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.bindMobsim().toInstance(new Mobsim() {
					@Override
					public void run() {
						runLocal();
					}
				});
			}
		});
		
	}
	
//	@Override
	private void runLocal(){
		//if (buffer == null){
			// the processing happens during the reading process
			buffer = new ArrayList<>();
			MatsimEventsReader reader = new MatsimEventsReader(controler.getEvents());
			controler.getEvents().addHandler(new BasicEventHandler() {
				@Override
				public void reset(int iteration) {
				}
	
				@Override
				public void handleEvent(Event event) {
					buffer.add(event);
				}
			});
			reader.readFile(pathToEventsFile);
			/*
		} else {
			// if events buffered, process events directly
			for (int i=0;i<buffer.size();i++){
				events.processEvent(buffer.get(i));
			}
		}
		*/
	}


	public Controler getControler() {
		return controler;
	}
}
