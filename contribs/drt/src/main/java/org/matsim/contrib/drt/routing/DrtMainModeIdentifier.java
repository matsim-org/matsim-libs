package org.matsim.contrib.drt.routing;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;

import com.google.inject.Inject;

public class DrtMainModeIdentifier implements MainModeIdentifier{

	private final MainModeIdentifier delegate = new MainModeIdentifierImpl();
	private final String mode;
	@Inject
	public DrtMainModeIdentifier(Config config) {
		mode = DvrpConfigGroup.get(config).getMode();
	}
	
	@Override
	public String identifyMainMode(List<? extends PlanElement> tripElements) {
		for (PlanElement pe : tripElements) {
			if (pe instanceof Activity) {
				if (((Activity) pe).getType().equals(DrtStageActivityType.DRT_STAGE_ACTIVITY))
				return mode;
			}
		}
		
		return delegate.identifyMainMode(tripElements);
	}

}
