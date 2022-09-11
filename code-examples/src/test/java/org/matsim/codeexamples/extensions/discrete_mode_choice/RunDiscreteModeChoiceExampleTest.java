package org.matsim.codeexamples.extensions.discrete_mode_choice;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class RunDiscreteModeChoiceExampleTest{
	private static final Logger log = LogManager.getLogger( RunDiscreteModeChoiceExampleTest.class );
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
//	@Ignore // this works with with 14.x since approx jun.  kai, jun'21
	public void main(){
		RunDiscreteModeChoiceExample.main(null);
	}
}
