package playground.sebhoerl.avtaxi.data;

import org.matsim.api.core.v01.Id;

import playground.sebhoerl.avtaxi.config.AVOperatorConfig;

public class AVOperatorImpl implements AVOperator {
    final private Id<AVOperator> id;
    private final AVOperatorConfig config;

    public AVOperatorImpl(Id<AVOperator> id, AVOperatorConfig config) {
        this.id = id;
        this.config = config;
    }

    @Override
    public Id<AVOperator> getId() {
        return id;
    }

    @Override
    public AVOperatorConfig getConfig() {
        return config;
    }
}
