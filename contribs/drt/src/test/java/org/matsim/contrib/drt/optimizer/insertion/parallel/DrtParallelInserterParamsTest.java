package org.matsim.contrib.drt.optimizer.insertion.parallel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DrtParallelInserterParamsTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    void DrtParallelInserterParamsIOTest() {

		String outputPath = utils.getOutputDirectory();
        Config config = ConfigUtils.createConfig();
		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();

        DrtParallelInserterParams originalParams = new DrtParallelInserterParams();
        originalParams.setCollectionPeriod(30.0);
        originalParams.setMaxIterations(5);
        originalParams.setMaxPartitions(8);
        originalParams.setInsertionSearchThreadsPerWorker(2);
        originalParams.setLogThreadActivity(true);
        originalParams.setVehiclesPartitioner(DrtParallelInserterParams.VehiclesPartitioner.RoundRobinVehicleEntryPartitioner);
        originalParams.setRequestsPartitioner(DrtParallelInserterParams.RequestsPartitioner.RoundRobinRequestsPartitioner);

		drtConfigGroup.addParameterSet(originalParams);

        config.addModule(drtConfigGroup);

        File configFile = Path.of(outputPath).resolve("config.xml").toFile();
        ConfigUtils.writeMinimalConfig(config,configFile.toString());


        Config loadedConfig = ConfigUtils.loadConfig(configFile.getAbsolutePath(), new DrtConfigGroup());
		DrtConfigGroup read = ConfigUtils.addOrGetModule(loadedConfig,DrtConfigGroup.class);
		assertTrue(read.getDrtParallelInserterParams().isPresent());

		var loadedParams = read.getDrtParallelInserterParams().get();

        assertEquals(30.0, loadedParams.getCollectionPeriod());
        assertEquals(5, loadedParams.getMaxIterations());
        assertEquals(8, loadedParams.getMaxPartitions());
        assertEquals(2, loadedParams.getInsertionSearchThreadsPerWorker());
        assertTrue(loadedParams.isLogThreadActivity());
        assertEquals(DrtParallelInserterParams.VehiclesPartitioner.RoundRobinVehicleEntryPartitioner, loadedParams.getVehiclesPartitioner());
        assertEquals(DrtParallelInserterParams.RequestsPartitioner.RoundRobinRequestsPartitioner, loadedParams.getRequestsPartitioner());
    }
}
