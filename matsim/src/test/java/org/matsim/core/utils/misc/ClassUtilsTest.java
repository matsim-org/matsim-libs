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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author mrieser
 */
public class ClassUtilsTest {

	@Test
	public void testInterfaceNoInheritance() {
		Set<Class<?>> set = ClassUtils.getAllTypes(A.class);
		Assert.assertEquals(1, set.size());
		Assert.assertTrue(set.contains(A.class));
	}

	@Test
	public void testClassNoInheritance() {
		Set<Class<?>> set = ClassUtils.getAllTypes(Z.class);
		Assert.assertEquals(2, set.size());
		Assert.assertTrue(set.contains(Z.class));
		Assert.assertTrue(set.contains(Object.class));
	}

	@Test
	public void testInterfaceSingleInheritance() {
		Set<Class<?>> set = ClassUtils.getAllTypes(B.class);
		Assert.assertEquals(2, set.size());
		Assert.assertTrue(set.contains(A.class));
		Assert.assertTrue(set.contains(B.class));
	}

	@Test
	public void testClassSingleInheritance() {
		Set<Class<?>> set = ClassUtils.getAllTypes(Y.class);
		Assert.assertEquals(3, set.size());
		Assert.assertTrue(set.contains(Z.class));
		Assert.assertTrue(set.contains(Y.class));
		Assert.assertTrue(set.contains(Object.class));
	}

	@Test
	public void testInterfaceMultipleInheritance_SingleLevel() {
		Set<Class<?>> set = ClassUtils.getAllTypes(AB.class);
		Assert.assertEquals(3, set.size());
		Assert.assertTrue(set.contains(A.class));
		Assert.assertTrue(set.contains(B.class));
		Assert.assertTrue(set.contains(AB.class));
	}

	@Test
	public void testInterfaceMultipleInheritance_MultipleLevel() {
		Set<Class<?>> set = ClassUtils.getAllTypes(C.class);
		Assert.assertEquals(3, set.size());
		Assert.assertTrue(set.contains(A.class));
		Assert.assertTrue(set.contains(B.class));
		Assert.assertTrue(set.contains(C.class));
	}

	@Test
	public void testClassMultipleInheritance_MultipleLevel() {
		Set<Class<?>> set = ClassUtils.getAllTypes(X.class);
		Assert.assertEquals(4, set.size());
		Assert.assertTrue(set.contains(Z.class));
		Assert.assertTrue(set.contains(Y.class));
		Assert.assertTrue(set.contains(X.class));
		Assert.assertTrue(set.contains(Object.class));
	}

	@Test
	public void testSingleInterfaceImplementation() {
		Set<Class<?>> set = ClassUtils.getAllTypes(Aimpl.class);
		Assert.assertEquals(3, set.size());
		Assert.assertTrue(set.contains(Aimpl.class));
		Assert.assertTrue(set.contains(A.class));
		Assert.assertTrue(set.contains(Object.class));
	}

	@Test
	public void testSingleInterfaceImplementation_MultipleLevel() {
		Set<Class<?>> set = ClassUtils.getAllTypes(Bimpl.class);
		Assert.assertEquals(4, set.size());
		Assert.assertTrue(set.contains(Bimpl.class));
		Assert.assertTrue(set.contains(B.class));
		Assert.assertTrue(set.contains(A.class));
		Assert.assertTrue(set.contains(Object.class));
	}

	@Test
	public void testMultipleInterfaceImplementation() {
		Set<Class<?>> set = ClassUtils.getAllTypes(ABimpl.class);
		Assert.assertEquals(4, set.size());
		Assert.assertTrue(set.contains(ABimpl.class));
		Assert.assertTrue(set.contains(B.class));
		Assert.assertTrue(set.contains(A.class));
		Assert.assertTrue(set.contains(Object.class));
	}

	@Test
	public void testComplexClass() {
		Set<Class<?>> set = ClassUtils.getAllTypes(Dimpl.class);
		Assert.assertEquals(5, set.size());
		Assert.assertTrue(set.contains(Dimpl.class));
		Assert.assertTrue(set.contains(Bimpl.class));
		Assert.assertTrue(set.contains(B.class));
		Assert.assertTrue(set.contains(A.class));
		Assert.assertTrue(set.contains(Object.class));

		set = ClassUtils.getAllTypes(BCDimpl.class);
		Assert.assertEquals(6, set.size());
		Assert.assertTrue(set.contains(BCDimpl.class));
		Assert.assertTrue(set.contains(Bimpl.class));
		Assert.assertTrue(set.contains(C.class));
		Assert.assertTrue(set.contains(B.class));
		Assert.assertTrue(set.contains(A.class));
		Assert.assertTrue(set.contains(Object.class));
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
