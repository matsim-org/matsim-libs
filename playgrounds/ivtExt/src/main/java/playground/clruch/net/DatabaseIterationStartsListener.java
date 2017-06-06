// code by jph
package playground.clruch.net;

import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

/* package */ class DatabaseIterationStartsListener implements IterationStartsListener {
    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        MatsimStaticDatabase.INSTANCE.setIteration(event.getIteration());
    }
}
