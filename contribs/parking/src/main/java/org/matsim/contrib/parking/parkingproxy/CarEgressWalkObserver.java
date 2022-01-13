/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.parking.parkingproxy;

import java.io.File;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

/**
 * Before the mobsim, gets the current {@linkplain PenaltyCalculator} from the provided {@linkplain PenaltyGenerator} and
 * resets that latter so it can - theoretically - gather information during the new iteration. However, it is your own
 * responsibility to feed it with data. The penalty for each space-time-gridcell with non-zero penalty is dumped for each
 * iteration. In the zeroth iteration, {@linkplain PenaltyCalculator#getDummyHourCalculator()} is used to calculate the penalties.
 * 
 * @author tkohl / Senozon
 *
 */
class CarEgressWalkObserver implements BeforeMobsimListener {
	
//	private static final String INSERTIONKEY = "[INSERTIONKEY]";
	public static final String OUTFILE_PENALTIES = "penalties.csv.gz";
	public static final String CARMODE = TransportMode.car;
	
	private final PenaltyGenerator penaltyGenerator;
	private final PenaltyFunction penaltyFunction;
	private final PenaltyCalculator iter0PenaltyCalculator;
	
	private PenaltyCalculator penaltyCalculator;
	
	/**
	 * Sets the class up with the {@linkplain PenaltyCalculator.DefaultPenaltyFunction} and the specified {@linkplain PenaltyGenerator}.
	 * 
	 * @param penaltyGenerator
	 */
	public CarEgressWalkObserver(PenaltyGenerator penaltyGenerator, PenaltyFunction penaltyFunction, PenaltyCalculator iter0PenaltyCalculator) {
		this.penaltyGenerator = penaltyGenerator;
		this.penaltyFunction = penaltyFunction;
		this.iter0PenaltyCalculator = iter0PenaltyCalculator;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		// update the Penalties to the result of the last iteration
		if (event.getIteration() == 0) {
			this.penaltyCalculator = iter0PenaltyCalculator;
		} else {
			this.penaltyCalculator = this.penaltyGenerator.generatePenaltyCalculator();
			this.penaltyCalculator.setPenaltyFunction(this.penaltyFunction);
			this.penaltyGenerator.reset();
		}
		event.getServices().getControlerIO().createIterationDirectory(event.getIteration());
		String file = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), OUTFILE_PENALTIES);
		this.penaltyCalculator.dump(new File(file));
	}
	
	/*package*/ PenaltyCalculator getPenaltyCalculator() {
		return this.penaltyCalculator;
	}

}
