// code by jph
package playground.clruch.net;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

/* package */ class DatabaseIterationEndsListener implements IterationEndsListener {

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        MatsimStaticDatabase.INSTANCE.setIteration(null);
    }

}
