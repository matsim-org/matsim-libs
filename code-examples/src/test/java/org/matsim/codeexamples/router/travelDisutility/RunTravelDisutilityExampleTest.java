package org.matsim.codeexamples.router.travelDisutility;

import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.*;

public class RunTravelDisutilityExampleTest {
	private static final Logger log = Logger.getLogger(RunTravelDisutilityExampleTest.class) ;

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