package playground.balac.allcsmodestest.events.handler;

import org.matsim.core.events.handler.EventHandler;

import playground.balac.allcsmodestest.events.CarsharingLegFinishedEvent;


public interface CarsharingLegFinishedEvenHandler extends EventHandler{

	public void handleEvent (CarsharingLegFinishedEvent event);


}
