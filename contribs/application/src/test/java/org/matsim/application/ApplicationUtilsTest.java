package org.matsim.application;

import org.junit.jupiter.api.Test;
import org.matsim.application.analysis.TestAnalysis;
import org.matsim.application.analysis.TestDependentAnalysis;
import org.matsim.application.options.ShpOptions;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationUtilsTest {

	@Test
	void shp() {

		assertTrue(ApplicationUtils.acceptsOptions(TestAnalysis.class, ShpOptions.class));

		assertFalse(ApplicationUtils.acceptsOptions(TestDependentAnalysis.class, ShpOptions.class));

	}
}
