package org.matsim.contrib.profiling.aop;

import com.google.inject.matcher.Matchers;
import org.matsim.contrib.profiling.events.JFRMatsimEvent;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ReplanningListener;

import java.lang.reflect.Modifier;

/**
 * AOP via Guice
 */
public class AOPEventsModule extends AbstractModule {

	public void install() {
		binder().bindInterceptor(Matchers.subclassesOf(ReplanningListener.class).and(Matchers.not((c) -> Modifier.isFinal(c.getModifiers()))),
			MethodNameMatcher.forName("notifyReplanning"),
			new JFREventCreator((invocation) -> JFRMatsimEvent.create("scoring AOP: " + invocation.getMethod().getDeclaringClass().getName())));
	}

}
