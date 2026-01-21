package org.matsim.simwrapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SimWrapperUtilsTest {
	@Test
	void testAddAndHasDashboard() {
		SimWrapper simWrapper = SimWrapper.create();
		simWrapper.addDashboard(new TestDashboard());
		Assertions.assertTrue(simWrapper.hasDashboard(TestDashboard.class, ""));
	}

	@Test
	void testReplaceDashboard() {
		SimWrapper simWrapper = SimWrapper.create();

		TestDashboard testDashboard = new TestDashboard();
		Dashboard testDashboard2 = Dashboard.customize(new TestDashboard()).context("replace");

		simWrapper.addDashboard(testDashboard);
		simWrapper.replaceDashboard(testDashboard, testDashboard2);
		Assertions.assertTrue(simWrapper.hasDashboard(testDashboard2.getClass(), "replace"));
	}
}
