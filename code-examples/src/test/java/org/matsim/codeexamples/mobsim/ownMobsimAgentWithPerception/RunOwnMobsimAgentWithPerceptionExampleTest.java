package org.matsim.codeexamples.mobsim.ownMobsimAgentWithPerception;

import org.junit.Test;

import static org.junit.Assert.*;

public class RunOwnMobsimAgentWithPerceptionExampleTest {
	@Test
	public void main() {
		try {
			RunOwnMobsimAgentWithPerceptionExample.main(new String[]{});
		} catch (Exception ee ) {
			ee.printStackTrace();
			fail("something went wrong") ;
		}
	}
	
}
