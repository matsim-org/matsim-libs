package org.matsim.application;

import org.junit.Test;
import org.matsim.application.analysis.TestAnalysis;
import org.matsim.application.analysis.TestDependentAnalysis;

import static org.junit.Assert.*;

public class ApplicationUtilsTest {

	@Test
	public void shp() {

		assertTrue(ApplicationUtils.acceptsShpFile(TestAnalysis.class));

		assertFalse(ApplicationUtils.acceptsShpFile(TestDependentAnalysis.class));

	}
}
