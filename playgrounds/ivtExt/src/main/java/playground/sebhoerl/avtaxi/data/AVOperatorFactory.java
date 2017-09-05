package playground.sebhoerl.avtaxi.data;

import org.matsim.api.core.v01.Id;

import com.google.inject.Singleton;

import playground.sebhoerl.avtaxi.config.AVOperatorConfig;

@Singleton
public class AVOperatorFactory {
    public AVOperator createOperator(Id<AVOperator> id, AVOperatorConfig config) {
        return new AVOperatorImpl(id, config);
    }
}
