/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.utils.misc;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * @author mrieser
 */
public final class ClassUtils {
	private ClassUtils(){} // do not instantiate

	/**
	 * Returns all classes and implemented interfaces of the given class.
	 * For each of the returned classes, an according call to
	 * <code>instanceof</code> would also return <code>true</code>.
	 *
	 * @param klass
	 * @return all classes and implemented interfaces of the given class
	 */
	public static Set<Class<?>> getAllTypes(final Class<?> klass) {
		Set<Class<?>> set = new HashSet<Class<?>>();
		Stack<Class<?>> stack = new Stack<Class<?>>();
		stack.add(klass);

		while (!stack.isEmpty()) {
			Class<?> c = stack.pop();
			set.add(c);
			for (Class<?> k : c.getInterfaces()) {
				stack.push(k);
			}
			if (c.getSuperclass() != null) {
				stack.push(c.getSuperclass());
			}
		}

		return set;
	}

	public static Set<Class<?>> getAllInterfaces( final Class<?> klass ) {
		Set<Class<?>> intfs = new HashSet<Class<?>>();
		for (Class<?> intf : klass.getInterfaces()) {
			intfs.add(intf);
			intfs.addAll(getAllInterfaces(intf));
		}
		if (!klass.isInterface()) {
			Class<?> superclass = klass.getSuperclass();
			while (superclass != Object.class) {
				intfs.addAll(getAllInterfaces(superclass));
				superclass = superclass.getSuperclass();
			}
		}
		return intfs;
	}
}
