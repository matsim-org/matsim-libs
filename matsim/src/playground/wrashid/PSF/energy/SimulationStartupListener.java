package playground.wrashid.PSF.energy;

import java.util.LinkedList;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;
import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.ParametersPSFMutator;

public class SimulationStartupListener implements StartupListener {

	LinkedList<EventHandler> eventHandler = new LinkedList<EventHandler>();
	private Controler controler;
	private ParametersPSFMutator parametersPSFMutator;

	public SimulationStartupListener(Controler controler) {
		this.controler=controler;
	}

	public void addEventHandler(EventHandler eventHandler) {
		this.eventHandler.add(eventHandler);
	}

	public void notifyStartup(StartupEvent event) {
		// add handlers
		for (int i = 0; i < eventHandler.size(); i++) {
			event.getControler().getEvents().addHandler(eventHandler.get(i));
		}
		
		// read config parameters
		ParametersPSF.readConfigParamters(controler);
		
		// modify config parameters
		if (parametersPSFMutator!=null){
			parametersPSFMutator.mutateParameters();
		}
		
		// if any thing needs to be processed after mutation of the parameters,
		// process it
		ParametersPSF.postMutationProcessing();
		
		// initialize events
		if (ParametersPSF.getEvents()==null){
			ParametersPSF.setEvents(event.getControler().getEvents());
		}
	}
	
	public void addParameterPSFMutator(ParametersPSFMutator parametersPSFMutator){
		this.parametersPSFMutator = parametersPSFMutator;
	}

}
