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

	private static final double MAX_CAP = 50;
	private SortedMap<Id, SignalSystemControler> scs;
	private final HashMap<Id,Counter> counts = new HashMap<Id, Counter>();
	private final HashMap<Id, Building> shelterLinkMapping;
	private int it = 0;
	
	public ShelterInputCounter(NetworkLayer network, HashMap<Id,Building> shelterLinkMapping) {
		
		this.shelterLinkMapping = shelterLinkMapping;
		
		for (Link link : network.getLinks().values()) {
			if (link.getId().toString().contains("sl") && link.getId().toString().contains("a")) {
				this.counts.put(link.getId(), new Counter());
			}
		}
	}

	public void handleEvent(LinkLeaveEvent event) {
//		if (event.getLink().getId().toString().contains("s")){
//			System.out.println(event.getLink().getId());
//			System.out.println(event.getLinkId());
//			System.out.println();
//		}
		Counter c = this.counts.get(event.getLink().getId()); 
		if (c != null) {
			Building b = this.shelterLinkMapping.get(event.getLink().getId());
			c.count++;
//			if (c.count > b.getShelterSpace()) {
//				AgentMoneyEvent e = new AgentMoneyEvent(event.getTime(),event.getPersonId(),-(c.count - b.getShelterSpace())/10);
//				QueueSimulation.getEvents().processEvent(e);
//			}

		}
		
	}
	
	boolean getShelterOfLinkHasSpace(Id linkRefId) {
		if (this.it >= 200) {
			return true;
		}
		Counter c = this.counts.get(linkRefId);
		Building b = this.shelterLinkMapping.get(linkRefId);
		
		if (c.count <= b.getShelterSpace()) {
			return true;
		}
		else if (!c.open) {
			return false;
		}
		c.open = false;
//		c.open = (MatsimRandom.getRandom().nextDouble()/2) > (c.count/b.getShelterSpace()-1);
		
//		return MatsimRandom.getRandom().nextDouble() < (1/60); 
		return c.open;
	}

	public void reset(int iteration) {
		this.it = iteration;
		int c = 0;
		double o = 0;
		for (Entry<Id, Counter> e : this.counts.entrySet()) {
			int count = e.getValue().count;
			int max = this.shelterLinkMapping.get(e.getKey()).getShelterSpace();
			double overload = (double)count / max;
			System.out.println("Link:" + e.getKey() + "  count:" + count + " should be at most:" + max + " overload:" + overload);
			e.getValue().count = 0;
			c++;
			o+=overload;
		}
		System.err.println("avgOverload:" + o/c);
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

		
	}

	private static class Counter {
		int count = 0;
		boolean open = true;
	}

//	public void handleEvent(LaneEnterEvent event) {
//		System.err.println("LaneEnterEvent " + event.getLaneId());
//	}
//
//	public void handleEvent(LaneLeaveEvent event) {
//		System.err.println("LaneLeaveEvent " + event.getLaneId());
//	}




}
