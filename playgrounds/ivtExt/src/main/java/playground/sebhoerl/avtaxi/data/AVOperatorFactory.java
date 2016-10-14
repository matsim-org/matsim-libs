package playground.sebhoerl.avtaxi.data;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcherFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class AVOperatorFactory {
    @Inject
    Injector injector;

    @Inject
    Map<String, Class<? extends AVDispatcher>> dispatcherStrategies;

    public AVOperator createOperator(String id, String dispatchmentStrategy) {
        /*AVDispatcherFactory dispatcherFactory = dispatcherFactories.get(dispatchmentStrategy);

        if (dispatcherFactory == null) {
            throw new IllegalArgumentException("Dispatchment strategy '" + dispatchmentStrategy + "' does not exist.");
        }*/

        System.out.println(dispatchmentStrategy);
        System.out.println(dispatcherStrategies.get(dispatchmentStrategy));

        return new AVOperatorImpl(
                Id.create(id, AVOperator.class),
                injector.getInstance(dispatcherStrategies.get(dispatchmentStrategy)));
    }
}
