package org.matsim.simwrapper;

public class TestDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {
		header.title = "Test";
		header.description = "Test";
	}
}

