package playground.sebhoerl.avtaxi.data;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVOperatorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class AVOperatorFactory {
    public AVOperator createOperator(Id<AVOperator> id, AVOperatorConfig config) {
        return new AVOperatorImpl(id, config);
    }
}
