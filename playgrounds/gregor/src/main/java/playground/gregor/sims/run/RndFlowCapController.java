/* *********************************************************************** *
 * project: org.matsim.*
 * RndFlowCapController.java
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
package playground.gregor.sims.run;

import org.matsim.core.controler.Controler;

import playground.gregor.sims.socialcostII.LinkFlowCapRandomizer;


public class RndFlowCapController extends Controler {

	private double c;

	public RndFlowCapController(String[] args, double c) {
		super(args);
		this.c = c;
	}
	
	

	@Override
	protected void setUp() {
		super.setUp();
		LinkFlowCapRandomizer lr = new LinkFlowCapRandomizer(this.network,c,0.1);
		this.addControlerListener(lr);
	}



	public static void main(String [] args ) {
		double c = Double.parseDouble(args[1]);
		Controler controller = new RndFlowCapController(args, c);
		controller.setOverwriteFiles(true);
		controller.run();
		System.exit(0);
	}
}
