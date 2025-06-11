package org.matsim.integration.daily;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SomeDailyTest {

	@Test
	void doTest() {
		System.out.println("RUN TEST DAILY");
		System.out.println("available ram: " + (Runtime.getRuntime().maxMemory() / 1024/1024));

		Assertions.assertTrue(true);
	}

}
