package playground.clruch.export;

/**
 * Created by Claudio on 2/2/2017.
 */

class VehicleStatusEventXML extends AbstractEventXML<AVStatus> {
    VehicleStatusEventXML() {
        super("SimulationResult", "av", "id", "event", "time", "status");
    }
}
