package org.matsim.freight.logistics.io;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.logistics.FreightLogisticsConfigGroup;
import org.matsim.freight.logistics.LSPUtils;
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

		new CarrierVehicleTypeReader(carrierVehicleTypes).readFile(utils.getPackageInputDirectory() + "vehicles.xml");
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes).readFile(utils.getPackageInputDirectory() + "carriers.xml");
		new LSPPlanXmlReader(lsPs, carriers).readFile(inputFilename);

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

		new CarrierVehicleTypeReader(carrierVehicleTypes).readFile(utils.getPackageInputDirectory() + "vehicles.xml");
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes).readFile(utils.getPackageInputDirectory() + "carriers.xml");
		new LSPPlanXmlReader(lsps, carriers).readFile(inputFilename);

		new LSPPlanXmlWriter(lsps).write(outputFilename);

		//clear and 2nd read - based on written file.
		lsps.getLSPs().clear();

		new LSPPlanXmlReader(lsps, carriers).readFile(outputFilename);

		new LSPPlanXmlWriter(lsps).write(outputFilename2);

		MatsimTestUtils.assertEqualFilesLineByLine(inputFilename, outputFilename2);
	}

	/**
	 * Tests if the LSPs are correctly read from the files set in the config file and written to a new file.
	 */
	@Test
	public void readWriteFromConfigTest() {

		String inputFilename = utils.getPackageInputDirectory() + "lsps.xml";
		String outputFilename = utils.getOutputDirectory() + "/outputLsps.xml";

		Config config = ConfigUtils.createConfig();

		var carriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		carriersConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		carriersConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		var logisticsConfigGroup = ConfigUtils.addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(inputFilename);

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		new LSPPlanXmlWriter(LSPUtils.getLSPs(scenario)).write(outputFilename);

		MatsimTestUtils.assertEqualFilesLineByLine(inputFilename, outputFilename);
	}

	/**
	 * Tests if the LSPs are correctly read from the files set in the config file and written to a new file.
	 * Also tests if the LSPs are correctly read from the written file.
	 */
	@Test
	public void readWriteReadFromConfigTest() {

		String inputFilename = utils.getPackageInputDirectory() + "lsps.xml";
		String outputFilename = utils.getOutputDirectory() + "/outputLsps.xml";

		String carriersFile = utils.getPackageInputDirectory() + "carriers.xml";
		String carriersVehicleTypesFile = utils.getPackageInputDirectory() + "vehicles.xml";

		Config config = ConfigUtils.createConfig();

		var carriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		carriersConfigGroup.setCarriersFile(carriersFile);
		carriersConfigGroup.setCarriersVehicleTypesFile(carriersVehicleTypesFile);

		var logisticsConfigGroup = ConfigUtils.addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(inputFilename);

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		new LSPPlanXmlWriter(LSPUtils.getLSPs(scenario)).write(outputFilename);
		MatsimTestUtils.assertEqualFilesLineByLine(inputFilename, outputFilename);

		// 2nd read - based on written file. Use now the non config way for loading the data.
		// write it out again and compare it to the original input
		LSPs lsps = new LSPs();
		Carriers carriers = new Carriers();
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();

		new CarrierVehicleTypeReader(carrierVehicleTypes).readFile(carriersVehicleTypesFile);
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes).readFile(carriersFile);
		new LSPPlanXmlReader(lsps, carriers).readFile(outputFilename);

		String outputFilename2 = utils.getOutputDirectory() + "/outputLsps2.xml";
		new LSPPlanXmlWriter(lsps).write(outputFilename2);

		MatsimTestUtils.assertEqualFilesLineByLine(inputFilename, outputFilename2);
	}


}
