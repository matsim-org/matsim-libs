package org.matsim.examples;

import java.net.URL;

public class ExamplesUtils {
    public static URL getTestScenarioURL(String name) {
        return ExamplesUtils.class.getResource("/test/scenarios/"+ name +"/");
        // note that this works because it is entered as such in the pom.xml.  kai, sep'17
    }
}
