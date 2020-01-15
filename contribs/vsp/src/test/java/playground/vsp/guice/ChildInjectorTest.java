package playground.vsp.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.junit.Test;

public class ChildInjectorTest{


	@Test public void test() {
		Injector root = Guice.createInjector();

		Injector firstChild = root.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				this.bind(MySingleton.class).toInstance(new MySingleton());
			}
		});
		Injector secondChild = root.createChildInjector();
		Injector thirdChild = root.createChildInjector();

		MySingleton singleton1 = firstChild.getInstance(MySingleton.class);
		MySingleton singleton2 = secondChild.getInstance(MySingleton.class);
		MySingleton singleton3 = thirdChild.getInstance(MySingleton.class);

		assert singleton1 != singleton2;
		assert singleton2 == singleton3;
	}

	@Singleton
	private static class MySingleton implements MySingletonI {}

	private interface MySingletonI {}

}
