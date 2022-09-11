package org.matsim.codeexamples.router.travelDisutility;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.Test;

import static org.junit.Assert.*;

public class RunTravelDisutilityExampleTest {
	private static final Logger log = LogManager.getLogger(RunTravelDisutilityExampleTest.class) ;

	@Test
	public void main() {
		try {
			RunTravelDisutilityExample.main(null);
		} catch ( Exception ee ) {
			log.fatal(ee) ;
			fail() ;
		}
	}
}