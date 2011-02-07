/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.scoring;

import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;


public class Main {
	public static void main(String[] args) {
		Controler controler = new Controler(args);
		CharyparNagelScoringFunctionFactory factory = new CharyparNagelScoringFunctionFactory(controler.getConfig().planCalcScore());
		controler.setScoringFunctionFactory(factory);
		controler.setOverwriteFiles(true);
		controler.run();
	}
}
