/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.usability;

import java.io.PrintWriter;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.michalm.taxi.util.stats.*;

/**
 * @author  jbischoff
 *
 */
public class TaxiStatsControlerListener implements IterationEndsListener {

	
	
	private final MatsimVrpContext context;
	private final TaxiConfigGroup tcg;

	public TaxiStatsControlerListener(MatsimVrpContext context, TaxiConfigGroup tcg) {
		this.tcg = tcg;
		this.context = context;
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		 PrintWriter pw = new PrintWriter(System.out);
	        pw.println(tcg.getAlgorithmConfig());
	        pw.println("m\t" + context.getVrpData().getVehicles().size());
	        pw.println("n\t" + context.getVrpData().getRequests().size());
	        pw.println(TaxiStats.HEADER);
	        TaxiStats stats = new TaxiStatsCalculator(context.getVrpData().getVehicles().values()).getStats();
	        pw.println(stats);
	        pw.flush();
	}

}
