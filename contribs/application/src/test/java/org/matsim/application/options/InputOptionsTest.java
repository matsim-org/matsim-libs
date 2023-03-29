package org.matsim.application.options;

import org.junit.Test;
import org.matsim.application.analysis.TestAnalysis;

public class InputOptionsTest {

	@Test
	public void test() {

		new TestAnalysis().execute("--help");

	}
}
