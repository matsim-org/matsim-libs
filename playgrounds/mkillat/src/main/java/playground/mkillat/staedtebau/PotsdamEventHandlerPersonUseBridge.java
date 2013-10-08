package playground.mkillat.staedtebau;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;


public class PotsdamEventHandlerPersonUseBridge implements LinkLeaveEventHandler{
	
	Map <Id, Double> personsBridge  = new HashMap<Id, Double>();
	Scenario scenario;
	Id linkid1;
	Id linkid2;
	Id linkid3;
	Id linkid4;
	Id linkid5;
	Id linkid6;
	Id linkid7;
	Id linkid8;
	
	
	public PotsdamEventHandlerPersonUseBridge(Scenario scenario, Id id1, Id id2, Id id3, Id id4, Id id5, Id id6, Id id7, Id id8) {//wird nur einmal ausgef�hrt
	this.linkid1 = id1;
	this.linkid2 = id2;
	this.linkid3 = id3;
	this.linkid4 = id4;
	this.linkid5 = id5;
	this.linkid6 = id6;
	this.linkid7 = id7;
	this.linkid8 = id8;
	
		
	}
	
	public void reset(int iteration) {
	}

	public void handleEvent(LinkLeaveEvent event) {
		if (event.getLinkId().equals(linkid1) || event.getLinkId().equals(linkid2) || event.getLinkId().equals(linkid3)|| event.getLinkId().equals(linkid4) || event.getLinkId().equals(linkid5) || event.getLinkId().equals(linkid6) || event.getLinkId().equals(linkid7) || event.getLinkId().equals(linkid8)){
			if (personsBridge.containsKey(event.getPersonId())==false){
				personsBridge.put(event.getPersonId(), event.getTime());
			}
			;
		}
	}	

	Map<Id, Double> getPersonsOnBridge() {
		return personsBridge;
	}
}
