/* *********************************************************************** *
 * project: org.matsim.*
 * Run.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.parknride;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import playground.thibautd.analysis.listeners.ActivityTypesAnalysis;
import playground.thibautd.analysis.listeners.ModeAnalysis;
import playground.thibautd.parknride.scoring.CenteredTimeProportionalPenaltyFactory;
import playground.thibautd.parknride.scoring.ParkAndRideScoringFunctionFactory;
import playground.thibautd.parknride.scoring.ParkingPenaltyFactory;

/**
 * Executable to run a simulation with park and ride
 * @author thibautd
 */
public class Run {
	private static final Coord center = new Coord((double) 0, (double) 0);
	private static final double radius = 100;
	private static final double costPerSecondAtCenter = 100;

	public static void main(final String[] args) {
		String configFile = args[0];

		Config config = ConfigUtils.createConfig();
		ParkAndRideUtils.setConfigGroup( config );
		ConfigUtils.loadConfig( config , configFile );

		Scenario scenario = ParkAndRideUtils.loadScenario( config );

		ParkingPenaltyFactory penalty =
			new CenteredTimeProportionalPenaltyFactory(
					center,
					radius,
					costPerSecondAtCenter);

		Controler controler = new Controler( scenario );
		controler.addControlerListener( ParkAndRideScoringFunctionFactory.createFactoryListener( penalty ) );
		controler.addControlerListener(new ActivityTypesAnalysis( true ));
		controler.addControlerListener(new ModeAnalysis( true ));
		controler.run();

		String outputFacilities =
			controler.getControlerIO().getOutputFilename( "output_pnrFacilities.xml.gz" );
		(new ParkAndRideFacilitiesXmlWriter( ParkAndRideUtils.getParkAndRideFacilities( scenario ) )).write( outputFacilities );
	}
}

