package lsp;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public class LSPReadWriteTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();
    private static final String TESTXML_v1 = "testVehicles_v1_withDefaultValues.xml";
    private static final String OUTXML_v1 = "testOutputVehicles_v1.xml";
    private static final String TESTXML_v2 = "testVehicles_v2_withDefaultValues.xml";
    private static final String OUTXML_v2 = "testOutputVehicles_v2.xml";

    public LSPReadWriteTest() {
    }

    @Before
    public void setUp() throws IOException {
    }

    @Test
    public void v1_isWrittenCorrect() throws FileNotFoundException, IOException {

        LSPs lsPs = new LSPs(Collections.emptyList());
        Carriers carriers = new Carriers();
        CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();

        LSPPlanXmlReader reader = new LSPPlanXmlReader(lsPs, carriers, carrierVehicleTypes);
        String inputFilename = utils.getPackageInputDirectory() + "lsps.xml";
        reader.readFile(inputFilename);

        String outputFilename = utils.getOutputDirectory() + "/outputLsps.xml";
        new LSPPlanWriter(lsPs).write(outputFilename);

        MatsimTestUtils.assertEqualFilesLineByLine(inputFilename, outputFilename);
    }


}
