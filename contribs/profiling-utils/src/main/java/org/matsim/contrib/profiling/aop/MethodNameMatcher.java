/* ********************************************************************** *
 * project: org.matsim.*
 * MethodNameMatcher.java
 *                                                                        *
 * ********************************************************************** *
 *                                                                        *
 * copyright       : (C) 2025 by the members listed in the COPYING,       *
 *                   LICENSE and WARRANTY file.                           *
 * email           : info at matsim dot org                               *
 *                                                                        *
 * ********************************************************************** *
 *                                                                        *
 *   This program is free software; you can redistribute it and/or modify *
 *   it under the terms of the GNU General Public License as published by *
 *   the Free Software Foundation; either version 2 of the License, or    *
 *   (at your option) any later version.                                  *
 *   See also COPYING, LICENSE and WARRANTY file                          *
 *                                                                        *
 * ********************************************************************** */

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
