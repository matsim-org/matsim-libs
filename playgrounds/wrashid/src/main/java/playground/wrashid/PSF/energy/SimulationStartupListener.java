package playground.wrashid.PSF.energy;

import java.util.LinkedList;

import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.ParametersPSFMutator;

public class SimulationStartupListener implements StartupListener {

	LinkedList<EventHandler> eventHandler = new LinkedList<EventHandler>();
	private MatsimServices controler;
	private ParametersPSFMutator parametersPSFMutator;

	public SimulationStartupListener(MatsimServices controler) {
		this.controler=controler;
	}

	public void addEventHandler(EventHandler eventHandler1) {
		this.eventHandler.add(eventHandler1);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// add handlers
		for (int i = 0; i < eventHandler.size(); i++) {
			event.getServices().getEvents().addHandler(eventHandler.get(i));
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
		
//		// initialize events
//		if (ParametersPSF.getEvents()==null){
//			ParametersPSF.setEvents(event.getServices().getEvents());
//		}
		
		
		
	}
	
	public void addParameterPSFMutator(ParametersPSFMutator parametersPSFMutator1){
		this.parametersPSFMutator = parametersPSFMutator1;
	}

}
