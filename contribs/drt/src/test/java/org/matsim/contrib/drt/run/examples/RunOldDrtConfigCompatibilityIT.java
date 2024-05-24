package org.matsim.contrib.drt.run.examples;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;

/**
 *
 * Tests the older drt config version where optimization constraints where not stored as separate parameters
 * @author nkuehnel / MOIA
 */
public class RunOldDrtConfigCompatibilityIT {

    @RegisterExtension
    private MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    void testRunDrtExampleWithNoRejections_ExtensiveSearch() {
        Id.resetCaches();

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
        matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

        URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config_v1.xml");
        Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfigGroup,
                new OTFVisConfigGroup());

        for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
            //disable rejections
			drtCfg.addOrGetDrtOptimizationConstraintsParams()
                    .addOrGetDefaultDrtOptimizationConstraintsSet()
                    .rejectRequestIfMaxWaitOrTravelTimeViolated = false;
        }

        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controller().setOutputDirectory(utils.getOutputDirectory());
        RunDrtExample.run(config, false);

        var expectedStats = RunDrtExampleIT.Stats.newBuilder()
                .rejectionRate(0.0)
                .rejections(0)
                .waitAverage(297.19)
                .inVehicleTravelTimeMean(386.78)
                .totalTravelTimeMean(683.97)
                .build();

        RunDrtExampleIT.verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
    }
}
