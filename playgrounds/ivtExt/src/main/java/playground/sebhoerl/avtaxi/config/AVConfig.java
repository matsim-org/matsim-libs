package playground.sebhoerl.avtaxi.config;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.HashSet;
import java.util.Set;

public class AVConfig extends ReflectiveConfigGroup {
    final static String AV = "av";

    final private Set<AVOperatorConfig> operators = new HashSet<>();
    final private AVTimingParameters timingParameters = AVTimingParameters.createDefault();

    final static String NUMBER_OF_VEHICLES = "numberOfVehicles";
    private long numberOfVehicles = 10;

    public AVConfig() {
        super(AV);
    }

    public Set<AVOperatorConfig> getOperatorConfigs() {
        return operators;
    }

    public AVTimingParameters getTimingParameters() {
        return timingParameters;
    }

    @StringSetter(NUMBER_OF_VEHICLES)
    public void setNumberOfVehicles(long numberOfVehicles) {
        this.numberOfVehicles = numberOfVehicles;
    }

    @StringGetter(NUMBER_OF_VEHICLES)
    public long getNumberOfVehicles() {
        return numberOfVehicles;
    }
}
