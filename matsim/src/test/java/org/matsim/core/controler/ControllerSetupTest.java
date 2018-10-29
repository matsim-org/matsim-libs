package org.matsim.core.controler;

import java.util.concurrent.atomic.AtomicBoolean;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import org.junit.Assert;
import org.junit.Test;

public class ControllerSetupTest {
	@Test
	public void testAddModule() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		
		Controler controller = new Controler(config);
		
		AtomicBoolean called = new AtomicBoolean(false);
		controller.addModule(new MockModule(called));
		controller.run();
		
		Assert.assertTrue(called.get());
	}
	
	@Test
	public void testAddOverridingModule() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		
		Controler controller = new Controler(config);
		
		AtomicBoolean called = new AtomicBoolean(false);
		controller.addOverridingModule(new MockModule(called));
		controller.run();
		
		Assert.assertTrue(called.get());
	}
	
	@Test(expected = com.google.inject.CreationException.class)
	public void testAddModuleTwice() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		
		Controler controller = new Controler(config);
		
		AtomicBoolean called1 = new AtomicBoolean(false);
		AtomicBoolean called2 = new AtomicBoolean(false);
		
		controller.addModule(new MockModule(called1));
		controller.addModule(new MockModule(called2));
		
		controller.run();
	}
	
	@Test()
	public void testAddOverridingModuleTwice() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		
		Controler controller = new Controler(config);
		
		AtomicBoolean called1 = new AtomicBoolean(false);
		AtomicBoolean called2 = new AtomicBoolean(false);
		
		controller.addOverridingModule(new MockModule(called1));
		controller.addOverridingModule(new MockModule(called2));
		
		controller.run();
		
		Assert.assertFalse(called1.get());
		Assert.assertTrue(called2.get());
	}

	@Test()
	public void testOverrideModule() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		
		Controler controller = new Controler(config);
		
		AtomicBoolean called1 = new AtomicBoolean(false);
		AtomicBoolean called2 = new AtomicBoolean(false);
		
		controller.addModule(new MockModule(called1));
		controller.addOverridingModule(new MockModule(called2));
		
		controller.run();
		
		Assert.assertFalse(called1.get());
		Assert.assertTrue(called2.get());
	}
	
	public void testSetOverridingModule() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		
		Controler controller = new Controler(config);
		
		AtomicBoolean called1 = new AtomicBoolean(false);
		AtomicBoolean called2 = new AtomicBoolean(false);
		
		controller.addModule(new MockModule(called1));
		controller.setOverridingModule(new MockModule(called2));
		
		controller.run();
		
		Assert.assertFalse(called1.get());
		Assert.assertTrue(called2.get());
	}	
	
	static private class MockModule extends AbstractModule {
		private final AtomicBoolean called;
		
		public MockModule(AtomicBoolean called) {
			this.called = called;
		}
		
		@Override
		public void install() {
			addControlerListenerBinding().to(MockListener.class);
			bind(MockListener.class).toInstance(new MockListener(called));
		}
	}
	
	static private class MockListener implements StartupListener {
		private final AtomicBoolean called;
		
		public MockListener(AtomicBoolean called) {
			this.called = called;
		}

		@Override
		public void notifyStartup(StartupEvent event) {
			called.set(true);
		}
	}	
}
