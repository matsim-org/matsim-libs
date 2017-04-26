package playground.sebhoerl.avtaxi.config;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ReflectiveConfigGroup;
import playground.sebhoerl.avtaxi.data.AVOperator;

public class AVOperatorConfig extends ReflectiveConfigGroup {
    final static String OPERATOR = "operator";

    final private AVConfig parentConfig;
    final private Id<AVOperator> id;

    private AVTimingParameters timingParameters = null;
    private AVPriceStructureConfig priceStructureConfig = null;
    private AVDispatcherConfig dispatcherConfig = null;
    private AVGeneratorConfig generatorConfig = null;

    public AVOperatorConfig(String id, AVConfig parentConfig) {
        super(OPERATOR);
        this.parentConfig = parentConfig;
        this.id = Id.create(id, AVOperator.class);
    }

    public Id<AVOperator> getId() {
        return id;
    }

    public AVTimingParameters getTimingParameters() {
        if (timingParameters == null) {
            return parentConfig.getTimingParameters();
        }

        return timingParameters;
    }

    public AVTimingParameters createTimingParameters() {
        timingParameters = new AVTimingParameters();
        return timingParameters;
    }

    public AVDispatcherConfig getDispatcherConfig() {
        return dispatcherConfig;
    }

    public AVDispatcherConfig createDispatcherConfig(String name) {
        dispatcherConfig = new AVDispatcherConfig(this, name);
        return dispatcherConfig;
    }

    public AVGeneratorConfig getGeneratorConfig() {
        return generatorConfig;
    }

    public AVGeneratorConfig createGeneratorConfig(String name) {
        generatorConfig = new AVGeneratorConfig(this, name);
        return generatorConfig;
    }

    public AVPriceStructureConfig getPriceStructureConfig() {
        return priceStructureConfig;
    }

    public AVPriceStructureConfig createPriceStructureConfig() {
        priceStructureConfig = new AVPriceStructureConfig();
        return priceStructureConfig;
    }
}
