package playground.sebhoerl.avtaxi.generator;

import java.util.Iterator;

import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;

public interface AVGenerator extends Iterator<AVVehicle> {
    interface AVGeneratorFactory {
        AVGenerator createGenerator(AVGeneratorConfig generatorConfig);
    }
}
