/**
 * 
 */
package org.matsim.codeexamples.extensions.dvrp;

import java.io.File;
import java.net.URL;

/**
 * @author kainagel
 *
 */
public class RunTaxiPTIntermodalExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		URL configUrl = null ; // there is a regression test in the contrib; the input file is in the test input directory. kai, jul'19
		new org.matsim.contrib.av.intermodal.RunTaxiPTIntermodalExample().run(configUrl, false );
	}

}
