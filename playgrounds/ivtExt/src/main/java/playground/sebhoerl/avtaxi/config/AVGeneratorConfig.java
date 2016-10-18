package playground.sebhoerl.avtaxi.config;

import org.matsim.core.config.ReflectiveConfigGroup;

public class AVGeneratorConfig extends ReflectiveConfigGroup {
    final static String GENERATOR = "generator";
    final private String strategyName;

    public AVGeneratorConfig(String strategyName) {
        super(GENERATOR);
        this.strategyName = strategyName;
    }

    public String getStrategyName() {
        return strategyName;
    }
}
