package org.matsim.contrib.minibus.scoring;

import org.junit.Assert;
import org.junit.Test;

public class OperatorCostCollectorHandlerTest {

    @Test
    public void getVehicleIdToOperatorCostContainerMap() {
        OperatorCostCollectorHandler handler = new OperatorCostCollectorHandler("test", 1.0, 0.1, 0.01);
        Assert.assertNotNull("Map should not be null.", handler.getVehicleIdToOperatorCostContainerMap());
        Assert.assertEquals("Map should be empty.", 0, handler.getVehicleIdToOperatorCostContainerMap().size());
    }
}