package org.matsim.core.mobsim.qsim.components.guice;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * This test shows that Guice creates Singletons solely based on the *class
 * name*. So as shown in the example one can bind the same class to different
 * interfaces, even with different names. If the class is declared in the
 * Singleton scope, it will always be the *same* object that is used.
 */
public class MultipleBindingsTest {
	interface InterfaceA {
	}

	interface InterfaceB {
	}

	static class Implementation implements InterfaceA, InterfaceB {

	}

	@Test
	public void testGuiceComponentNaming() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Implementation.class).asEagerSingleton();
				bind(Key.get(InterfaceA.class, Names.named("abc1"))).to(Implementation.class);
				bind(Key.get(InterfaceB.class, Names.named("abc2"))).to(Implementation.class);
			}
		});

		InterfaceA implA = injector.getInstance(Key.get(InterfaceA.class, Names.named("abc1")));
		InterfaceB implB = injector.getInstance(Key.get(InterfaceB.class, Names.named("abc2")));

		Assert.assertSame(implA, implB);
	}
}
