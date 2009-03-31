package playground.gregor.sims.riskaversion;

import org.matsim.core.api.network.Link;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.handler.LinkEnterEventHandler;

public interface RiskCostCalculator extends LinkEnterEventHandler {
	
	
	public double getLinkRisk(final Link link, final double time);
	
	public void handleEvent(final LinkEnterEvent event);

}
