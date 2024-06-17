package org.matsim.application.prepare.freight.tripGeneration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultNumberOfTripsCalculatorTest {

	@Test
	void testCcalculateNumberOfTripsV2() {
		DefaultNumberOfTripsCalculator calculator0;
		DefaultNumberOfTripsCalculator calculator1;
		try{
			calculator0 = new DefaultNumberOfTripsCalculator(35, 250, 1);
			calculator1 = new DefaultNumberOfTripsCalculator(12, 150, 0.25);
		} catch (Exception e){
			Assertions.fail(e); // If this Assertion fails, there is an external problem in another class
			return;
		}

		/* Test Sample 0:
		Average Load = 35t
		working days = 250
		sample = 1
		 */

		//Check for FTL
		Assertions.assertEquals(556, calculator0.calculateNumberOfTripsV2(3750000, "10"));
		Assertions.assertEquals(556, calculator0.calculateNumberOfTripsV2(3750000, "21"));
		//...
		Assertions.assertEquals(556, calculator0.calculateNumberOfTripsV2(3750000, "190"));
		Assertions.assertEquals(556, calculator0.calculateNumberOfTripsV2(3750000, "200"));

		//Check for LTL
		Assertions.assertEquals(428, calculator0.calculateNumberOfTripsV2(3750000, "40"));
		//...
		Assertions.assertEquals(428, calculator0.calculateNumberOfTripsV2(3750000, "180"));

		//Check for different tPy
		Assertions.assertEquals(18, calculator0.calculateNumberOfTripsV2(120000, "10"));
		Assertions.assertEquals(14, calculator0.calculateNumberOfTripsV2(120000, "40"));


		/* Test Sample 1:
		Average Load = 12t
		working days = 200
		sample = 0.25
		 */

		//Check for FTL
		System.out.println(calculator1.calculateNumberOfTripsV2(120000, "40"));
		Assertions.assertEquals(232, calculator1.calculateNumberOfTripsV2(3750000, "10"));
		Assertions.assertEquals(232, calculator1.calculateNumberOfTripsV2(3750000, "200"));

		//Check for LTL
		Assertions.assertEquals(521, calculator1.calculateNumberOfTripsV2(3750000, "40"));
		Assertions.assertEquals(521, calculator1.calculateNumberOfTripsV2(3750000, "180"));

		//Check for different tPy
		Assertions.assertEquals(7, calculator1.calculateNumberOfTripsV2(120000, "10"));
		Assertions.assertEquals(17, calculator1.calculateNumberOfTripsV2(120000, "40"));


	}
}
