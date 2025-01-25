package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;

public interface NetworkModeDepartureHandler extends DepartureHandler{
	@Override boolean handleDeparture( double now, MobsimAgent agent, Id<Link> linkId );
}
