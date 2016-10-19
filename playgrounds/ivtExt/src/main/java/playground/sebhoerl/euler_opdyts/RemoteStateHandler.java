package playground.sebhoerl.euler_opdyts;

/**
 * Created by sebastian on 11/10/16.
 */

import floetteroed.utilities.math.Vector;
import org.matsim.core.events.handler.EventHandler;

interface RemoteStateHandler extends EventHandler {
    Vector getState();
}
