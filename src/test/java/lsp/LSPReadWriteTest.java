package lsp;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.Vehicles;

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

		String inputFilename = utils.getPackageInputDirectory() + "lsps.xml";
		String outputFilename = utils.getOutputDirectory() + "/outputLsps.xml";

		LSPPlanXmlReader reader = new LSPPlanXmlReader(lsPs, carriers);
		reader.readFile(inputFilename);

		CarrierVehicleTypeReader vehicleTypeReader = new CarrierVehicleTypeReader(carrierVehicleTypes);
		vehicleTypeReader.readFile(utils.getPackageInputDirectory() + "vehicles.xml");

		CarrierPlanXmlReader carrierReader = new CarrierPlanXmlReader(carriers, carrierVehicleTypes);
		carrierReader.readFile(utils.getPackageInputDirectory() + "carriers.xml");

		new LSPPlanXmlWriter(lsPs).write(outputFilename);

		MatsimTestUtils.assertEqualFilesLineByLine(inputFilename, outputFilename);
	}


}
