package org.matsim.api.core.v01;

import org.junit.Assert;

import org.junit.Test;

public class IdTest {

	@Test
	public void testConstructor() {
		Id<TLink> linkId1 = Id.create("1", TLink.class);
		Id<TLink> linkId2 = Id.create("2", TLink.class);
		
		Assert.assertEquals("1", linkId1.toString());
		Assert.assertEquals("2", linkId2.toString());
	}

	@Test
	public void testIdConstructor() {
		Id<TNode> nodeId1 = Id.create("1", TNode.class);
		Id<TLink> linkId1 = Id.create(nodeId1, TLink.class);
		
		Assert.assertEquals("1", linkId1.toString());
	}
	
	@Test
	public void testIdConstructor_Null() {
		Id<TLink> linkId1 = Id.create((Id) null, TLink.class);
		Assert.assertNull(linkId1);
	}
	
	@Test
	public void testObjectIdentity_cache() {
		Id<TLink> linkId1 = Id.create("1", TLink.class);
		Id<TLink> linkId2 = Id.create("2", TLink.class);
		Id<TLink> linkId1again = Id.create("1", TLink.class);
		
		Assert.assertTrue(linkId1 == linkId1again);
		Assert.assertFalse(linkId1 == linkId2);
	}
	
	@Test
	public void testObjectIdentity_types() {
		Id<TLink> linkId1 = Id.create("1", TLink.class);
		Id<TNode> nodeId1 = Id.create("1", TNode.class);
		
		Assert.assertFalse((Id) linkId1 == (Id) nodeId1);
	}
	
	@Test
	public void testCompareTo() {
		Id<TLink> linkId1 = Id.create("1", TLink.class);
		Id<TLink> linkId2 = Id.create("2", TLink.class);
		Id<TLink> linkId1again = Id.create("1", TLink.class);
		Id<TNode> nodeId1 = Id.create("1", TNode.class);
		
		Assert.assertTrue(linkId1.compareTo(linkId2) < 0);
		Assert.assertTrue(linkId1.compareTo(linkId1) == 0);
		Assert.assertTrue(linkId1.compareTo(linkId1again) == 0);
		Assert.assertTrue(linkId2.compareTo(linkId1) > 0);
		
//		try {
//			Assert.assertTrue(linkId1.compareTo((Id) nodeId1) == 0);
//			Assert.fail("expected exception, got none");
//		} catch (IllegalArgumentException e) {
//			// expected exception
//		} // FIXME temporarily deactivated
	}
	
	private static class TLink {}
	private static class TNode {}
	
}
