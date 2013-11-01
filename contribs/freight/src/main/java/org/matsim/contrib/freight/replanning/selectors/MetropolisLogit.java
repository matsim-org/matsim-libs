/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.contrib.freight.replanning.selectors;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.core.gbl.MatsimRandom;

/**
 * @author nagel
 *
 */
public class MetropolisLogit implements CarrierPlanSelector {

	private double beta = 1. ;
	private double alpha = 0.01 ;

	@Override
	public CarrierPlan selectPlan(Carrier carrier) {
			CarrierPlan selectedPlan = carrier.getSelectedPlan() ;
			CarrierPlan alternativePlan = new SelectBestPlan().selectPlan(carrier) ;
			double proba = 0. ;
			if ( alternativePlan.getScore() >= selectedPlan.getScore() ) {
				proba = 1. ;
			} else {
				proba = Math.exp( beta * ( selectedPlan.getScore() - alternativePlan.getScore() ) ) ;
			}
			if ( MatsimRandom.getRandom().nextDouble() < proba * alpha ) {
				return alternativePlan ;
			} else {
				return selectedPlan ;
			}
	}

}
