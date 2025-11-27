package org.matsim.simwrapper;

import org.junit.jupiter.api.Assertions;
import org.matsim.core.config.Config;

import java.util.List;

public class TestDashboardProvider implements DashboardProvider {
	private int counter = 0;

	@Override
	public List<Dashboard> getDashboards(Config config, SimWrapper simWrapper) {
		counter++;
		return List.of(new TestDashboard());
	}

	public void assertCalled() {
		Assertions.assertEquals(counter, 1);
	}
}
