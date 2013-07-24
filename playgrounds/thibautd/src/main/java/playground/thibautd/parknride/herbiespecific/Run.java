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
package playground.thibautd.parknride.herbiespecific;

import herbie.running.config.HerbieConfigGroup;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.thibautd.analysis.listeners.ActivityTypesAnalysis;
import playground.thibautd.analysis.listeners.ModeAnalysis;
import playground.thibautd.parknride.ParkAndRideFacilitiesXmlWriter;
import playground.thibautd.parknride.ParkAndRideUtils;
import playground.thibautd.parknride.scoring.CenteredTimeProportionalPenaltyFactory;
import playground.thibautd.parknride.scoring.ParkingPenaltyFactory;

/**
 * @author thibautd
 */
public class Run {
	private static final Coord center = RelevantCoordinates.HAUPTBAHNHOF;
	private static final Coord boundaryPoint = RelevantCoordinates.SEEBACH;
	// cost at parkaus hauptbahnof
	private static final double costPerSecondAtCenter = 4.4 / 3600d;

	public static void main(final String[] args) {
		String configFile = args[0];
		double personalizedCostAtCenter = args.length > 1 ? Double.parseDouble( args[ 1 ] ) : costPerSecondAtCenter;

		Config config = ConfigUtils.createConfig();
		ParkAndRideUtils.setConfigGroup( config );
		config.addModule( new HerbieConfigGroup() );
		ConfigUtils.loadConfig( config , configFile );

		Scenario scenario = ParkAndRideUtils.loadScenario( config );

		ParkingPenaltyFactory penalty =
			new CenteredTimeProportionalPenaltyFactory(
					center,
					CoordUtils.calcDistance( center , boundaryPoint ),
					personalizedCostAtCenter * config.planCalcScore().getMarginalUtilityOfMoney());

		UglyHerbieMultilegControler controler = new UglyHerbieMultilegControler( scenario );
		controler.setParkingPenaltyFactory( penalty );
		//controler.addControlerListener( ParkAndRideScoringFunctionFactory.createFactoryListener( penalty ) );
		controler.addControlerListener(new ModeAnalysis( true ));
		controler.addControlerListener(new ActivityTypesAnalysis( true ));
		controler.run();

		String outputFacilities =
			controler.getControlerIO().getOutputFilename( "output_pnrFacilities.xml.gz" );
		(new ParkAndRideFacilitiesXmlWriter( ParkAndRideUtils.getParkAndRideFacilities( scenario ) )).write( outputFacilities );
	}
}

