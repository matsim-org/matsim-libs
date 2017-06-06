// code by jph
package playground.clruch.net;

import java.util.Collections;

public class DummyStorageSupplier extends StorageSupplier {

    public DummyStorageSupplier() {
        super(Collections.emptyNavigableMap());
    }

    @Override
    public SimulationObject getSimulationObject(int index) throws Exception {
        return null;
    }

    @Override
    @Deprecated // not used
    protected SimulationObject getSimulationObjectForTime(int now) throws Exception {
        return null;
    }

}
