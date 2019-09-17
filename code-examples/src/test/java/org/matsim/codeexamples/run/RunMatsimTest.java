package org.matsim.codeexamples.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.*;

public class RunMatsimTest{
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void main(){

		String[] args = {"scenarios/equil/config.xml"
			  , "--config:controler.outputDirectory=" + utils.getOutputDirectory()
			  , "--config:controler.lastIteration=2"
		} ;

		try{
			RunMatsim.main( args );
		} catch ( Exception ee ) {
			ee.printStackTrace();
			Assert.fail();
		}

	}
}
