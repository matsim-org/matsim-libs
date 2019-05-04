package electric.edrt.energyconsumption;

import org.apache.commons.math.ArgumentOutsideDomainException;
import org.apache.commons.math.analysis.interpolation.LinearInterpolator;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.temperature.TemperatureService;

import javax.inject.Inject;
import javax.inject.Singleton;


/**
 * @author Joschka Bischoff
 * This class contains confidential values and should not be used outside the VW projects.
 * There's intentionally no GPL header.
 */
public class VwAVAuxEnergyConsumptionWithTemperatures implements AuxEnergyConsumption {

    @Singleton
    public static class VwAuxFactory implements VwAVAuxEnergyConsumptionWithTemperatures.Factory {

        @Inject
        TemperatureService temperatureService;
        @Inject
        VehicleAtChargerLinkTracker tracker;
        @Override
        public AuxEnergyConsumption create(ElectricVehicle electricVehicle) {
            return new VwAVAuxEnergyConsumptionWithTemperatures(temperatureService, electricVehicle, tracker);
        }
    }


    private LinearInterpolator linearInterpolator = new LinearInterpolator();
    private double[] x = {-15, -10, -5, 0, 5, 10, 15, 20, 25, 30, 35, 40};
    private double[] y = {2908, 2079, 1428, 1105, 773, 440, 214, 103, 205, 331, 498, 911};
    private final TemperatureService temperatureService;
    private final ElectricVehicle ev;
    private final VehicleAtChargerLinkTracker tracker;


    //Verbrauch Bordnetz konstant 1,5KW -> 1,5kWh/h -> 0,025kWh/min
    private static double auxConsumption_per_s = 1500;

    //Verbrauch Systeme automatische Fahren konstant 1,5KW -> 1,5kWh/h --> 1500Ws/s
    private static double AVauxConsumption_per_s = 1500;


    VwAVAuxEnergyConsumptionWithTemperatures(TemperatureService temperatureService, ElectricVehicle ev, VehicleAtChargerLinkTracker tracker) {
        this.temperatureService = temperatureService;
        this.ev = ev;
        this.tracker = tracker;
    }

    @Override
    public double calcEnergyConsumption(double period, double timeOfDay) {
        if (tracker.isAtCharger(ev)) return 0;
        double temp = temperatureService.getCurrentTemperature();
        double consumptionTemp;
        try {
            consumptionTemp = linearInterpolator.interpolate(x, y).value(temp);
        } catch (ArgumentOutsideDomainException e) {
            throw new IllegalArgumentException("Reported temperature " + temp + " is out of bound.");

        }
        return period * (AVauxConsumption_per_s + auxConsumption_per_s + consumptionTemp);
    }

}
