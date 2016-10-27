package tutorial.fixedTimeSignals;

import org.junit.Assert;

public class RunVisualizeSignalScenarioWithLanesGUI {

	public static void main(String[] args) {
		try {
			VisualizeSignalScenarioWithLanes.main(new String[]{});
		} catch (Exception ee ) {
			ee.printStackTrace();
			Assert.fail("something went wrong") ;
		}
	}

}
