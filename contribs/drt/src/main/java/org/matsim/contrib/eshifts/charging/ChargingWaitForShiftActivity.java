package org.matsim.contrib.eshifts.charging;

import org.matsim.contrib.evrp.ChargingActivity;
import org.matsim.contrib.evrp.ChargingTask;

/**
 * @author nkuehnel
 */
public class ChargingWaitForShiftActivity extends ChargingActivity {

    public ChargingWaitForShiftActivity(ChargingTask chargingTask) {
        super(chargingTask);
    }

    @Override
    public String getActivityType() {
        return "Charging wait for shift";
    }

}
