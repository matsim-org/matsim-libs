package playground.sebhoerl.recharging_avs.calculators;

import org.matsim.core.config.ReflectiveConfigGroup;

public class BinnedChargeCalculatorConfig extends ReflectiveConfigGroup {
    static final String BINNED_CHARGE_CALCULATOR = "binned_charge_calculator";
    static final String INPUT_PATH = "inputPath";

    private String inputPath = null;

    public BinnedChargeCalculatorConfig() {
        super(BINNED_CHARGE_CALCULATOR);
    }

    @StringGetter(INPUT_PATH)
    public String getInputPath() {
        return inputPath;
    }

    @StringSetter(INPUT_PATH)
    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }
}
