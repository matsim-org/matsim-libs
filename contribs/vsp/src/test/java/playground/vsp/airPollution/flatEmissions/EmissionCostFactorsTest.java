package playground.vsp.airPollution.flatEmissions;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static playground.vsp.airPollution.flatEmissions.EmissionCostFactors.NOx;

import org.junit.jupiter.api.Test;

public class EmissionCostFactorsTest{

	@Test
	void test() {

		System.out.println( "name=" + NOx.name() + "; factor=" + NOx.getCostFactor() );

		System.out.println( "noxFactor=" + EmissionCostFactors.getCostFactor( "NOx" ) ) ;
	}

	@Test
	void test_unknownParameter() {
		assertThrows(IllegalArgumentException.class, () -> {

			EmissionCostFactors.getCostFactor("does-not-exist");
			fail("Unknown pollutant should cause exception");
		});
	}

}
