package playground.polettif.multiModalMap.gtfs;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import static playground.polettif.multiModalMap.gtfs.GTFSReader.convertGTFS2MATSimTransitSchedule;

public class GTFSReaderTest {

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test() throws Exception {
	//	 convertGTFS2MATSimTransitSchedule("./test/playground/polettif/gtfs_sample/", utils.getOutputDirectory()+"converted_gtfs_sample.xml");
	}
}