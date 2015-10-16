package playground.mkillat.staedtebau;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;


public class PotsdamEventHandlerPersonUseBridge implements LinkLeaveEventHandler{
	
	Map <Id<Person>, Double> personsBridge  = new HashMap<>();
	Scenario scenario;
	Id<Link> linkid1;
	Id<Link> linkid2;
	Id<Link> linkid3;
	Id<Link> linkid4;
	Id<Link> linkid5;
	Id<Link> linkid6;
	Id<Link> linkid7;
	Id<Link> linkid8;
	
	
	public PotsdamEventHandlerPersonUseBridge(Scenario scenario, Id<Link> id1, Id<Link> id2, Id<Link> id3, Id<Link> id4, Id<Link> id5, Id<Link> id6, Id<Link> id7, Id<Link> id8) {//wird nur einmal ausgeführt
	this.linkid1 = id1;
	this.linkid2 = id2;
	this.linkid3 = id3;
	this.linkid4 = id4;
	this.linkid5 = id5;
	this.linkid6 = id6;
	this.linkid7 = id7;
	this.linkid8 = id8;
	
		
	}
	
	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getLinkId().equals(linkid1) || event.getLinkId().equals(linkid2) || event.getLinkId().equals(linkid3)|| event.getLinkId().equals(linkid4) || event.getLinkId().equals(linkid5) || event.getLinkId().equals(linkid6) || event.getLinkId().equals(linkid7) || event.getLinkId().equals(linkid8)){
			if (personsBridge.containsKey(event.getDriverId())==false){
				personsBridge.put(event.getDriverId(), event.getTime());
			}
			;
		}
	}	

	Map<Id<Person>, Double> getPersonsOnBridge() {
		return personsBridge;
	}
}
