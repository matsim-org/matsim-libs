// code by jph
package playground.clruch.net;

import org.matsim.core.controler.AbstractModule;

public class DatabaseModule extends AbstractModule {
    @Override
    public void install() {
        addControlerListenerBinding().to(DatabaseIterationStartsListener.class);
        addControlerListenerBinding().to(DatabaseIterationEndsListener.class);
    }
}
