/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

/**
 * @author mrieser / SBB
 */
public class SBBTransitConfigGroupTest {

    @Test
    public void testConfigIO() {
        System.setProperty("matsim.preferLocalDtds", "true");

        SBBTransitConfigGroup ptConfig1 = new SBBTransitConfigGroup();
        Config config1 = ConfigUtils.createConfig(ptConfig1);

        ptConfig1.setDeterministicServiceModes(Collections.singleton("schienenfahrzeug"));
        ptConfig1.setCreateLinkEventsInterval(4);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(output);
        new ConfigWriter(config1).writeStream(writer);

        SBBTransitConfigGroup ptConfig2 = new SBBTransitConfigGroup();
        Config config2 = ConfigUtils.createConfig(ptConfig2);

        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        new ConfigReader(config2).parse(input);

        Assert.assertEquals(1, ptConfig2.getDeterministicServiceModes().size());
        Assert.assertTrue(ptConfig2.getDeterministicServiceModes().contains("schienenfahrzeug"));
        Assert.assertEquals(4, ptConfig2.getCreateLinkEventsInterval());
    }
}
