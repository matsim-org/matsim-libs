package org.matsim.contrib.msp.capability.measure;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.msp.capability.DiscretionaryActivityParams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the function that turns flexible time into a utility bonus.
 */
public class FlexibilityFunctionTest {

	@Test
	void noFlexibleTimeMeansNoBonus() {
		// not minus infinity: an activity that just fits is worth the base value
		assertEquals(0, FlexibilityFunction.LOG.apply(0, 1.0, 3600));
	}

	@Test
	void growsWithDiminishingReturns() {

		double oneHour = FlexibilityFunction.LOG.apply(3600, 1.0, 3600);
		double twoHours = FlexibilityFunction.LOG.apply(2 * 3600, 1.0, 3600);
		double threeHours = FlexibilityFunction.LOG.apply(3 * 3600, 1.0, 3600);
		double fourHours = FlexibilityFunction.LOG.apply(4 * 3600, 1.0, 3600);

		assertEquals(Math.log(2), oneHour, 1e-9, "one reference duration gives k * ln(2)");
		assertTrue(twoHours > oneHour);
		assertTrue(fourHours > threeHours, "more flexible time is always worth more");

		// each further hour is worth less than the one before it - compared over equal spans, since the
		// increment over a wider span can of course be larger
		assertTrue(twoHours - oneHour < oneHour - 0, "the second hour is worth less than the first");
		assertTrue(threeHours - twoHours < twoHours - oneHour);
		assertTrue(fourHours - threeHours < threeHours - twoHours);
	}

	@Test
	void staysBelowTheBaseUtilityEvenForAWholeFreeDay() {

		DiscretionaryActivityParams params = new DiscretionaryActivityParams("shop");

		// someone free all day, at a shop open 07:00-21:00: about 13.5 h of flexible time
		double bonus = FlexibilityFunction.LOG.apply(13.5 * 3600, params.getFlexibilityUtility(),
			params.getReferenceDuration());

		assertTrue(bonus < params.getBaseUtility(),
			"being able to perform the activity at all has to outweigh being free about when: "
				+ bonus + " vs " + params.getBaseUtility());
	}

	@Test
	void theOtherFunctionsAreNotImplementedYet() {
		assertThrows(UnsupportedOperationException.class, () -> FlexibilityFunction.LINEAR.apply(3600, 1, 3600));
		assertThrows(UnsupportedOperationException.class, () -> FlexibilityFunction.DAY_PARTS.apply(3600, 1, 3600));
	}

	@Test
	void rejectsNegativeFlexibleTime() {
		assertThrows(IllegalArgumentException.class, () -> FlexibilityFunction.LOG.apply(-1, 1, 3600));
	}
}
