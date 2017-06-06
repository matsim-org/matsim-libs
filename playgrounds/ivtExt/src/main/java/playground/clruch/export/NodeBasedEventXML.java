// code by clruch
package playground.clruch.export;

/**
 * Created by Claudio on 1/26/2017.
 */
class NodeBasedEventXML extends AbstractEventXML<Integer> {
    NodeBasedEventXML() {
        super("SimulationResult", "link", "id", "event", "time", "numCustWait");
    }
}
