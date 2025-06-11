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
package ch.sbb.matsim.mobsim.qsim.pt;

import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.pt.TransitEngineModule;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser / SBB
 */
public class SBBTransitQSimEngineIntegrationTest {

    private static final Logger log = LogManager.getLogger(SBBTransitQSimEngineIntegrationTest.class);
    @RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testIntegration() {
        TestFixture f = new TestFixture();

        f.config.controller().setOutputDirectory(this.utils.getOutputDirectory());
        f.config.controller().setLastIteration(0);

        Controler controler = new Controler(f.scenario);
        controler.addOverridingModule(new SBBTransitModule());
        controler.configureQSimComponents(components -> {
            new SBBTransitEngineQSimModule().configure(components);
        });

        controler.run();

        Mobsim mobsim = controler.getInjector().getInstance(Mobsim.class);
        Assertions.assertNotNull(mobsim);
        Assertions.assertEquals(QSim.class, mobsim.getClass());

        QSim qsim = (QSim) mobsim;
        QSimComponentsConfig components = qsim.getChildInjector().getInstance(QSimComponentsConfig.class);
        Assertions.assertTrue(components.hasNamedComponent(SBBTransitEngineQSimModule.COMPONENT_NAME));
        Assertions.assertFalse(components.hasNamedComponent(TransitEngineModule.TRANSIT_ENGINE_NAME));
    }

	@Test
	void testIntegration_misconfiguration() {
        TestFixture f = new TestFixture();

        Set<String> mainModes = new HashSet<>();
        mainModes.add("car");
        mainModes.add("train");
        f.config.qsim().setMainModes(mainModes);
        f.config.controller().setOutputDirectory(this.utils.getOutputDirectory());
        f.config.controller().setLastIteration(0);

        Controler controler = new Controler(f.scenario);
        controler.addOverridingModule(new SBBTransitModule());
        controler.configureQSimComponents(components -> {
            new SBBTransitEngineQSimModule().configure(components);
        });

        try {
            controler.run();
            Assertions.fail("Expected exception, got none.");
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.getMessage().endsWith("This will not work! common modes = train"));
        }
    }

}
