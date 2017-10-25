// code by jph
package playground.clruch.net;

import java.util.concurrent.CopyOnWriteArraySet;

import ch.ethz.idsc.queuey.view.util.net.ObjectHandler;

public class SimulationClientSet extends CopyOnWriteArraySet<ObjectHandler> {
    public static final SimulationClientSet INSTANCE = new SimulationClientSet();

    private SimulationClientSet() {
    }

}
