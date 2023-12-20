package org.matsim.codeexamples.run;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class RunMatsimTest{
	@RegisterExtension public MatsimTestUtils utils = new MatsimTestUtils() ;

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
			Assertions.fail();
		}

	}
}
