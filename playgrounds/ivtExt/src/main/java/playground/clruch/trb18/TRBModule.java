package playground.clruch.trb18;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.name.Named;

import playground.sebhoerl.avtaxi.framework.AVUtils;

public class TRBModule extends AbstractModule {
    final private Network network;

    public TRBModule(Network network) {
        this.network = network;
    }

    @Override
    public void install() {
        AVUtils.registerGeneratorFactory(binder(), "TRB", TRBGenerator.Factory.class);
    }

    @Provides @Named("trb_reduced")
    public Network provideReducedTRBNetwork() {
        return network;
    }
}
