package playground.julia.misc;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

/**
 * minimalistic handler to compare calculate sums in some tests
 * @author julia
 *
 */
public class HandlerToTestEmissionAnalysisModules implements EventsManager {
	static Double sumOverAll=0.0;

	@Override
	public EventsFactory getFactory() {
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
				}else{
					//Assert.fail("this is not an expected cold emission event attribute: "+notANumber);
				}
				
			}
		}

	}

	@Override
	public void addHandler(EventHandler handler) {
	}

	@Override
	public void removeHandler(EventHandler handler) {

	}

	@Override
	public void resetHandlers(int iteration) {
	}

	@Override
	public void initProcessing() {

	}

	@Override
	public void afterSimStep(double time) {

	}

	@Override
	public void finishProcessing() {

	}

	public static Double getSum() {
		return sumOverAll;

	}

	public static void reset() {
		sumOverAll=.0;
		
	}

}
