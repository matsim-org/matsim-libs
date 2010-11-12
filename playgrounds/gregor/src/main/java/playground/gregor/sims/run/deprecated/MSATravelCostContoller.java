/* *********************************************************************** *
 * project: org.matsim.*
 * MSATravelCostContoller.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sims.run.deprecated;

import org.matsim.core.controler.Controler;
import org.matsim.evacuation.run.EvacuationQSimControllerII;

import playground.gregor.sims.msa.MSATravelTimeCalculatorFactory;

@Deprecated
public class MSATravelCostContoller extends EvacuationQSimControllerII {

	public MSATravelCostContoller(String[] args) {
		super(args);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void setUp() {
		setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
		super.setUp();

	}

	public static void main(final String[] args) {
		final Controler controler = new MSATravelCostContoller(args);
		controler.run();
		System.exit(0);
	}
}
