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

import java.io.IOException;

import org.matsim.controler.Controler;
import org.matsim.plans.Plans;

/**
 * test of PtCheck
 * @author ychen
 *
 */
public class NewPtcheckControler extends Controler {
	private PtCheck check;

	/**
	 * @throws IOException
	 * 
	 */
	public NewPtcheckControler(String fileName) throws IOException {
		super();
		check = new PtCheck(fileName);
	}

	protected void finishIteration(final int iteration) {
		super.finishIteration(iteration);
		Plans population = this.population;
		check.resetCnt();
		check.run(population);
		try {
			if (iteration == 100) {
				check.write(iteration);
				check.writeEnd();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(final String[] args) {
		final NewPtcheckControler controler;
		try {
			controler = new NewPtcheckControler(
					"./test/yu/200PtRateCap100MU_Pt-4.txt");
			controler.run(args);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}
