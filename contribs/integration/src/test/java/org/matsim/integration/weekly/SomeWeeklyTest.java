package org.matsim.integration.weekly;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SomeWeeklyTest {
	@Test
	void doTest() {
		System.out.println("RUN TEST WEEKLY");
		System.out.println("available ram: " + (Runtime.getRuntime().maxMemory() / 1024/1024));
		Assertions.assertTrue(true);
	}
}
