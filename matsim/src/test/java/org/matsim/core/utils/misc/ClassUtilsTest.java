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

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author mrieser
 */
public class ClassUtilsTest {

	@Test
	void testInterfaceNoInheritance() {
		Set<Class<?>> set = ClassUtils.getAllTypes(A.class);
		Assertions.assertEquals(1, set.size());
		Assertions.assertTrue(set.contains(A.class));
	}

	@Test
	void testClassNoInheritance() {
		Set<Class<?>> set = ClassUtils.getAllTypes(Z.class);
		Assertions.assertEquals(2, set.size());
		Assertions.assertTrue(set.contains(Z.class));
		Assertions.assertTrue(set.contains(Object.class));
	}

	@Test
	void testInterfaceSingleInheritance() {
		Set<Class<?>> set = ClassUtils.getAllTypes(B.class);
		Assertions.assertEquals(2, set.size());
		Assertions.assertTrue(set.contains(A.class));
		Assertions.assertTrue(set.contains(B.class));
	}

	@Test
	void testClassSingleInheritance() {
		Set<Class<?>> set = ClassUtils.getAllTypes(Y.class);
		Assertions.assertEquals(3, set.size());
		Assertions.assertTrue(set.contains(Z.class));
		Assertions.assertTrue(set.contains(Y.class));
		Assertions.assertTrue(set.contains(Object.class));
	}

	@Test
	void testInterfaceMultipleInheritance_SingleLevel() {
		Set<Class<?>> set = ClassUtils.getAllTypes(AB.class);
		Assertions.assertEquals(3, set.size());
		Assertions.assertTrue(set.contains(A.class));
		Assertions.assertTrue(set.contains(B.class));
		Assertions.assertTrue(set.contains(AB.class));
	}

	@Test
	void testInterfaceMultipleInheritance_MultipleLevel() {
		Set<Class<?>> set = ClassUtils.getAllTypes(C.class);
		Assertions.assertEquals(3, set.size());
		Assertions.assertTrue(set.contains(A.class));
		Assertions.assertTrue(set.contains(B.class));
		Assertions.assertTrue(set.contains(C.class));
	}

	@Test
	void testClassMultipleInheritance_MultipleLevel() {
		Set<Class<?>> set = ClassUtils.getAllTypes(X.class);
		Assertions.assertEquals(4, set.size());
		Assertions.assertTrue(set.contains(Z.class));
		Assertions.assertTrue(set.contains(Y.class));
		Assertions.assertTrue(set.contains(X.class));
		Assertions.assertTrue(set.contains(Object.class));
	}

	@Test
	void testSingleInterfaceImplementation() {
		Set<Class<?>> set = ClassUtils.getAllTypes(Aimpl.class);
		Assertions.assertEquals(3, set.size());
		Assertions.assertTrue(set.contains(Aimpl.class));
		Assertions.assertTrue(set.contains(A.class));
		Assertions.assertTrue(set.contains(Object.class));
	}

	@Test
	void testSingleInterfaceImplementation_MultipleLevel() {
		Set<Class<?>> set = ClassUtils.getAllTypes(Bimpl.class);
		Assertions.assertEquals(4, set.size());
		Assertions.assertTrue(set.contains(Bimpl.class));
		Assertions.assertTrue(set.contains(B.class));
		Assertions.assertTrue(set.contains(A.class));
		Assertions.assertTrue(set.contains(Object.class));
	}

	@Test
	void testMultipleInterfaceImplementation() {
		Set<Class<?>> set = ClassUtils.getAllTypes(ABimpl.class);
		Assertions.assertEquals(4, set.size());
		Assertions.assertTrue(set.contains(ABimpl.class));
		Assertions.assertTrue(set.contains(B.class));
		Assertions.assertTrue(set.contains(A.class));
		Assertions.assertTrue(set.contains(Object.class));
	}

	@Test
	void testComplexClass() {
		Set<Class<?>> set = ClassUtils.getAllTypes(Dimpl.class);
		Assertions.assertEquals(5, set.size());
		Assertions.assertTrue(set.contains(Dimpl.class));
		Assertions.assertTrue(set.contains(Bimpl.class));
		Assertions.assertTrue(set.contains(B.class));
		Assertions.assertTrue(set.contains(A.class));
		Assertions.assertTrue(set.contains(Object.class));

		set = ClassUtils.getAllTypes(BCDimpl.class);
		Assertions.assertEquals(6, set.size());
		Assertions.assertTrue(set.contains(BCDimpl.class));
		Assertions.assertTrue(set.contains(Bimpl.class));
		Assertions.assertTrue(set.contains(C.class));
		Assertions.assertTrue(set.contains(B.class));
		Assertions.assertTrue(set.contains(A.class));
		Assertions.assertTrue(set.contains(Object.class));
	}

	/*package*/ interface A { }
	/*package*/ interface B extends A { }
	/*package*/ interface AB extends A, B { }
	/*package*/ interface C extends B { }

	/*package*/ static class Aimpl implements A { }
	/*package*/ static class Bimpl implements B { }
	/*package*/ static class ABimpl implements A, B { }

	/*package*/ static class Dimpl extends Bimpl { }
	/*package*/ @SuppressWarnings("unused") static class BCDimpl extends Bimpl implements B, C { }

	/*package*/ static class Z { }
	/*package*/ static class Y extends Z { }
	/*package*/ static class X extends Y { }
}
