package playground.sebhoerl.avtaxi.dispatcher;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

import javax.inject.Provider;
import java.util.Collection;

public class SingleFIFODispatcherFactory implements AVDispatcherFactory {
    @Inject
    AVConfigGroup config;

    @Inject
    Provider<SingleFIFODispatcher> provider;

    @Override
    public AVDispatcher createDispatcher() {
        return provider.get(); //new SingleFIFODispatcher();
    }
}
