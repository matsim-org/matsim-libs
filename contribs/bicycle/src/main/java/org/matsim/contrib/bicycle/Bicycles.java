package org.matsim.contrib.bicycle;

import org.matsim.core.controler.AllowsConfiguration;

public class Bicycles {

	public static void addAsOverridingModule(AllowsConfiguration ao) {

		ao.addOverridingModule(new BicycleModule());
		ao.addOverridingQSimModule(new BicycleQSimModule());
	}
}
