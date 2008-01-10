/* *********************************************************************** *
 * project: org.matsim.*
 * NewPtcheckControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.yu;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.controler.Controler;

/**
 * test of PtCheck
 * 
 * @author ychen
 * 
 */
public class NewPtcheckControler extends Controler {

	/*
	 * @Override // protected void finishIteration(final int iteration) { //
	 * super.finishIteration(iteration); // try { // if (iteration ==
	 * super.getMaximumIteration()) { // check.resetCnt(); //
	 * check.run(population); // check.write(iteration); // check.writeEnd(); // } // }
	 * catch (IOException e) { // e.printStackTrace(); // } // // }
	 */

	@Override
	protected void loadData() {
		super.loadData();
		try {
			addControlerListener(new PtRate(population, Controler
					.getOutputFilename("PtRate.txt"), getMaximumIteration(),
					config.getParam("planCalcScore", "traveling"), config
							.getParam("planCalcScore", "travelingPt")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		final NewPtcheckControler controler;
		controler = new NewPtcheckControler();
		controler.run(args);
		System.exit(0);
	}
}
