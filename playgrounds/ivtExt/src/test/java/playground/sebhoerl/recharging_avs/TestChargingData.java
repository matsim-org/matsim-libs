package playground.sebhoerl.recharging_avs;

import org.junit.Test;
import playground.sebhoerl.recharging_avs.calculators.BinnedChargeCalculatorData;
import playground.sebhoerl.recharging_avs.calculators.BinnedChargeDataReader;
import playground.sebhoerl.recharging_avs.calculators.FixedBinSizeData;
import playground.sebhoerl.recharging_avs.calculators.VariableBinSizeData;

import org.junit.Assert;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class TestChargingData {
    @Test
    public void testFixedData() {
        VariableBinSizeData variableData = new VariableBinSizeData();

        BinnedChargeDataReader reader = new BinnedChargeDataReader(variableData);
        reader.readFile("src/test/resources/sebhoerl/recharging_avs/fixed.csv");

        Assert.assertTrue(variableData.hasFixedIntervals());

        FixedBinSizeData fixedData = FixedBinSizeData.createFromVariableData(variableData);

        Assert.assertEquals((int) fixedData.getBinStartTime(0), 50);
        Assert.assertEquals((int) fixedData.getBinStartTime(1), 100);
        Assert.assertEquals((int) fixedData.getBinStartTime(2), 150);
        Assert.assertEquals((int) fixedData.getBinStartTime(3), 200);

        Assert.assertEquals((int) fixedData.getBinEndTime(0), 100);
        Assert.assertEquals((int) fixedData.getBinEndTime(1), 150);
        Assert.assertEquals((int) fixedData.getBinEndTime(2), 200);
        Assert.assertEquals((int) fixedData.getBinEndTime(3), 200);

        Assert.assertEquals((int) fixedData.getBinDuration(0), 50);
        Assert.assertEquals((int) fixedData.getBinDuration(1), 50);
        Assert.assertEquals((int) fixedData.getBinDuration(2), 50);
        Assert.assertEquals((int) fixedData.getBinDuration(3), 50);

        testDataSet(fixedData);
    }

    @Test
    public void testVariableData() {
        VariableBinSizeData data = new VariableBinSizeData();

        BinnedChargeDataReader reader = new BinnedChargeDataReader(data);
        reader.readFile("src/test/resources/sebhoerl/recharging_avs/variable.csv");

        Assert.assertFalse(data.hasFixedIntervals());

        Assert.assertEquals((int) data.getBinStartTime(0), 50);
        Assert.assertEquals((int) data.getBinStartTime(1), 90);
        Assert.assertEquals((int) data.getBinStartTime(2), 150);
        Assert.assertEquals((int) data.getBinStartTime(3), 240);

        Assert.assertEquals((int) data.getBinEndTime(0), 90);
        Assert.assertEquals((int) data.getBinEndTime(1), 150);
        Assert.assertEquals((int) data.getBinEndTime(2), 240);
        Assert.assertEquals((int) data.getBinEndTime(3), 240);

        Assert.assertEquals((int) data.getBinDuration(0), 40);
        Assert.assertEquals((int) data.getBinDuration(1), 60);
        Assert.assertEquals((int) data.getBinDuration(2), 90);
        Assert.assertEquals((int) data.getBinDuration(3), 0);

        testDataSet(data);
    }

    private void testDataSet(BinnedChargeCalculatorData data) {
        // Test data

        Assert.assertEquals(data.getDischargeRateByDistance(0), 0.1, 1e-6);
        Assert.assertEquals(data.getDischargeRateByDistance(1), 0.2, 1e-6);
        Assert.assertEquals(data.getDischargeRateByDistance(2), 0.3, 1e-6);
        Assert.assertEquals(data.getDischargeRateByDistance(3), 0.4, 1e-6);

        Assert.assertEquals(data.getDischargeRateByTime(0), 0.01, 1e-6);
        Assert.assertEquals(data.getDischargeRateByTime(1), 0.02, 1e-6);
        Assert.assertEquals(data.getDischargeRateByTime(2), 0.03, 1e-6);
        Assert.assertEquals(data.getDischargeRateByTime(3), 0.04, 1e-6);

        Assert.assertEquals(data.getRechgargeRate(0), 0.11, 1e-6);
        Assert.assertEquals(data.getRechgargeRate(1), 0.12, 1e-6);
        Assert.assertEquals(data.getRechgargeRate(2), 0.13, 1e-6);
        Assert.assertEquals(data.getRechgargeRate(3), 0.14, 1e-6);

        Assert.assertEquals(data.getMaximumCharge(0), 10, 1e-6);
        Assert.assertEquals(data.getMaximumCharge(1), 20, 1e-6);
        Assert.assertEquals(data.getMaximumCharge(2), 30, 1e-6);
        Assert.assertEquals(data.getMaximumCharge(3), 40, 1e-6);

        Assert.assertEquals(data.getMinimumCharge(0), 1, 1e-6);
        Assert.assertEquals(data.getMinimumCharge(1), 2, 1e-6);
        Assert.assertEquals(data.getMinimumCharge(2), 3, 1e-6);
        Assert.assertEquals(data.getMinimumCharge(3), 4, 1e-6);

        // Test bin calculation

        Assert.assertEquals(0, data.calculateBin(5.0));
        Assert.assertEquals(0, data.calculateBin(80.0));
        Assert.assertEquals(1, data.calculateBin(120.0));
        Assert.assertEquals(3, data.calculateBin(250.0));
        Assert.assertEquals(3, data.calculateBin(400.0));
    }
}
