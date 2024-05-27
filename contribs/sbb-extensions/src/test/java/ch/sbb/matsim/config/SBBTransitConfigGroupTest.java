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
package ch.sbb.matsim.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

/**
 * @author mrieser / SBB
 */
public class SBBTransitConfigGroupTest {

	@Test
	void testConfigIO() {
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

        Assertions.assertEquals(1, ptConfig2.getDeterministicServiceModes().size());
        Assertions.assertTrue(ptConfig2.getDeterministicServiceModes().contains("schienenfahrzeug"));
        Assertions.assertEquals(4, ptConfig2.getCreateLinkEventsInterval());
    }
}
