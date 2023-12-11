package org.matsim.contrib.locationchoice.frozenepsilons;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

public class FacilityPenaltyTest {

	@Test
	void testGetPenalty() {
		FacilityPenalty facilitypenalty = new FacilityPenalty(0.0, new DestinationChoiceConfigGroup());
		Assertions.assertEquals(facilitypenalty.getCapacityPenaltyFactor(0.0, 1.0), 0.0, MatsimTestUtils.EPSILON);
	}

	@Test
	void testcalculateCapPenaltyFactor() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {

		FacilityPenalty facilitypenalty = new FacilityPenalty(0.0, new DestinationChoiceConfigGroup());

	    Method method = null;
		method = facilitypenalty.getClass().getDeclaredMethod("calculateCapPenaltyFactor", new Class[]{int.class, int.class});
		method.setAccessible(true);
		Double val = (Double)method.invoke(facilitypenalty, new Object[]{0, 1});
		Assertions.assertTrue(Math.abs(val.doubleValue()) < 0.000000001);
	}
}
