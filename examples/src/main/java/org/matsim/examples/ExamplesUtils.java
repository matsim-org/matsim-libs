package org.matsim.examples;

import java.net.URL;

public class ExamplesUtils {
	/**
	 * @param name of scenario
	 * @return directory URL pointing to scenario in the "scenarios" directory in the "examples" project
	 * (where this method is located).  Note that this works because it is entered as such in the pom.xml,
	 * so it is a bit of magic.
	 */
	public static URL getTestScenarioURL(String name) {
		final URL resource = ExamplesUtils.class.getResource( "/test/scenarios/" + name + "/" );
		// note that this works because it is entered as such in the pom.xml.  kai, sep'17
		if ( resource==null ) {
			throw new RuntimeException( "could not find test scenario with name=" + name ) ;
		}
		return resource;
	}
}
