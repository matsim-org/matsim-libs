package org.matsim.contrib.ev.strategic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlanActivity;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.strategic.plan.ChargingPlansConverter;

public class ChargingPlansConverterTest {
    @Test
    public void testCharingPlansConverter() {
        ChargingPlans chargingPlans = new ChargingPlans();

        ChargingPlan chargingPlan = new ChargingPlan();
        chargingPlans.addChargingPlan(chargingPlan);

        ChargingPlanActivity activity = new ChargingPlanActivity(5, 6, Id.create("charger", Charger.class));
        chargingPlan.addChargingActivity(activity);

        ChargingPlansConverter converter = new ChargingPlansConverter();
        String representation = converter.convertToString(chargingPlans);

        ChargingPlans restored = converter.convert(representation);
        assertEquals(5, restored.getChargingPlans().get(0).getChargingActivities().get(0).getStartActivityIndex());
        assertEquals(6, restored.getChargingPlans().get(0).getChargingActivities().get(0).getEndActivityIndex());
        assertEquals("charger",
                restored.getChargingPlans().get(0).getChargingActivities().get(0).getChargerId().toString());

    }
}
