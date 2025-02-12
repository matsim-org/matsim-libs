package org.matsim.contrib.profiling.aop;

import com.google.inject.matcher.Matcher;

import java.lang.reflect.Method;
import java.util.Objects;

public class MethodNameMatcher implements Matcher<Method> {

	public static Matcher<Method> forName(String methodName) {
		return new MethodNameMatcher(methodName);
	}

	private final String methodName;

	public MethodNameMatcher(String methodName) {
		this.methodName = Objects.requireNonNull(methodName, "Method name to match must not be null");
	}

	@Override
	public boolean matches(Method method) {
		return method.getName().equals(methodName);
	}

	@Override
	public String toString() {
		return "methodCalled(" + methodName + ")";
	}
}
