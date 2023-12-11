package org.matsim.codeexamples.fixedTimeSignals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests otfvis for signals with/ without lanes.
 * 
 * Additionally, it supports a main method to start the otfvis with signals and lanes example out of Eclipse, 
 * where it is not possible to execute main methods using MATSim from contribs (except in tests).
 * 
 */
public class RunVisualizeSignalScenarioWithLanesGUITest {
	
	public static void main(String[] args) {
		boolean startOtfvis = true;
		if (args != null && args.length != 0 && args[0].equals("false")){
			startOtfvis = false;
		}
		
		try {
			VisualizeSignalScenarioWithLanes.run(startOtfvis);
		} catch (Exception ee ) {
			ee.printStackTrace();
			Assertions.fail("something went wrong") ;
		}
	}

	@Test
	void testSignalExampleVisualizationWithLanes(){
		String[] startOtfvis = {"false"};
		main(startOtfvis);
	}

	@Test
	void testSignalExampleVisualizationWoLanes(){
		try {
			VisualizeSignalScenario.run(false);
		} catch (Exception ee) {
			ee.printStackTrace();
			Assertions.fail("something went wrong");
		}
	}

}
