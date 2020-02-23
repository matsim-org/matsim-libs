package playground.vsp.airPollution.flatEmissions;

import org.junit.Test;

import static junit.framework.TestCase.fail;
import static playground.vsp.airPollution.flatEmissions.EmissionCostFactors.NOx;

public class EmissionCostFactorsTest{

    @Test
	public void test() {

		System.out.println( "name=" + NOx.name() + "; factor=" + NOx.getCostFactor() );

		System.out.println( "noxFactor=" + EmissionCostFactors.getCostFactor( "NOx" ) ) ;
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_unknownParameter() {

		EmissionCostFactors.getCostFactor("does-not-exist");
		fail("Unknown pollutant should cause exception");
	}

}
