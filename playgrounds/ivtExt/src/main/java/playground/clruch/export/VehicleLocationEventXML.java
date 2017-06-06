// code by clruch
package playground.clruch.export;

class VehicleLocationEventXML extends AbstractEventXML<String> {
    VehicleLocationEventXML() {
        super("SimulationResult", "av", "id", "event", "time", "link");
    }
}
