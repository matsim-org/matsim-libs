/* *********************************************************************** *
 * project: org.matsim.*
 * BseControlerRWPutilityCorrection.java
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

/**
 * 
 */
package playground.dziemke.cadyts.other;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;

/**
 * @author yu
 * 
 */
public class BseUCControler extends Controler {
	// ControlerListener
	private final BseUCControlerListener extension;
	private boolean writeQGISFile = false;

	public BseUCControler(final String[] args) {
		super(args);
		// ControlerListener Constructor
		extension = new BseUCControlerListener();
		extension.setWriteQGISFile(writeQGISFile);
		addControlerListener(extension);	
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		// StrategyManager Constructor
		StrategyManager manager = new BseUCStrategyManager(network, getFirstIteration());
		StrategyManagerConfigLoader.load(this, manager);
		return manager;
	}

	public void setWriteQGISFile(final boolean writeQGISFile) {
		this.writeQGISFile = writeQGISFile;
	}

	/** @param args */
	public static void main(final String[] args) {
		// Controller Constructor
		BseUCControler ctl = new BseUCControler(args);
		// ctl.setCreateGraphs(false);
		
		// switched off before run_16
		// ctl.setOverwriteFiles(true);
		
		ctl.getConfig().controler().setWriteEventsInterval(100);
		// ctl.setWriteQGISFile(false);
		
		// 2013-04-21, switching off scoring
		ZeroScoringFunctionFactory zeroScoringFunctionFactory = new ZeroScoringFunctionFactory();
		ctl.setScoringFunctionFactory(zeroScoringFunctionFactory);
		//	
		
		// new, testing 2013-03-14
		// so far travelTimes have been null
		ctl.setUp();
		//
		
		ctl.run();
	}
}
