package playground.florian;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for Dom4j");
		//$JUnit-BEGIN$
		suite.addTestSuite(Dom4jXmlTransformationTest.class);
		suite.addTestSuite(XmlPngOutputTest.class);
		//$JUnit-END$
		return suite;
	}
}

