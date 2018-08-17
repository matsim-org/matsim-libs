package org.matsim.core.mobsim.qsim.components;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

public class ComponentFinderTest {
	@Test
	public void testComponentFinder() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Key.get(MobsimEngine.class, Names.named("A"))).toInstance(createFakeEngine());
				bind(Key.get(MobsimEngine.class, Names.named("B"))).toInstance(createFakeEngine());
				bind(MobsimEngine.class).toInstance(createFakeEngine());
			}
		});

		Map<String, Key<MobsimEngine>> result = NamedComponentUtils.find(injector, MobsimEngine.class);

		Assert.assertEquals(2, result.size());
	}

	private static MobsimEngine createFakeEngine() {
		return new MobsimEngine() {
			@Override
			public void doSimStep(double time) {
			}

			@Override
			public void setInternalInterface(InternalInterface internalInterface) {
			}

			@Override
			public void onPrepareSim() {
			}

			@Override
			public void afterSim() {
			}
		};
	}
}
