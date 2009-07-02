package playground.gregor.sims.confluent;

import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentStuckEventHandler;
import org.matsim.core.network.LinkImpl;

public interface LinkPenalty extends BasicLinkEnterEventHandler, AfterMobsimListener, AgentStuckEventHandler, AgentArrivalEventHandler {
	public double getLinkCost(LinkImpl link);
}
