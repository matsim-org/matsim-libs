package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;

/**
 * marker interface in order to be able to bind that specific departure handler for network mode departures.  kai, jan'25
 */
public interface NetworkModeDepartureHandler extends DepartureHandler{

}
