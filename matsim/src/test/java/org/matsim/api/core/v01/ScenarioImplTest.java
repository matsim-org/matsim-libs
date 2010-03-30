package org.matsim.api.core.v01;

import junit.framework.Assert;

import org.junit.Test;

/**
 * @author mrieser
 */
public class ScenarioImplTest {

	/**
	 * Tests that {@link ScenarioImpl#createId(String)} returns the same
	 * Id object for equil Strings.
	 */
	@Test
	public void testCreateId_sameObjectForSameId() {
		ScenarioImpl s = new ScenarioImpl();
		String str1 = "9832";
		String str2 = new String(str1);
		Assert.assertNotSame(str1, str2);
		Assert.assertEquals(str1, str2);
		Id id1 = s.createId(str1);
		Id id2 = s.createId(str2);
		Id id3 = s.createId(str1);
		Assert.assertSame(id1, id2);
		Assert.assertSame(id1, id3);
		Assert.assertSame(id2, id3);
	}

	@Test
	public void testAddGetScenarioElement_simple() {
		Scenario s = new ScenarioImpl();
		Assert.assertNull(s.getScenarioElement(A.class));
		Assert.assertNull(s.getScenarioElement(AImpl.class));
		A a = new AImpl();
		s.addScenarioElement(a);
		Assert.assertEquals(a, s.getScenarioElement(A.class));
		Assert.assertEquals(a, s.getScenarioElement(AImpl.class));
	}

	@Test
	public void testAddGetScenarioElement_complex() {
		Scenario s = new ScenarioImpl();

		B b = new BImpl();
		C c = new CImpl();

		s.addScenarioElement(b);
		Assert.assertEquals(b, s.getScenarioElement(BImpl.class));
		Assert.assertEquals(b, s.getScenarioElement(B.class));
		Assert.assertEquals(b, s.getScenarioElement(AImpl.class));
		Assert.assertEquals(b, s.getScenarioElement(A.class));

		s.addScenarioElement(c);
		Assert.assertEquals(c, s.getScenarioElement(CImpl.class));
		Assert.assertEquals(c, s.getScenarioElement(C.class));
		Assert.assertEquals(b, s.getScenarioElement(BImpl.class));
		Assert.assertEquals(c, s.getScenarioElement(B.class));
		Assert.assertEquals(b, s.getScenarioElement(AImpl.class));
		Assert.assertEquals(b, s.getScenarioElement(A.class));
	}

	@Test
	public void testRemoveScenarioElement_simple() {
		Scenario s = new ScenarioImpl();
		A a = new AImpl();
		s.addScenarioElement(a);
		Assert.assertEquals(a, s.getScenarioElement(A.class));
		Assert.assertEquals(a, s.getScenarioElement(AImpl.class));
		Assert.assertFalse(s.removeScenarioElement(new AImpl()));
		Assert.assertTrue(s.removeScenarioElement(a));
		Assert.assertNull(s.getScenarioElement(A.class));
		Assert.assertNull(s.getScenarioElement(AImpl.class));
		Assert.assertFalse(s.removeScenarioElement(a));
	}

	@Test
	public void testRemoveScenarioElement_complex() {
		Scenario s = new ScenarioImpl();

		B b = new BImpl();
		C c = new CImpl();

		s.addScenarioElement(b); // BImpl, B, AImpl, A

		s.addScenarioElement(c); // CImpl, C, B

		Assert.assertTrue(s.removeScenarioElement(c));
		Assert.assertNull(s.getScenarioElement(CImpl.class));
		Assert.assertNull(s.getScenarioElement(C.class));
		Assert.assertNull(s.getScenarioElement(B.class));
		Assert.assertEquals(b, s.getScenarioElement(BImpl.class));
		Assert.assertEquals(b, s.getScenarioElement(AImpl.class));
		Assert.assertEquals(b, s.getScenarioElement(A.class));

		s.addScenarioElement(c);

		Assert.assertTrue(s.removeScenarioElement(b));
		Assert.assertEquals(c, s.getScenarioElement(CImpl.class));
		Assert.assertEquals(c, s.getScenarioElement(C.class));
		Assert.assertEquals(c, s.getScenarioElement(B.class));
		Assert.assertNull(s.getScenarioElement(BImpl.class));
		Assert.assertNull(s.getScenarioElement(AImpl.class));
		Assert.assertNull(s.getScenarioElement(A.class));
	}

	/*package*/ interface A { }
	/*package*/ class AImpl implements A { }
	/*package*/ interface B { }
	/*package*/ class BImpl extends AImpl implements B { }
	/*package*/ interface C extends B { }
	/*package*/ class CImpl implements C { }

}
