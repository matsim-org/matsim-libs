package playground.wrashid.artemis.smartCharging;

import java.util.LinkedList;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class ChargingTimeTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void basicTests() {
		LinkedList<ChargingTime> bins = ChargingTime.get15MinChargingBins(100.0, 1810);

		bins.get(0).getStartChargingTime();

		Assert.assertEquals(100, bins.get(0).getStartChargingTime(), 0.1);
		Assert.assertEquals(900, bins.get(0).getEndChargingTime(), 0.1);
		Assert.assertEquals(900, bins.get(1).getStartChargingTime(), 0.1);
		Assert.assertEquals(1800, bins.get(1).getEndChargingTime(), 0.1);
		Assert.assertEquals(1800, bins.get(2).getStartChargingTime(), 0.1);
		Assert.assertEquals(1810, bins.get(2).getEndChargingTime(), 0.1);		
		
	}

}
