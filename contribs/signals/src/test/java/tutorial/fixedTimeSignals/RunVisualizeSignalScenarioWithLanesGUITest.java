package tutorial.fixedTimeSignals;

import org.junit.Assert;
import org.junit.Test;

public class RunVisualizeSignalScenarioWithLanesGUITest {

	public static void main(String[] args) {
		try {
			VisualizeSignalScenarioWithLanes.main(new String[]{});
		} catch (Exception ee ) {
			ee.printStackTrace();
			Assert.fail("something went wrong") ;
		}
	}
	
	@Test
	public void testSignalExampleVisualization(){
		main(null);
	}

}
