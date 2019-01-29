package org.matsim.contrib.matrixbasedptrouter;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;

public class MatrixBasedPtModule extends AbstractModule {

	@Override
	public void install() {
		if (getConfig().transit().isUseTransit()) {
			System.out.println("You are trying to use MatrixBasedPtModule and physical transit simulation "
					+ "at the same time. This probably will not work!");
		}
		addRoutingModuleBinding(TransportMode.pt).to(MatrixBasedPtRoutingModule.class);
	}
	
	@Provides
	PtMatrix createPtMatrix(PlansCalcRouteConfigGroup plansCalcRoute, MatrixBasedPtRouterConfigGroup ippcm, Network network) {
		return PtMatrix.createPtMatrix(plansCalcRoute, BoundingBox.createBoundingBox(network), ippcm);
	}
}