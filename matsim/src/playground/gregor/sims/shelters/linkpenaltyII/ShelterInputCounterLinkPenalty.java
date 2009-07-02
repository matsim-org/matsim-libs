package playground.gregor.sims.shelters.linkpenaltyII;

import java.util.HashMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.events.AgentMoneyEvent;
import org.matsim.core.events.Events;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.evacuation.base.Building;

public class ShelterInputCounterLinkPenalty implements LinkLeaveEventHandler {

	private static final Logger log = Logger.getLogger(ShelterInputCounterLinkPenalty.class);

	private static final double PENALTY_COEF = 5;
	
	private final HashMap<Id,LinkInfo> linkInfos = new HashMap<Id, LinkInfo>();
	private final HashMap<Id, Building> shelterLinkMapping;

	private final Events events;
	
	private int it;


	public ShelterInputCounterLinkPenalty(NetworkLayer network, HashMap<Id,Building> shelterLinkMapping, Events events) {

		this.events = events;
		this.shelterLinkMapping = shelterLinkMapping;
		for (LinkImpl link : network.getLinks().values()) {
			if (link.getId().toString().contains("sl") && link.getId().toString().contains("a")) {
				Building b = this.shelterLinkMapping.get(link.getId());
				this.linkInfos.put(link.getId(), new LinkInfo(b.getShelterSpace(),link.getId()));
			}
		}
	}

	
	public double getLinkTravelCost(LinkImpl link, double time) {
		LinkInfo li = this.linkInfos.get(link.getId()); 
		if (li != null) {
			if (time > li.blockingTime) {
				return PENALTY_COEF * (time - li.blockingTime);
			}
		}
		return 0.;
	}

	public void handleEvent(LinkLeaveEvent event) {
		LinkInfo li = this.linkInfos.get(event.getLinkId()); 
		if (li != null) {
			li.count++;
			if (li.space == li.count) {
				double n = this.it;
				li.blockingTime = n / (n+1) * li.blockingTime + 1/ (n+1) * event.getTime();
//				li.blockingTime = event.getTime();
			}
			if (li.count > li.space) {
				double pen = PENALTY_COEF * (event.getTime() - li.blockingTime) / -600;
				AgentMoneyEvent e = new AgentMoneyEvent(event.getTime(),event.getPersonId(),pen);
				this.events.processEvent(e);
			}
			
		}

	}

	public void reset(int iteration) {
		this.it = iteration;
		
		double maxOverload = 0; LinkInfo mxLi = null;
		for (LinkInfo li : this.linkInfos.values()) {
			if (li.space > li.count) {
				int n = this.it;
				li.blockingTime = n / (n+1) * li.blockingTime + 1/ (n+1) * 30*3600;
//				li.blockingTime = 30*3600;			
			}
			log.info("Link:" + li.id + "  count:" + li.count + " should be at most:" + li.space + " overload:" + (double)li.count/li.space);
			if ((double)li.count/li.space > maxOverload) {
				maxOverload = (double)li.count/li.space;
				mxLi = li;
			}
			li.count = 0;
		}
		if (mxLi != null){
			log.info("MAX  Link:" + mxLi.id + "  count:" + mxLi.count + " should be at most:" + mxLi.space + " overload:" + maxOverload);
		}

	}
	private static class LinkInfo {
		double blockingTime = 30*3600;
		int count = 0;
		final int space;
		final Id id;
		public LinkInfo(int shelterSpace, Id id) {
			this.id = id;
			this.space = shelterSpace;

		}
	}
}
