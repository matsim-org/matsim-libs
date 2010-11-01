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
package playground.benjamin.income;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.listener.ControlerListener;

import playground.benjamin.income.BkIncomeControlerListener;

/**
 * @author kai
 * @author benjamin
 */
public class BkIncomeControler extends Controler{

	
	private static Config config;

	public BkIncomeControler(Config config) {
		super(config);
	}

	public static void main(String[] args) {

		final Controler controler = new Controler(config);

		controler.setOverwriteFiles(true);
		
		ControlerListener listener = new BkIncomeControlerListener() ;
		
		controler.addControlerListener(listener) ;
		
		controler.run();
	}

}


