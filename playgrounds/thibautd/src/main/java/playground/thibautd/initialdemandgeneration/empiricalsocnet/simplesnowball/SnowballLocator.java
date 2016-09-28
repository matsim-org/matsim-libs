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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.simplesnowball;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.PersonUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.EgoLocator;

import static playground.meisterk.PersonAnalyseTimesByActivityType.Activities.e;
import static playground.thibautd.initialdemandgeneration.empiricalsocnet.simplesnowball.SimpleCliquesFiller.getSex;

/**
 * @author thibautd
 */
@Singleton
public class SnowballLocator implements EgoLocator, SimpleCliquesFiller.Position {
	@Override
	public int getDimensionality() {
		return 4;
	}

	@Override
	public double[] getCoord( final Ego ego ) {
		final Activity firstActivity =
				ego.getPerson().getSelectedPlan().getPlanElements().stream()
						.filter( pe -> pe instanceof Activity )
						.map( pe -> (Activity) pe )
						.findFirst()
						.get();

		return new double[]{
				firstActivity.getCoord().getX() ,
				firstActivity.getCoord().getY() ,
				SimpleCliquesFiller.calcAgeClass( PersonUtils.getAge( ego.getPerson() ) ) ,
				SimpleCliquesFiller.getSex( ego ).ordinal() };
	}

	@Override
	public double[] calcPosition( final Ego center, final SimpleCliquesFiller.CliquePosition position ) {
		final double[] egoCoord = getCoord( center );

		final double xTranslation = Math.cos( position.getBearing() ) * position.getDistance();
		final double yTranslation = Math.sin( position.getBearing() ) * position.getDistance();

		return new double[]{
				egoCoord[ 0 ] + xTranslation ,
				egoCoord[ 1 ] + yTranslation ,
				// can go out of bounds, in which case the closest is the first or last class.
				// would be better to manage to get something "at a distance", whatever the direction...
				egoCoord[ 2 ] + position.getAgeClassDistance() ,
				position.isSameSex() ? egoCoord[ 3 ] : 1 - egoCoord[ 3 ] };
	}
}
