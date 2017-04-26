package playground.sebhoerl.avtaxi.data;

import org.matsim.api.core.v01.Id;
import playground.sebhoerl.avtaxi.config.AVOperatorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
