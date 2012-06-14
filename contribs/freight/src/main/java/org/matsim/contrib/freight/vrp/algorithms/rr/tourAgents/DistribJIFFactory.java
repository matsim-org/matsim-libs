/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public class DistribJIFFactory implements JobInsertionFinderFactory{

	private MCCalculatorFactory mcCalculatorFactory;
	
	public DistribJIFFactory(MCCalculatorFactory mcCalculatorFactory) {
		super();
		this.mcCalculatorFactory = mcCalculatorFactory;
	}

	@Override
	public JobInsertionFinder createFinder(Costs costs, Vehicle vehicle, Tour tour) {
		DistribJIF distribJIF = new DistribJIF(costs, vehicle, tour);
		distribJIF.setMcCalculatorFactory(mcCalculatorFactory);
		return distribJIF;
	}

}
