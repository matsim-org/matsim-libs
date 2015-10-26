package playground.wrashid.lib.tools.txtConfig;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class TxtConfigTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
    @Test
    public final void basicTests() {
    	String configFile = utils.getInputDirectory()+ "config.txt";
    	TxtConfig config=new TxtConfig(configFile);
    	
    	Assert.assertEquals("H:/data/ktiRun22/output/",config.getParameterValue("baseFolder"));
    	Assert.assertEquals("H:/data/ktiRun22/output/ITERS/it.50/50.events.xml.gz",config.getParameterValue("eventsFile"));
    	
    	Assert.assertEquals("AB",config.getParameterValue("b"));
    	Assert.assertEquals("ABC",config.getParameterValue("c"));
    	Assert.assertEquals("ABCD",config.getParameterValue("d"));
    	Assert.assertEquals("ABCDE",config.getParameterValue("e"));
    	
    	String parameterValue = config.getParameterValue("outputDirectory");
		Assert.assertEquals("test/input/playground/wrashid/lib/tools/txtConfig/TxtConfigTest/basicTests/output/",parameterValue);
    	
    }
	
}
