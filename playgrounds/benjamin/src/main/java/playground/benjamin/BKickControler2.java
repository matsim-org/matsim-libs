/* *********************************************************************** *
 * project: org.matsim.*
 * BKickControler2
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.benjamin;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunctionFactory;


/**
 * Controler for first zurich scenario test run of estimated scoring function.
 * @author dgrether
 *
 */
public class BKickControler2 extends BKickControler {

	public BKickControler2(String configFileName) {
		super(configFileName);
	}
	
	public BKickControler2(Config conf){
		super(conf);
	}

	public BKickControler2(String[] args) {
		super(args);
	}

	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		return new BKickScoringFunctionFactory(this.config.charyparNagelScoring());
	}

	
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new BKickControler2(args);
			controler.run();
		}
		System.exit(0);
	}

}
