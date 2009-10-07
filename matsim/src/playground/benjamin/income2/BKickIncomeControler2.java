/* *********************************************************************** *
 * project: org.matsim.*
 * BKickIncomeControler2
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.income2;

import org.matsim.core.controler.Controler;

import playground.benjamin.income1.BKickIncomeControler;


/**
 * @author dgrether
 *
 */
public class BKickIncomeControler2 extends BKickIncomeControler {

	public BKickIncomeControler2(String[] args) {
		super(args);
	}

	
	public static void main(String[] args) {
//	String config = DgPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/config.xml"; //can also be included in runConfigurations/arguments/programArguments
//	String[] args2 = {config};
//	args = args2;
	if ((args == null) || (args.length == 0)) {
		System.out.println("No argument given!");
		System.out.println("Usage: Controler config-file [dtd-file]");
		System.out.println();
	} else {
		final Controler controler = new BKickIncomeControler(args);
		controler.run();
	}
}
	
}
