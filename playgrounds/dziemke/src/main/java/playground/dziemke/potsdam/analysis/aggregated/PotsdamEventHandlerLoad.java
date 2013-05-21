package playground.dziemke.potsdam.analysis.aggregated;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;


public class PotsdamEventHandlerLoad implements LinkLeaveEventHandler{
	
	Map<Id, Integer> load = new HashMap<Id, Integer>();
	Scenario scenario;
	
	public PotsdamEventHandlerLoad(Scenario scenario) {//wird nur einmal ausgef�hrt
		this.scenario = scenario;
		for(Id id : scenario.getNetwork().getLinks().keySet()) {
			this.load.put(id, 0); 
		}
	}
	
	public void reset(int iteration) {
	}

	public void handleEvent(LinkLeaveEvent event) {
		int n = load.get(event.getLinkId());
		n++;
		load.put(event.getLinkId(), n);			
	}	

	Integer getLoad(Id id) {
		return load.get(id);
	}
}
