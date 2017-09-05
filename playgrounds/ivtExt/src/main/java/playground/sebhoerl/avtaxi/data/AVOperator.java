package playground.sebhoerl.avtaxi.data;

import org.matsim.api.core.v01.Id;

import playground.sebhoerl.avtaxi.config.AVOperatorConfig;

public interface AVOperator {
    Id<AVOperator> getId();
    AVOperatorConfig getConfig();
}
