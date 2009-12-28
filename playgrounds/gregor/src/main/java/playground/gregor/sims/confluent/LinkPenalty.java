package playground.gregor.sims.confluent;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.controler.listener.AfterMobsimListener;

public interface LinkPenalty extends LinkEnterEventHandler, AfterMobsimListener, AgentStuckEventHandler, AgentArrivalEventHandler {
	public double getLinkCost(Link link);
}
