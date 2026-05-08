/**
 * 
 */
package org.matsim.codeexamples.extensions.dvrp;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;

/**
 * @author kainagel
 *
 */
public class RunRobotaxiExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		URL configUrl = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL("mielec" ), "mielec_taxi_config.xml" );
		org.matsim.contrib.av.robotaxi.run.RunRobotaxiExample.run(configUrl, false );
	}

}
