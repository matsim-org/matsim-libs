/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.mobsim.qsim.pt;

import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
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

    private static final Logger log = Logger.getLogger(SBBTransitQSimEngineIntegrationTest.class);
    @Rule public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testIntegration() {
        TestFixture f = new TestFixture();

        f.config.controler().setOutputDirectory(this.utils.getOutputDirectory());
        f.config.controler().setLastIteration(0);

        Controler controler = new Controler(f.scenario);
        controler.addOverridingModule(new SBBTransitModule());
        controler.configureQSimComponents(components -> {
            SBBTransitEngineQSimModule.configure(components);
        });

        controler.run();

        Mobsim mobsim = controler.getInjector().getInstance(Mobsim.class);
        Assert.assertNotNull(mobsim);
        Assert.assertEquals(QSim.class, mobsim.getClass());

        QSim qsim = (QSim) mobsim;
        QSimComponentsConfig components = qsim.getChildInjector().getInstance(QSimComponentsConfig.class);
        Assert.assertTrue(components.hasNamedComponent(SBBTransitEngineQSimModule.COMPONENT_NAME));
        Assert.assertFalse(components.hasNamedComponent(TransitEngineModule.TRANSIT_ENGINE_NAME));
    }

    @Test
    public void testIntegration_misconfiguration() {
        TestFixture f = new TestFixture();

        Set<String> mainModes = new HashSet<>();
        mainModes.add("car");
        mainModes.add("train");
        f.config.qsim().setMainModes(mainModes);
        f.config.controler().setOutputDirectory(this.utils.getOutputDirectory());
        f.config.controler().setLastIteration(0);

        Controler controler = new Controler(f.scenario);
        controler.addOverridingModule(new SBBTransitModule());
        controler.configureQSimComponents(components -> {
            SBBTransitEngineQSimModule.configure(components);
        });

        try {
            controler.run();
            Assert.fail("Expected exception, got none.");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().endsWith("This will not work! common modes = train"));
        }
    }

}
