/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

/**
 * @author mrieser / SBB
 */
public class RaptorUtilsTest {

    @Before
    public void setup() {
        System.setProperty("matsim.preferLocalDtds", "true");
    }

    @Test
    public void testConfigLoading() {
        // prepare config
        SwissRailRaptorConfigGroup srrConfig = new SwissRailRaptorConfigGroup();
        Config config1 = ConfigUtils.createConfig(srrConfig);
        srrConfig.setUseRangeQuery(true);
        srrConfig.setUseIntermodalAccessEgress(true);

        // write config1
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(output);
        new ConfigWriter(config1).writeStream(writer);

        // read config in again as config2
        Config config2 = ConfigUtils.createConfig();
        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        new ConfigReader(config2).parse(input);

        // first checks
        ConfigGroup srrConfig2 = config2.getModules().get(SwissRailRaptorConfigGroup.GROUP);
        Assert.assertNotNull(srrConfig2);
        Assert.assertEquals(ConfigGroup.class, srrConfig2.getClass());

        // create RaptorConfig, test if SwissRailRaptorConfigGroup got created
        RaptorParameters raptorParams = RaptorUtils.createParameters(config2);
        srrConfig2 = config2.getModules().get(SwissRailRaptorConfigGroup.GROUP);
        Assert.assertNotNull(srrConfig2);
        Assert.assertEquals(SwissRailRaptorConfigGroup.class, srrConfig2.getClass());

        Assert.assertNotNull(raptorParams.getConfig());
        Assert.assertEquals(srrConfig2, raptorParams.getConfig());

        RaptorParameters raptorParams2 = RaptorUtils.createParameters(config2);
        Assert.assertEquals("the same config object should be returned in subsequent calls.", srrConfig2, raptorParams2.getConfig());

        // check that the config is actually what we configured
        Assert.assertTrue(raptorParams2.getConfig().isUseRangeQuery());
        Assert.assertTrue(raptorParams2.getConfig().isUseIntermodalAccessEgress());
        Assert.assertFalse(raptorParams2.getConfig().isUseModeMappingForPassengers());
    }
}
