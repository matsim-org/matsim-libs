package org.matsim.core.mobsim;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.AbstractMobsimModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class AbstractMobsimModuleTest {
	@Test
	public void testOverrides() {
		AbstractMobsimModule moduleA = new AbstractMobsimModule() {
			@Override
			protected void configureMobsim() {
				getConfig().createModule("testA");
				bind(String.class).toInstance("testAString");
			}
		};

		AbstractMobsimModule moduleB = new AbstractMobsimModule() {
			@Override
			protected void configureMobsim() {
				getConfig().createModule("testB");
				bind(String.class).toInstance("testBString");
			}
		};

		AbstractMobsimModule composite = AbstractMobsimModule.overrideMobsimModules(Collections.singleton(moduleA),
				Collections.singletonList(moduleB));

		Config config = ConfigUtils.createConfig();
		composite.setConfig(config);

		Injector injector = Guice.createInjector(composite);

		Assert.assertTrue(config.getModules().containsKey("testA"));
		Assert.assertTrue(config.getModules().containsKey("testB"));

		Assert.assertEquals("testBString", injector.getInstance(String.class));
	}
}
