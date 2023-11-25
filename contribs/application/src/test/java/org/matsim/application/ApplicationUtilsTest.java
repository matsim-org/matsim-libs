package org.matsim.application;

import static org.junit.Assert.*;

import org.junit.Test;
import org.matsim.application.analysis.TestAnalysis;
import org.matsim.application.analysis.TestDependentAnalysis;
import org.matsim.application.options.ShpOptions;

public class ApplicationUtilsTest {

  @Test
  public void shp() {

    assertTrue(ApplicationUtils.acceptsOptions(TestAnalysis.class, ShpOptions.class));

    assertFalse(ApplicationUtils.acceptsOptions(TestDependentAnalysis.class, ShpOptions.class));
  }
}
