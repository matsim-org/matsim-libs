package org.matsim.contrib.roadpricing;

import org.matsim.core.controler.AllowsConfiguration;

public final class RoadPricing{
	private RoadPricing(){} // do not instantiate
	
	public static void configure( AllowsConfiguration ao ) {
		ao.addOverridingModule( new RoadPricingModule() );
	}
}
