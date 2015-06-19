package playground.pieter.singapore.hits;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.pt.config.TransitRouterConfigGroup;
import org.matsim.pt.router.TransitRouterConfig;

class MyTransitRouterConfig extends TransitRouterConfig {

	public MyTransitRouterConfig(Config config) {
		super(config);
		// TODO Auto-generated constructor stub
	}

	public MyTransitRouterConfig(PlanCalcScoreConfigGroup planCalcScore,
			PlansCalcRouteConfigGroup plansCalcRoute,
			TransitRouterConfigGroup transitRouter,
			VspExperimentalConfigGroup vspExperimental) {
		super(planCalcScore, plansCalcRoute, transitRouter, vspExperimental);
	}

	public void setSearchradius(double radius) {
		this.setSearchRadius(radius);
	}
}
