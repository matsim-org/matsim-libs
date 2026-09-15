package org.matsim.contrib.drt.analysis.zonal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.common.zones.systems.grid.GISFileZoneSystemParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;

/**
 * @author nkuehnel / MOIA
 */
public class ReadOldConfigTest {

    @RegisterExtension
    private MatsimTestUtils utils = new MatsimTestUtils() ;


    @Test
    public void test() {
        {
            URL context = ExamplesUtils.getTestScenarioURL("kelheim");
            Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config-with-drt_old.xml"));
            Assertions.assertThatNoException().isThrownBy(() -> ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class));
            Assertions.assertThatNoException().isThrownBy(() -> ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class));

            new ConfigWriter(config).write(utils.getOutputDirectory() + "output_config.xml");
        }


        {
            // load config file without materializing the config group
            Config config = ConfigUtils.loadConfig(new String[] { utils.getOutputDirectory() + "output_config.xml"});

            // materialize the config group
            MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule( config, MultiModeDrtConfigGroup.class );
            DrtConfigGroup drtConfigGroup = multiModeDrtConfigGroup.getModalElements().iterator().next();

            // check if you are getting back the values from the config file:
            GISFileZoneSystemParams configGroup = (GISFileZoneSystemParams) drtConfigGroup
                    .getParameterSets(GISFileZoneSystemParams.SET_NAME).stream().findFirst().get();

            org.junit.jupiter.api.Assertions.assertEquals( "drt-zones/drt-zonal-system.shp", configGroup.getZonesShapeFile());
            org.junit.jupiter.api.Assertions.assertTrue(drtConfigGroup.getParameterSets("zonalSystem").isEmpty());
        }
    }
}
