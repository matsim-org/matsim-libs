package org.matsim.codeexamples.router.travelDisutility;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;

import static org.junit.jupiter.api.Assertions.*;

public class RunTravelDisutilityExampleTest {
	private static final Logger log = LogManager.getLogger(RunTravelDisutilityExampleTest.class) ;

	@Test
	void main() {
		try {
			RunTravelDisutilityExample.main(null);
		} catch ( Exception ee ) {
			log.fatal(ee) ;
			fail() ;
		}
	}
}