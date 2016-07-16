package org.matsim.contrib;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;

import javax.inject.Inject;
import javax.inject.Provider;

public class PtMatrixProvider implements Provider<PtMatrix> {

	@Inject private PlansCalcRouteConfigGroup plansCalcRoute;
	@Inject private MatrixBasedPtRouterConfigGroup ippcm;
	@Inject private Network network;

	@Override
	public PtMatrix get() {
		return PtMatrix.createPtMatrix(plansCalcRoute, BoundingBox.createBoundingBox(network), ippcm);
	}

}
