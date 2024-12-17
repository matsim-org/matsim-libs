package org.matsim.freight.logistics.io;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.freight.carriers.CarrierPlanXmlReader;
import org.matsim.freight.carriers.CarrierVehicleTypeReader;
import org.matsim.freight.carriers.CarrierVehicleTypes;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.logistics.LSPs;
import org.matsim.testcases.MatsimTestUtils;

public class LSPReadWriteTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void readWriteTest() {

		LSPs lsPs = new LSPs(Collections.emptyList());
		Carriers carriers = new Carriers();
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();

		String inputFilename = utils.getPackageInputDirectory() + "lsps.xml";
		String outputFilename = utils.getOutputDirectory() + "/outputLsps.xml";

		CarrierVehicleTypeReader vehicleTypeReader = new CarrierVehicleTypeReader(carrierVehicleTypes);
		vehicleTypeReader.readFile(utils.getPackageInputDirectory() + "vehicles.xml");

		CarrierPlanXmlReader carrierReader = new CarrierPlanXmlReader(carriers, carrierVehicleTypes);
		carrierReader.readFile(utils.getPackageInputDirectory() + "carriers.xml");

		LSPPlanXmlReader reader = new LSPPlanXmlReader(lsPs, carriers);
		reader.readFile(inputFilename);

		new LSPPlanXmlWriter(lsPs).write(outputFilename);

		MatsimTestUtils.assertEqualFilesLineByLine(inputFilename, outputFilename);
	}

	@Test
	public void readWriteReadTest() {

		LSPs lsps = new LSPs(Collections.emptyList());
		Carriers carriers = new Carriers();
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();

		String inputFilename = utils.getPackageInputDirectory() + "lsps.xml";
		String outputFilename = utils.getOutputDirectory() + "/outputLsps.xml";
		String outputFilename2 = utils.getOutputDirectory() + "/outputLsps2.xml";

		CarrierVehicleTypeReader vehicleTypeReader = new CarrierVehicleTypeReader(carrierVehicleTypes);
		vehicleTypeReader.readFile(utils.getPackageInputDirectory() + "vehicles.xml");

		CarrierPlanXmlReader carrierReader = new CarrierPlanXmlReader(carriers, carrierVehicleTypes);
		carrierReader.readFile(utils.getPackageInputDirectory() + "carriers.xml");

		LSPPlanXmlReader reader = new LSPPlanXmlReader(lsps, carriers);
		reader.readFile(inputFilename);

		new LSPPlanXmlWriter(lsps).write(outputFilename);

		//clear and 2nd read - based on written file.
		lsps.getLSPs().clear();

		LSPPlanXmlReader reader2 = new LSPPlanXmlReader(lsps, carriers);
		reader2.readFile(outputFilename);

		new LSPPlanXmlWriter(lsps).write(outputFilename2);

		MatsimTestUtils.assertEqualFilesLineByLine(inputFilename, outputFilename2);
	}




}
