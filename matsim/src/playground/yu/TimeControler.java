/* *********************************************************************** *
 * project: org.matsim.*
 * TimeControler.java
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

/*
 * $Id: TimeControler.java,v 1.5 2007/11/23 13:04:04 ychen Exp $
 */

/* *********************************************************************** *
 *                                                                         *
 *                            TimeControler.java                            *
 *                          ---------------------                          *
 * copyright       : (C) 2007 by Michael Balmer, Marcel Rieser,            *
 *                   David Strippgen, Gunnar Flötteröd, Konrad Meister,    *
 *                   Kai Nagel, Kay W. Axhausen                            *
 *                   Technische Universitaet Berlin (TU-Berlin) and        *
 *                   Swiss Federal Institute of Technology Zurich (ETHZ)   *
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

import org.matsim.controler.Controler;

/**
 * test of TimeWriter and BottleneckTravol
 * @author ychen
 *
 */
public class TimeControler extends Controler {

	private TimeWriter timeWriter;
	private BottleneckTraVol bTV;

	/**
	 * 
	 */
	public TimeControler() {
		super();
		timeWriter = new TimeWriter("./test/input/org/matsim/events/algorithms/Bottleneck/timeEvents.txt");
		bTV=new BottleneckTraVol("./test/input/org/matsim/events/algorithms/Bottleneck/bottleneckTraVol.txt");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.demandmodeling.controler.Controler#setupIteration(int)
	 */
	@Override
	protected void setupIteration(int iteration) {
		super.setupIteration(iteration);
		if (iteration == 500){
			this.events.addHandler(timeWriter);
			this.events.addHandler(bTV);
		}
	}

	public static void main(String[] args) {
		final TimeControler ctl = new TimeControler();
		System.out.println(args);
		ctl.run(args);
		System.out.println(args);
		ctl.timeWriter.closefile();
		ctl.bTV.closefile();
		System.exit(0);
	}
}
