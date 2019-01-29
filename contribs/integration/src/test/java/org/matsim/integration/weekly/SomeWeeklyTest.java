package org.matsim.integration.weekly;

import org.junit.Assert;
import org.junit.Test;

public class SomeWeeklyTest {
	@Test
	public void doTest() {
		System.out.println("RUN TEST WEEKLY");
		System.out.println("available ram: " + (Runtime.getRuntime().maxMemory() / 1024/1024));
		Assert.assertTrue(true);
	}
}
