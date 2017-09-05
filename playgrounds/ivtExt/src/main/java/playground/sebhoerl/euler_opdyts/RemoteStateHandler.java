package playground.sebhoerl.euler_opdyts;

import org.matsim.core.events.handler.EventHandler;

/**
 * Created by sebastian on 11/10/16.
 */

import floetteroed.utilities.math.Vector;

interface RemoteStateHandler extends EventHandler {
    Vector getState();
}
