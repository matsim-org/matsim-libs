package org.matsim.simwrapper;

public class TestDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout, SimWrapperConfigGroup configGroup) {
		header.title = "Test";
		header.description = "Test";
	}
}

