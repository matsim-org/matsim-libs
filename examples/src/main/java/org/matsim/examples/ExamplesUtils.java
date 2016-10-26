package org.matsim.examples;

import java.net.URL;

public class ExamplesUtils {
    public static URL getTestScenarioURL(String name) {
        return ExamplesUtils.class.getResource("/test/scenarios/"+ name +"/");
    }
}
