package org.matsim.api.core.v01;

import junit.framework.Assert;

import org.junit.Test;

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
}
