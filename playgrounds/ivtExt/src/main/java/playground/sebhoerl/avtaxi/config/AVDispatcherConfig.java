package playground.sebhoerl.avtaxi.config;


import org.matsim.core.config.ReflectiveConfigGroup;

public class AVDispatcherConfig extends ReflectiveConfigGroup {
    final static String DISPATCHER = "dispatcher";
    final private String strategyName;
    final private AVOperatorConfig parent;

    public AVDispatcherConfig(AVOperatorConfig parent, String strategyName) {
        super(DISPATCHER, true);
        this.strategyName = strategyName;
        this.parent = parent;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public AVOperatorConfig getParent() {
        return parent;
    }
}
