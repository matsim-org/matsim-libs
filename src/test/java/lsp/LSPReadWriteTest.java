package lsp;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.testcases.MatsimTestUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;

public class LSPReadWriteTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void readWriteTest() throws FileNotFoundException, IOException {

		LSPs lsPs = new LSPs(Collections.emptyList());
		Carriers carriers = new Carriers();
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();

		LSPPlanXmlReader reader = new LSPPlanXmlReader(lsPs, carriers);
		String inputFilename = utils.getPackageInputDirectory() + "lsps.xml";
		reader.readFile(inputFilename);

		String outputFilename = utils.getOutputDirectory() + "/outputLsps.xml";
		new LSPPlanXmlWriter(lsPs).write(outputFilename);

		MatsimTestUtils.assertEqualFilesLineByLine(inputFilename, outputFilename);
	}


}
