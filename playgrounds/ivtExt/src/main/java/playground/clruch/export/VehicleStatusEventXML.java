// code by clruch
package playground.clruch.export;

import playground.clruch.dispatcher.core.AVStatus;

/**
 * Created by Claudio on 2/2/2017.
 */

class VehicleStatusEventXML extends AbstractEventXML<AVStatus> {
    VehicleStatusEventXML() {
        super("SimulationResult", "av", "id", "event", "time", "status");
    }
}
