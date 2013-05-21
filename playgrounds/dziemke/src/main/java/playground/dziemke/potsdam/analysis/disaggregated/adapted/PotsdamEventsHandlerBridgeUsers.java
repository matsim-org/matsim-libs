package playground.dziemke.potsdam.analysis.disaggregated.adapted;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;


public class PotsdamEventsHandlerBridgeUsers implements LinkLeaveEventHandler{
	
	List <Id> personsBridge  = new ArrayList <Id>();
	Scenario scenario;
	Id linkid1;
	Id linkid2;

	
	public PotsdamEventsHandlerBridgeUsers(Scenario scenario, Id id1, Id id2) {
		this.scenario = scenario;
		this.linkid1 = id1;
		this.linkid2 = id2;
	}

	
	public void reset(int iteration) {
	}

	
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getLinkId().equals(linkid1) || event.getLinkId().equals(linkid2)){
			personsBridge.add(event.getPersonId());
		}
	}	
	
	
	// new; should render class "RemoveDuplicates" dispensable
//	public void handleEvent(LinkLeaveEvent event) {
//		if (event.getLinkId().equals(linkid1) || event.getLinkId().equals(linkid2)){
//			if (!personsBridge.contains(event.getPersonId())) {
//				personsBridge.add(event.getPersonId());
//			}
//		}
//	}	

	
	List<Id> getPersonsOnBridge() {
		return personsBridge;
	}
}
