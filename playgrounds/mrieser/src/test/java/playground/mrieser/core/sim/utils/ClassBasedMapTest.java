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

package playground.mrieser.core.sim.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author mrieser
 */
public class ClassBasedMapTest {

	@Test
	public void testGet_Simple() {
		ClassBasedMap<Object, String> map = new ClassBasedMap<Object, String>();
		map.put(String.class, "String");
		map.put(Integer.class, "Integer");
		map.put(Float.class, "Float");

		Assert.assertEquals("String", map.get(String.class));
		Assert.assertEquals("Integer", map.get(Integer.class));
		Assert.assertEquals("Float", map.get(Float.class));
		Assert.assertNull(map.get(Double.class));
	}

	@Test
	public void testGet_withInheritance() {
		ClassBasedMap<Object, String> map = new ClassBasedMap<Object, String>();
		map.put(A.class, "A");
		map.put(B.class, "B");

		Assert.assertEquals("A", map.get(A.class));
		Assert.assertEquals("B", map.get(B.class));
		Assert.assertEquals("A", map.get(Aimpl.class));
		Assert.assertEquals("B", map.get(Bimpl.class));
		Assert.assertEquals("B", map.get(Cimpl.class));
	}

	/**
	 * Tests an internal implementation detail: if the cache works correctly
	 */
	@Test
	public void testGet_withInheritance_Cached() {
		ClassBasedMap<Object, String> map = new ClassBasedMap<Object, String>();
		map.put(A.class, "A");
		map.put(B.class, "B");

		Assert.assertEquals("A", map.get(A.class));
		Assert.assertEquals("B", map.get(B.class));
		Assert.assertEquals("A", map.get(Aimpl.class));
		Assert.assertEquals("B", map.get(Bimpl.class));
		Assert.assertEquals("B", map.get(Cimpl.class));

		// call all again to test the cache

		Assert.assertEquals("A", map.get(A.class));
		Assert.assertEquals("B", map.get(B.class));
		Assert.assertEquals("A", map.get(Aimpl.class));
		Assert.assertEquals("B", map.get(Bimpl.class));
		Assert.assertEquals("B", map.get(Cimpl.class));
	}

	@Test
	public void testGet_withMultipleInheritance() {
		ClassBasedMap<Object, String> map = new ClassBasedMap<Object, String>();
		map.put(A.class, "A");
		map.put(B.class, "B");
		map.put(ABimpl.class, "AB");

		Assert.assertEquals("AB", map.get(ABimpl.class));
	}

	@Test
	public void testRemove() {
		ClassBasedMap<Object, String> map = new ClassBasedMap<Object, String>();
		map.put(A.class, "A");
		map.put(B.class, "B");

		Assert.assertEquals("A", map.get(A.class));
		Assert.assertEquals("A", map.get(Aimpl.class));
		Assert.assertEquals("B", map.get(B.class));

		Assert.assertEquals("A", map.remove(A.class));
		Assert.assertNull(map.get(A.class));
		Assert.assertNull(map.get(Aimpl.class));
		Assert.assertEquals("B", map.get(B.class));
	}

	@Test
	public void testPut_Replace() {
		ClassBasedMap<Object, String> map = new ClassBasedMap<Object, String>();
		map.put(A.class, "A");
		map.put(B.class, "B");

		Assert.assertEquals("A", map.get(A.class));
		Assert.assertEquals("A", map.get(Aimpl.class));
		Assert.assertEquals("B", map.get(B.class));

		Assert.assertEquals("A", map.put(A.class, "aa"));

		Assert.assertEquals("aa", map.get(A.class));
		Assert.assertEquals("aa", map.get(Aimpl.class));
		Assert.assertEquals("B", map.get(B.class));
	}

	@Test
	public void testGet_Superclass() {
		ClassBasedMap<Object, String> map = new ClassBasedMap<Object, String>();
		map.put(Aimpl.class, "A");

		Assert.assertEquals("A", map.get(Aimpl.class));
		Assert.assertNull(map.get(A.class));
	}

	/*package*/ interface A { }
	/*package*/ interface B { }
	/*package*/ class Aimpl implements A { }
	/*package*/ class Bimpl implements B { }
	/*package*/ class Cimpl extends Bimpl { }
	/*package*/ class ABimpl implements A, B { }
}
