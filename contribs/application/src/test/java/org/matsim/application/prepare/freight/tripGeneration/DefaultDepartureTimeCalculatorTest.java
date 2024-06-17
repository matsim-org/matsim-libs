package org.matsim.application.prepare.freight.tripGeneration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultDepartureTimeCalculatorTest {

	@Test
	void testGetDepartureTime() {
		DefaultDepartureTimeCalculator calculator = new DefaultDepartureTimeCalculator();
		// Just check if values are in range of 0-86.400
		for(int i = 0; i < 50; i++){
			Assertions.assertEquals(43200, calculator.getDepartureTime(), 43200);
		}
	}
}
