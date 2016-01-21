package org.matsim.contrib.matrixbasedptrouter;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.PtMatrixProvider;
import org.matsim.core.controler.AbstractModule;

public class MatrixBasedPtModule extends AbstractModule {

	@Override
	public void install() {
		if (getConfig().transit().isUseTransit()) {
			System.out.println("you try to use PseudoPtRoutingModule and physical transit simulation at the same time. This probably will not work!");
		}
		bind(PtMatrix.class).toProvider(PtMatrixProvider.class);
		addRoutingModuleBinding(TransportMode.pt).to(MatrixBasedPtRoutingModule.class);
	}
}
