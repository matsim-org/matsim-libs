/**
 * 
 */
package org.matsim.codeexamples.extensions.dvrp;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;

/**
 * Simple "empty" example to plug in your own taxi dispatcher.
 * <br/>
 * This is a pointer to example code in the contrib.
 * 
 * @author kainagel
 */
public class RunTaxiExample {

	public static void main(String[] args) {
		URL configUrl = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL("mielec" ), "mielec_taxi_config.xml" );
		org.matsim.contrib.taxi.run.examples.RunTaxiExample.run(configUrl, false, 0 );
	}

}
