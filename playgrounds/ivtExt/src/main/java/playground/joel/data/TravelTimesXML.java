package playground.joel.data;

import playground.clruch.export.AVStatus;

/**
 * Created by Joel on 28.02.2017.
 */
//public class TravelTimesXML extends AbstractDataXML<AVStatus>
class TravelTimesXML extends AbstractDataXML<Double> {
    TravelTimesXML() {
        super("SimulationResult", "av", "id", "times", "start", "end");
    }
}