package org.matsim.core.mobsim.qsim.components.guice;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * This test shows how Guice works with child injectors and explicit bindings.
 * In the test there is a ParentScopeObject in a parent injector. Then a child
 * injector is created which contains a ChildScopeObject, which has the
 * ParentScopeObject as a dependency. Also, the child injector provides a
 * OtherChildScopeObject.
 * 
 * Now, only OtherChildScopeObject is requested (which has no dependency
 * whatsoever). Nevertheless, because explicit bindings are required in the
 * parent injector, an exception is thrown if ParentScopeObject is not
 * explicitly bound either injector. Interestingly, the error is raised when
 * calling createChildInjector, not when createInjector is called. This is a bit
 * confusing, but makes sense.
 */
public class ExplicitBindingsRequiredTest {
	static class ParentScopeObject {

	}

	static class ChildScopeObject {
		@Inject
		ChildScopeObject(ParentScopeObject o) {
			System.out.println("ChildScopeObject");
		}
	}

	static class OtherChildScopeObject {

	}

	@Test
	public void testExplicitBindings() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				binder().requireExplicitBindings();
				// bind(ParentScopeObject.class);
			}
		});

		boolean exceptionOccured = false;

		try {
			injector.createChildInjector(new AbstractModule() {
				@Override
				protected void configure() {
					bind(ChildScopeObject.class);
					bind(OtherChildScopeObject.class);
				}
			});
		} catch (CreationException e) {
			exceptionOccured = true;
		}

		Assert.assertTrue(exceptionOccured);
	}
}
