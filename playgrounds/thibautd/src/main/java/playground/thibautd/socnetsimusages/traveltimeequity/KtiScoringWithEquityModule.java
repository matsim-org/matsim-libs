/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsimusages.traveltimeequity;

import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.pt.PtConstants;
import playground.ivt.analysis.scoretracking.ScoreTrackingModule;
import playground.thibautd.socnetsimusages.scoring.KtiScoringFunctionFactoryWithJointModesAndEquity;

/**
 * @author thibautd
 */
public class KtiScoringWithEquityModule extends AbstractModule {
	@Override
	public void install() {
		install( new ScoreTrackingModule() );

		binder().bind( TravelTimesRecord.class ).toInstance(
				new TravelTimesRecord(
						new StageActivityTypesImpl(
								PtConstants.TRANSIT_ACTIVITY_TYPE,
								JointActingTypes.INTERACTION
						)) );
		addEventHandlerBinding().to( TravelTimesRecord.class );
		binder().bind(ScoringFunctionFactory.class).to(KtiScoringFunctionFactoryWithJointModesAndEquity.class);
	}
}
