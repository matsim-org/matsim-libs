package playground.sebhoerl.recharging_avs.calculators;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import playground.sebhoerl.recharging_avs.RechargeUtils;

public class BinnedChargeCalculatorModule extends AbstractModule {
    @Override
    public void install() {
        bind(BinnedChargeCalculator.class);
        RechargeUtils.registerChargeCalculator(binder(), "binned", BinnedChargeCalculator.class);
    }

    @Provides
    @Singleton
    private BinnedChargeCalculatorData provideBinnedChargeCalculatorData(Config config, BinnedChargeCalculatorConfig chargeConfig) {
        VariableBinSizeData data = new VariableBinSizeData();

        if (chargeConfig.getInputPath() == null) {
            return new VariableBinSizeData();
        }

        BinnedChargeDataReader reader = new BinnedChargeDataReader(data);
        reader.readFile(ConfigGroup.getInputFileURL(config.getContext(), chargeConfig.getInputPath()).getPath());

        return data.hasFixedIntervals() ? FixedBinSizeData.createFromVariableData(data) : data;
    }
}
