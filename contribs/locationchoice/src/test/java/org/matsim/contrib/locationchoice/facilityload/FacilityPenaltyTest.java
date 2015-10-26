package org.matsim.contrib.locationchoice.facilityload;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.utils.misc.MatsimTestUtils;

public class FacilityPenaltyTest {

	@Test
	public void testGetPenalty() {
		FacilityPenalty facilitypenalty = new FacilityPenalty(0.0, 1, new DestinationChoiceConfigGroup());
		Assert.assertEquals(facilitypenalty.getCapacityPenaltyFactor(0.0, 1.0), 0.0, MatsimTestUtils.EPSILON);
	}

	@Test
	public void testcalculateCapPenaltyFactor() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
	IllegalAccessException, InvocationTargetException {

		FacilityPenalty facilitypenalty = new FacilityPenalty(0.0, 1, new DestinationChoiceConfigGroup());

	    Method method = null;
		method = facilitypenalty.getClass().getDeclaredMethod("calculateCapPenaltyFactor", new Class[]{int.class, int.class});
		method.setAccessible(true);
		Double val = (Double)method.invoke(facilitypenalty, new Object[]{0, 1});
		Assert.assertTrue(Math.abs(val.doubleValue()) < 0.000000001);
	}
}