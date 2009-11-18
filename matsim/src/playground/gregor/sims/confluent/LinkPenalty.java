package playground.gregor.sims.confluent;

import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentStuckEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.listener.AfterMobsimListener;

public interface LinkPenalty extends BasicLinkEnterEventHandler, AfterMobsimListener, BasicAgentStuckEventHandler, BasicAgentArrivalEventHandler {
	public double getLinkCost(Link link);
}
