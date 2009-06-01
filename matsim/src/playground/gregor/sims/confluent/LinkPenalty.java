package playground.gregor.sims.confluent;

import org.matsim.core.api.network.Link;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentStuckEventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;

public interface LinkPenalty extends LinkEnterEventHandler, AfterMobsimListener, AgentStuckEventHandler, AgentArrivalEventHandler {
	public double getLinkCost(Link link);
}
