package playground.sebhoerl.ant.handlers;

import org.matsim.core.events.handler.EventHandler;
import playground.sebhoerl.ant.DataFrame;

public abstract class AbstractHandler implements EventHandler {
    final protected DataFrame data;

    public AbstractHandler(DataFrame data) {
        this.data = data;
    }

    abstract protected void finish();

    @Override
    public void reset(int iteration) {
        finish();
    }
}
