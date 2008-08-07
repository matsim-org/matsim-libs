/* *********************************************************************** *
 * project: org.matsim.*
 * KtiControler.java
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

package playground.marcel.kti;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.controler.Controler;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.visum.VisumMatrixReader;

import playground.marcel.kti.router.PlansCalcRouteKti;
import playground.marcel.kti.router.SwissHaltestellen;

/**
 * A special controler for the KTI-Project.
 * <br>
 * Changes to the regular Controler:
 * <ul>
 * <li>Make use of a special Routing Module for finding (more or less) realistic public transit travel times.</li>
 * </ul>.
 * Requires additional configuration parameters:
 * <ul>
 * <li>config-group "kti" with the following parameters:
 * 	<ul>
 * 	<li>pt_traveltime_matrix: the path to the file with the travel-time matrix (VISUM-format).</li>
 * 	<li>pt_haltestellen: the path to the file containing the list of all pt-stops and their coordinates.</li>
 * 	</ul>
 * </li>
 * <li>A world file containing a layer 'municipality'</li>
 * </ul>
 *
 * @author mrieser
 */
public class KtiControler extends Controler {

	private PreProcessLandmarks commonRoutingData = null;
	private Matrix ptTravelTimes = null;
	private SwissHaltestellen haltestellen = null;
	
	public KtiControler(String[] args) {
		super(args);
	}

	@Override
	protected void setup() {
		super.setup();
		
		// read additional data for the special pt-router
		VisumMatrixReader reader = new VisumMatrixReader("pt_traveltime", getWorld().getLayer("municipality"));
		reader.readFile(this.config.getModule("kti").getValue("pt_traveltime_matrix"));
		this.ptTravelTimes = Matrices.getSingleton().getMatrix("pt_traveltime");
		this.haltestellen = new SwissHaltestellen(getNetwork());
		try {
			haltestellen.readFile(this.config.getModule("kti").getValue("pt_haltestellen"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public PlanAlgorithm getRoutingAlgorithm(final TravelCostI travelCosts, final TravelTimeI travelTimes) {
		synchronized (this) {
			if (this.commonRoutingData == null) {
				this.commonRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
				this.commonRoutingData.run(this.network);
			}
		}

		return new PlansCalcRouteKti(this.network, this.commonRoutingData, travelCosts, travelTimes, ptTravelTimes, haltestellen, getWorld().getLayer("municipality"));
	}
	
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: KtiControler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new KtiControler(args);
			controler.run();
		}
		System.exit(0);
	}	
}
