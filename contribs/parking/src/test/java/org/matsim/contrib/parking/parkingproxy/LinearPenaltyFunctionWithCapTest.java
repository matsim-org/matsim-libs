package org.matsim.contrib.parking.parkingproxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinearPenaltyFunctionWithCapTest {
	@Test
	void calculatePenalty() {
		LinearPenaltyFunctionWithCap linearPenaltyFunctionWithCap = new LinearPenaltyFunctionWithCap(10, 360);
		assertEquals(0, linearPenaltyFunctionWithCap.calculatePenalty(-1));
		assertEquals(0, linearPenaltyFunctionWithCap.calculatePenalty(0));
		assertEquals(10, linearPenaltyFunctionWithCap.calculatePenalty(1));
		assertEquals(360, linearPenaltyFunctionWithCap.calculatePenalty(36));
		assertEquals(360, linearPenaltyFunctionWithCap.calculatePenalty(37));
	}
}
