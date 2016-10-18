package playground.sebhoerl.avtaxi.config;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import playground.sebhoerl.avtaxi.data.AVOperator;

public class AVOperatorConfig extends ReflectiveConfigGroup {
    final static String OPERATOR = "operator";

    final private AVConfig parentConfig;
    final private Id<AVOperator> id;
    final private AVTimingParameters timingParameters;

    private AVDispatcherConfig dispatcherConfig = null;

    public AVOperatorConfig(String id, AVConfig parentConfig) {
        super(OPERATOR);
        this.parentConfig = parentConfig;
        this.timingParameters = new AVTimingParameters(parentConfig.getTimingParameters());
        this.id = Id.create(id, AVOperator.class);
    }

    public Id<AVOperator> getId() {
        return id;
    }

    public AVTimingParameters getTimingParameters() {
        return timingParameters;
    }

    public AVDispatcherConfig getDispatcherConfig() {
        return dispatcherConfig;
    }

    public AVDispatcherConfig createDispatcherConfig(String name) {
        dispatcherConfig = new AVDispatcherConfig(name);
        return dispatcherConfig;
    }
}
