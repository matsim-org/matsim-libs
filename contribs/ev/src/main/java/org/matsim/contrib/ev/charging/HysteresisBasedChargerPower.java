package org.matsim.contrib.ev.charging;

import java.util.Collection;

import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.ChargingLogic.ChargingVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

public class HysteresisBasedChargerPower implements ChargerPower {

    @Override
    public void plugVehicle(double now, ElectricVehicle vehicle) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'plugVehicle'");
    }

    @Override
    public void unplugVehicle(double now, ElectricVehicle vehicle) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unplugVehicle'");
    }

    @Override
    public double calcAvailableEnergyToCharge(double now, ElectricVehicle vehicle) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calcAvailableEnergyToCharge'");
    }

    @Override
    public double calcChargingPower(double now, ElectricVehicle vehicle) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calcChargingPower'");
    }

    @Override
    public void consumeEnergy(double energy) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'consumeEnergy'");
    }

    @Override
    public void update(double now) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    /*static public record Settings( //
            double batteryChargingPower_kW, //
            double maximumGridPower_kW, //

            double highOutputPower_kW, //
            double lowOutputPower_kW, //

            double dischargingThreshold_kWh, //
            double chargingTheshold_kWh, //

            double batteryCapacity_kWh, //
            double initialSoc) {
    }

    private final Settings settings;

    private enum LogicState {
        HIGH_POWER, LOW_POWER
    }

    private LogicState logicState;

    private final double capacity_Ws;
    private double chargingState_Ws;

    public HysteresisBasedChargerPower(Settings settings) {
        this.settings = settings;

        this.capacity_Ws = EvUnits.kWh_to_J(settings.batteryCapacity_kWh);
        this.chargingState_Ws = capacity_Ws * settings.initialSoc;
        this.logicState = chargingState_Ws < settings.dischargingThreshold_kWh ? LogicState.LOW_POWER
                : LogicState.HIGH_POWER;
    }

    @Override
    public double calcMaximumChargingPower(double requestedEnergy_Ws) {
        if (requestedEnergy_Ws > batteryState_Ws) {
            // one part with battery charging, one part with network charging gives an
            // average
            double batteryShare = batteryState_Ws / requestedEnergy_Ws;
        }
    }

    @Override
    public double getMaximumEnergy() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public void consumeEnergy(double energy) {
        // possible that more energy got drawn than battery
        chargingState_Ws = Math.max(0.0, chargingState_Ws - energy);
    }

    private double previousChargingState_Ws = Double.NaN;

    @Override
    public void update(double chargePeriod, double now, Collection<ChargingVehicle> vehicles) {
        if (Double.isFinite(previousChargingState_Ws)) {
            // here we simulate the hysteresis logic

            if (previousChargingState_Ws >= settings.dischargingThreshold_kWh
                    && chargingState_Ws < settings.dischargingThreshold_kWh) {
                // we go into low power charging mode
                logicState = LogicState.LOW_POWER;
            }

            if (previousChargingState_Ws <= settings.chargingTheshold_kWh
                    && chargingState_Ws > settings.chargingTheshold_kWh) {
                // we go into high power charging mode
                logicState = LogicState.HIGH_POWER;
            }
        }

        previousChargingState_Ws = chargingState_Ws;
    }*/
}
