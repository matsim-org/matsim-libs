package playground.clruch.trb18;

import org.matsim.core.controler.AbstractModule;
import playground.sebhoerl.avtaxi.framework.AVUtils;

public class TRBModule extends AbstractModule {
    @Override
    public void install() {
        AVUtils.registerGeneratorFactory(binder(), "TRB", TRBGenerator.Factory.class);
    }
}
