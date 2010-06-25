package playground.mzilske.deteval;

import java.util.ArrayList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.run.Controler;

public class Controller {

private org.matsim.core.controler.Controler controler;
	
	private ArrayList<EventHandler> eventHandlers = new ArrayList<EventHandler>();
	
	public Controller(final String[] args) {
		this.controler = new org.matsim.core.controler.Controler(args);
	}
	
	public Controller(final String configFilename) {
		this.controler = new org.matsim.core.controler.Controler(configFilename);
	}
	
	public void setOverwriteFiles(final boolean overwriteFiles) {
		this.controler.setOverwriteFiles(overwriteFiles);
	}
	
	public Scenario getScenario() {
		return this.controler.getScenario() ;
	}
	
	public void run() {
		ControlerListener startupListener = new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				for (EventHandler eventHandler : eventHandlers) {
					event.getControler().getEvents().addHandler(eventHandler);
				}
			}
			
		};
		this.controler.addControlerListener(startupListener);
		this.controler.run();
	}
	
	public void addEventHandler(EventHandler eventHandler) {
		eventHandlers.add(eventHandler);
	}

	public static void main(String[] args) {
		new Controler(args).run();
	}
	
}
