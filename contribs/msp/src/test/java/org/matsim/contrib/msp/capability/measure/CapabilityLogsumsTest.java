package org.matsim.contrib.msp.capability.measure;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.contrib.msp.capability.measure.CapabilityLogsums.Alternative;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the headline numbers: more choice has to be better, a further brand has to beat a further branch
 * of a brand the person can already reach, and the scales have to mean what the config says they mean.
 */
public class CapabilityLogsumsTest {

	private final CapabilityConfigGroup config = new CapabilityConfigGroup();

	@Test
	void nothingReachableIsNotANumber() {
		assertTrue(Double.isNaN(CapabilityLogsums.nestedUtilityLogsum(List.of(), this.config)));
		assertTrue(Double.isNaN(CapabilityLogsums.costLogsum(new double[0], this.config)));
	}

	@Test
	void aSingleAlternativeIsWorthExactlyItself() {
		// nothing is inflated by being alone, on either level of the nesting
		assertEquals(4.0, CapabilityLogsums.nestedUtilityLogsum(List.of(new Alternative("Edeka", 4.0)),
			this.config), 1e-12);
		assertEquals(4.0, CapabilityLogsums.logsum(new double[]{4.0}, 8.0), 1e-12);
	}

	@Test
	void nIdenticalAlternativesAreWorthLnNOverBeta() {
		// the rule the betas are picked with
		assertEquals(4.0 + Math.log(2) / 2, CapabilityLogsums.logsum(new double[]{4.0, 4.0}, 2.0), 1e-12);
		assertEquals(4.0 + Math.log(5) / 8, CapabilityLogsums.logsum(new double[]{4, 4, 4, 4, 4}, 8.0), 1e-12);
	}

	@Test
	void aLargeBetaApproachesTheMaximum() {
		double[] values = {4.0, 3.0, 1.0};
		assertEquals(4.0, CapabilityLogsums.logsum(values, 1000), 1e-6);
		assertTrue(CapabilityLogsums.logsum(values, 1) > 4.0, "a small scale rewards having alternatives");
	}

	@Test
	void aFurtherFacilityAlwaysAdds() {
		double one = CapabilityLogsums.nestedUtilityLogsum(List.of(new Alternative("Edeka", 4.0)), this.config);
		double two = CapabilityLogsums.nestedUtilityLogsum(
			List.of(new Alternative("Edeka", 4.0), new Alternative("Edeka", 4.0)), this.config);
		assertTrue(two > one);
	}

	@Test
	void aFurtherBrandBeatsAFurtherBranchOfTheSameBrand() {

		List<Alternative> sameBrand = List.of(new Alternative("Edeka", 4.0), new Alternative("Edeka", 4.0));
		List<Alternative> twoBrands = List.of(new Alternative("Edeka", 4.0), new Alternative("Rewe", 4.0));

		// identical utilities: only the nesting tells the two choice sets apart
		assertEquals(4.0 + Math.log(2) / this.config.getBetaWithinSubType(),
			CapabilityLogsums.nestedUtilityLogsum(sameBrand, this.config), 1e-12);
		assertEquals(4.0 + Math.log(2) / this.config.getBetaAcrossSubType(),
			CapabilityLogsums.nestedUtilityLogsum(twoBrands, this.config), 1e-12);

		assertTrue(CapabilityLogsums.nestedUtilityLogsum(twoBrands, this.config)
				> CapabilityLogsums.nestedUtilityLogsum(sameBrand, this.config),
			"variety across brands has to count for more than a second branch of one brand");
	}

	@Test
	void facilitiesWithoutASubTypeShareOneNest() {

		List<Alternative> unknown = List.of(new Alternative("", 4.0), new Alternative(null, 4.0));

		// missing data must not look like variety, so these count as one kind of place, not two
		assertEquals(4.0 + Math.log(2) / this.config.getBetaWithinSubType(),
			CapabilityLogsums.nestedUtilityLogsum(unknown, this.config), 1e-12);
	}

	@Test
	void theWithinScaleHasToBeTheLargerOne() {
		// the condition for the nested logit to be consistent with random utility maximisation
		assertTrue(this.config.getBetaWithinSubType() > this.config.getBetaAcrossSubType());
		assertTrue(this.config.getBetaAcrossSubType() / this.config.getBetaWithinSubType() <= 1);
	}

	@Test
	void aCheaperFacilityIsWorthMore() {
		double near = CapabilityLogsums.costLogsum(new double[]{0.5}, this.config);
		double far = CapabilityLogsums.costLogsum(new double[]{3.0}, this.config);
		assertTrue(near > far);
		assertTrue(CapabilityLogsums.costLogsum(new double[]{0.5, 3.0}, this.config) > near,
			"a further reachable facility always adds");
	}

	@Test
	void aScaleOfZeroIsRejected() {
		// the degenerate case: the count of alternatives takes over completely and the logsum diverges
		assertThrows(IllegalArgumentException.class, () -> CapabilityLogsums.logsum(new double[]{1.0}, 0));
		assertThrows(IllegalArgumentException.class, () -> CapabilityLogsums.logsum(new double[]{1.0}, -1));
	}
}
