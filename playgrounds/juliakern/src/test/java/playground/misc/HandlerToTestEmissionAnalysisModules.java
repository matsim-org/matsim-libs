package playground.misc;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

public class HandlerToTestEmissionAnalysisModules implements EventsManager {
	static Double sumOverAll=0.0;

	@Override
	public EventsFactory getFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void processEvent(Event event) {	
		for(String attribute: event.getAttributes().keySet()){
			try {
				if (!(attribute.equals("time"))) {
					sumOverAll += Double.parseDouble(event.getAttributes().get(attribute));
				}
				
			} catch (NumberFormatException e) {
				String notANumber = event.getAttributes().get(attribute).toString();
				if(notANumber.equals("coldEmissionEvent")||notANumber.equals("coldEmissionEventLinkId")||notANumber.equals("personId")){
					//everything ok
					//TODO ueberdenken
				}else{
					//Assert.fail("this is not an expected cold emission event attribute: "+notANumber);
				}
				
			}
		}

	}

	@Override
	public void addHandler(EventHandler handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeHandler(EventHandler handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void resetHandlers(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void initProcessing() {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterSimStep(double time) {
		// TODO Auto-generated method stub

	}

	@Override
	public void finishProcessing() {
		// TODO Auto-generated method stub

	}
	
	//TODO
	public Double getFirstParameter(){
		return 10.0;
	}

	public static Double getSum() {
		return sumOverAll;

	}

	public static void reset() {
		sumOverAll=.0;
		// TODO Auto-generated method stub
		
	}

}
