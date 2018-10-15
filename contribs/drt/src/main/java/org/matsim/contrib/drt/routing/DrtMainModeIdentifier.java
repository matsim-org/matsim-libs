package org.matsim.contrib.drt.routing;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;

import com.google.inject.Inject;

public class DrtMainModeIdentifier implements MainModeIdentifier{

	private final MainModeIdentifier delegate = new MainModeIdentifierImpl();
	private final String mode;
	private final DrtStageActivityType drtStageActivityType;
	
	@Inject
	public DrtMainModeIdentifier(DrtConfigGroup drtCfg) {
		mode = drtCfg.getMode();
		drtStageActivityType = new DrtStageActivityType(drtCfg.getMode());
	}
	
	@Override
	public String identifyMainMode(List<? extends PlanElement> tripElements) {
		for (PlanElement pe : tripElements) {
			if (pe instanceof Activity) {
				if (((Activity) pe).getType().equals(drtStageActivityType.drtStageActivity))
				return mode;
			}
		}
		
		return delegate.identifyMainMode(tripElements);
	}

}
