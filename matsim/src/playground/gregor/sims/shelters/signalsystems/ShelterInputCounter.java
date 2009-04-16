package playground.gregor.sims.shelters.signalsystems;

import java.util.HashMap;
import java.util.SortedMap;
import java.util.Map.Entry;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationInitializedEvent;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationInitializedListener;
import org.matsim.core.network.NetworkLayer;
import org.matsim.signalsystems.control.SignalSystemControler;

import playground.gregor.sims.shelters.Building;

public class ShelterInputCounter implements LinkLeaveEventHandler, AfterMobsimListener, QueueSimulationInitializedListener {

//	private static final int MAX_CAP = 50;
	private SortedMap<Id, SignalSystemControler> scs;
	private final HashMap<Id,Counter> counts = new HashMap<Id, Counter>();
	private final HashMap<Id, Building> shelterLinkMapping;
	
	public ShelterInputCounter(NetworkLayer network, HashMap<Id,Building> shelterLinkMapping) {
		
		this.shelterLinkMapping = shelterLinkMapping;
		
		for (Link link : network.getLinks().values()) {
			if (link.getId().toString().contains("sl") && link.getId().toString().contains("a")) {
				this.counts.put(link.getId(), new Counter());
			}
		}
	}

	public void handleEvent(LinkLeaveEvent event) {
		Counter c = this.counts.get(event.getLink()); 
		if (c != null) {
			c.count++;
//			if (c.count > MAX_CAP) {
//				AgentMoneyEvent e = new AgentMoneyEvent(event.getTime(),event.getPersonId(),-(c.count - MAX_CAP)/10);
//				QueueSimulation.getEvents().processEvent(e);
//			}			
		}
		
	}
	
	boolean getShelterOfLinkHasSpace(Id linkRefId) {
		Counter c = this.counts.get(linkRefId);
		Building b = this.shelterLinkMapping.get(linkRefId);
		
		
		return c.count <= b.getShelterSpace();
	}

	public void reset(int iteration) {
		for (Counter c : this.counts.values()) {
			c.count = 0;
		}
	}

	public void notifySimulationInitialized(QueueSimulationInitializedEvent e) {
		this.scs = e.getQueueSimulation().getSignalSystemControlerBySystemId();
		for (SignalSystemControler ssc : this.scs.values()) {
			if (!(ssc instanceof SheltersDoorBlockerController) ){
				throw new RuntimeException("wrong SignalsystemsController type!");
			}
			((SheltersDoorBlockerController)ssc).shelterInputCounter(this);
		}
		
	}

	public void notifyAfterMobsim(AfterMobsimEvent event) {
		for (Entry<Id, Counter> e : this.counts.entrySet()) {
			System.out.println("Link:" + e.getKey() + "  count:" + e.getValue().count);
		}
		
	}

	private static class Counter {
		int count = 0;
	}




}
