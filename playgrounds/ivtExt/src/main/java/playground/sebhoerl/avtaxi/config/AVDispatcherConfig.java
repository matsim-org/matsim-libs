package playground.sebhoerl.avtaxi.config;


import org.matsim.core.config.ReflectiveConfigGroup;

public class AVDispatcherConfig extends ReflectiveConfigGroup {
    final static String DISPATCHER = "dispatcher";
    final private String strategyName;

    public AVDispatcherConfig(String strategyName) {
        super(DISPATCHER);
        this.strategyName = strategyName;
    }

    public String getStrategyName() {
        return strategyName;
    }
}
