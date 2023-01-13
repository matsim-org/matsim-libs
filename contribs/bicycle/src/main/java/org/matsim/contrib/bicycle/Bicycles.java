package org.matsim.contrib.bicycle;

import org.matsim.core.controler.AllowsConfiguration;

/**
 * @deprecated -- please inline.  kai, dec'22
 */
public class Bicycles {

	/**
	 * @deprecated -- please inline.  kai, dec'22
	 */
	public static void addAsOverridingModule(AllowsConfiguration ao) {
		ao.addOverridingModule(new BicycleModule());
	}
}
