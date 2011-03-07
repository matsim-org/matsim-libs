/* *********************************************************************** *
 * project: org.matsim.*
 * LCControler.java
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

package playground.anhorni.scenarios;

import org.matsim.core.controler.Controler;

import playground.anhorni.scenarios.analysis.ShoppingCalculator;

public class SingleRunControler extends Controler {
		
	public SingleRunControler(final String[] args) {
		super(args);	
	}
	
    public static void main (final String[] args) { 
    	SingleRunControler controler = new SingleRunControler(args);
    	controler.run();
    }
    
    public void run() {
    	super.setOverwriteFiles(true);
    	super.addControlerListener(new ShoppingCalculator());
    	super.run();
    }
}
