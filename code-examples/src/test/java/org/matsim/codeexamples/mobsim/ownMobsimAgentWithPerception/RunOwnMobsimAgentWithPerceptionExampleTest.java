package org.matsim.codeexamples.mobsim.ownMobsimAgentWithPerception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RunOwnMobsimAgentWithPerceptionExampleTest {
	@Test
	void main() {
		try {
			RunOwnMobsimAgentWithPerceptionExample.main(new String[]{});
		} catch (Exception ee ) {
			ee.printStackTrace();
			fail("something went wrong") ;
		}
	}
	
}
