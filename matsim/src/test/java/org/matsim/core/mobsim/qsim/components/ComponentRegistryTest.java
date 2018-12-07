package org.matsim.core.mobsim.qsim.components;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.components.mock.MockComponentAnnotation;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class ComponentRegistryTest {
	@Test
	public void testMissingAnnotation() {
		// Here we may expect an exception in the future. So far we just ignore
		// QSimComponents without any annotation.
		Injector injector = Guice.createInjector(new ModuleWithMissingAnnotation());
		QSimComponentsRegistry registry = QSimComponentsRegistry.create(injector );
	}

	static private class ModuleWithMissingAnnotation extends AbstractModule {
		@Override
		protected void configure() {
			bind(QSimComponent.class).toInstance(new QSimComponent() {
			});
		}
	}

	@Test
	public void testComponentFinder() {
		Injector injector = Guice.createInjector(new ModuleWithProvides());

		Assert.assertEquals(orderedComponents(injector, namedComponents("A")),
				Arrays.asList(Key.get(MobsimEngine.class, Names.named("A"))));

		Assert.assertEquals(orderedComponents(injector, namedComponents("B")),
				Arrays.asList(Key.get(MobsimEngine.class, Names.named("B"))));

		Assert.assertEquals(orderedComponents(injector, namedComponents("A", "B")), Arrays
				.asList(Key.get(MobsimEngine.class, Names.named("A")), Key.get(MobsimEngine.class, Names.named("B"))));

		Assert.assertEquals(orderedComponents(injector, namedComponents("B", "A")), Arrays
				.asList(Key.get(MobsimEngine.class, Names.named("B")), Key.get(MobsimEngine.class, Names.named("A"))));

		Assert.assertEquals(orderedComponents(injector, namedComponents("A", "B", "C")), Arrays
				.asList(Key.get(MobsimEngine.class, Names.named("A")), Key.get(MobsimEngine.class, Names.named("B"))));

		Assert.assertEquals(orderedComponents(injector, namedComponents("C")), Arrays.asList());

		QSimComponentAnnotationsRegistry qSimComponents = namedComponents("A", "B" );
		qSimComponents.addAnnotation(Named.class );
		qSimComponents.addAnnotation(MockComponentAnnotation.class );
		Assert.assertEquals(orderedComponents(injector, qSimComponents),
				Arrays.asList(Key.get(MobsimEngine.class, Names.named("A")),
						Key.get(MobsimEngine.class, Names.named("B")), Key.get(MobsimEngine.class, Named.class),
						Key.get(MobsimEngine.class, MockComponentAnnotation.class)));
	}

	private static class ModuleWithProvides extends AbstractModule {
		@Override
		protected void configure() {
			bind(Key.get(MobsimEngine.class, Names.named("A"))).toInstance(createFakeEngine());
			bind(Key.get(MobsimEngine.class, Names.named("B"))).toInstance(createFakeEngine());
			bind(Key.get(MobsimEngine.class, Named.class)).toInstance(createFakeEngine());
			bind(MobsimEngine.class).toInstance(createFakeEngine());
		}

		@Provides
		@MockComponentAnnotation
		MobsimEngine getMobsimEngine() {
			return createFakeEngine();
		}
	}

	private QSimComponentAnnotationsRegistry namedComponents( String... activeComponentNames ) {
		QSimComponentAnnotationsRegistry qSimComponents = new QSimComponentAnnotationsRegistry();
		for (String n : activeComponentNames) {
			qSimComponents.addNamedAnnotation(n );
		}
		return qSimComponents;
	}

	private List<Key<? extends QSimComponent>> orderedComponents(Injector injector, QSimComponentAnnotationsRegistry qSimComponents ) {
		QSimComponentsRegistry registry = QSimComponentsRegistry.create(injector );
		return registry.getOrderedComponents(qSimComponents);
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
