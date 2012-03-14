package playground.gregor.sim2d_v2.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;



public class OnLinkStatistics implements LinkEnterEventHandler, LinkLeaveEventHandler {


	private final Map<Id,LinkInfo> linkInfos = new HashMap<Id,LinkInfo>();

	int time = 0;

	@Override
	public void reset(int iteration) {
		for (LinkInfo li : this.linkInfos.values()) {
			li.timeDepCounts.clear();
		}
	}

	public void handleEventH(Event event) {
		int t = (int)(0.5+event.getTime());
		if (t > this.time) {
			for (LinkInfo li : this.linkInfos.values()) {
				Count c = li.timeDepCounts.get(this.time);
				if (c == null) {
					c = new Count();
					li.timeDepCounts.put(this.time, c);
				}
				c.onLink = li.onLink;
				this.time = t;
			}
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		handleEventH(event);
		LinkInfo li = this.linkInfos.get(event.getLinkId());
		if (li != null) {
			if (!li.agents.containsKey(event.getPersonId())) {
				return;
			}
			LinkEnterEvent e = li.agents.remove(event.getPersonId());
			double t = e.getTime();
			int intTime = (int) (0.5 + event.getTime());
			Count count2 = li.timeDepCounts.get(intTime);
			if (count2 == null) {
				count2 = new Count();
				li.timeDepCounts.put(intTime, count2);
			}
			count2.left++;
			AgentInfo ai = new AgentInfo();
			ai.enterTime = t;
			ai.travelTime = event.getTime() - e.getTime();
			li.agentsList.add(ai);
			li.onLink--;
		}

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		handleEventH(event);
		LinkInfo li = this.linkInfos.get(event.getLinkId());
		if (li != null) {
			int time = (int)(0.5+event.getTime());
			Count count = li.timeDepCounts.get(time);
			if (count == null) {
				count = new Count();
				li.timeDepCounts.put(time, count);
			}
			li.agents.put(event.getPersonId(), event);
			count.entered++;
			li.onLink++;
		}
	}

	public double[] getEnterHistory(Id linkId, double time, int steps) {
		LinkInfo li = this.linkInfos.get(linkId);
		if (li == null) {
			throw new IllegalArgumentException("no information available for link with id:" + linkId);
		}
		double[] ret = new double[steps];
		int intTime = (int) (0.5 + time);
		for (int i = 0; i < steps; i++) {
			Count c = li.timeDepCounts.get(intTime + i);
			if (c == null) {
				ret[i] = 0;
			} else {
				ret[i] = c.entered;
			}
		}
		return ret;
	}
	
	public double[] getLeaveHistory(Id linkId, double time, int steps) {
		LinkInfo li = this.linkInfos.get(linkId);
		if (li == null) {
			throw new IllegalArgumentException("no information available for link with id:" + linkId);
		}
		double[] ret = new double[steps];
		int intTime = (int) (0.5 + time);
		for (int i = 0; i < steps; i++) {
			Count c = li.timeDepCounts.get(intTime + i);
			if (c == null) {
				ret[i] = 0;
			} else {
				ret[i] = c.left;
			}
		}
		return ret;
	}

	public double[] getOnLinkHistory(Id linkId, double time, int steps) {
		LinkInfo li = this.linkInfos.get(linkId);
		if (li == null) {
			throw new IllegalArgumentException("no information available for link with id:" + linkId);
		}
		int intTime = (int) (0.5 + time);
		double[] ret = new double[steps];
		for (int i = 0; i < steps; i++) {
			Count c = li.timeDepCounts.get(intTime);
			if (c == null) {
				ret[i] = 0;
			} else {
				ret[i] = c.onLink;
			}
		}
		return ret;
	}

	public void addObservationLinkId(Id id) {
		LinkInfo li = new LinkInfo();
		this.linkInfos.put(id, li);

	}

	private static final class LinkInfo {
		public List<AgentInfo> agentsList = new ArrayList<AgentInfo>();
		TreeMap<Integer,Count> timeDepCounts = new TreeMap<Integer, Count>();
		Map<Id,LinkEnterEvent> agents = new HashMap<Id,LinkEnterEvent>();
		int onLink = 0;

	}
	private static final class Count {
		public int left;
		public int onLink;
		int entered = 0;
	}
	
	public static class AgentInfo {
		public double enterTime;
		public double travelTime;
	}

	public List<AgentInfo> getAndRemoveAgentInfos(Id linkId) {
		LinkInfo li = this.linkInfos.get(linkId);
		List<AgentInfo> ret = li.agentsList;
		li.agentsList = new ArrayList<AgentInfo>();
		return ret;
	}







}
