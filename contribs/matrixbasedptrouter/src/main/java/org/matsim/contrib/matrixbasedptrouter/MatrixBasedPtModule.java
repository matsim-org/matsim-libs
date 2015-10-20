package org.matsim.contrib.matrixbasedptrouter;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;

public class MatrixBasedPtModule extends AbstractModule {
	private final PtMatrix ptMatrix;

	public MatrixBasedPtModule(PtMatrix ptMatrix) {
		this.ptMatrix = ptMatrix;
	}

	@Override
	public void install() {
		if (getConfig().transit().isUseTransit()) {
			System.out.println("you try to use PseudoPtRoutingModule and physical transit simulation at the same time. This probably will not work!");
		}
		bind(PtMatrix.class).toInstance(ptMatrix);
		addRoutingModuleBinding(TransportMode.pt).to(MatrixBasedPtRoutingModule.class);
	}
}
