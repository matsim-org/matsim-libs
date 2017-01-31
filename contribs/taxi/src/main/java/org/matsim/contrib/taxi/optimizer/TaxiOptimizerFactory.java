/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer;

import org.matsim.core.config.ConfigGroup;


public interface TaxiOptimizerFactory
{

	//	yyyy: Why is TaxiOptimizerFactory a factory, i.e. why not just
//
//	bind(TaxiOptimizer.class).toProvider( DefaultTaxiOptimizerProvider.class ) ;
//
//	I can see that you are passing TaxiOptimizerContext and ConfigGroup through the factory's creational method ... but in other work we have (I think) seen that in the end one is more flexible when calling the creational method without argument and get the necessary information from injection.
//
//	The only counter-argument I can see is that there might be several factories side by side, for example for different sub-fleets.  But right now the TaxiData is injected anyway, so this is not possible with the current set-up.
// 
// kai, jan'17
	
    TaxiOptimizer createTaxiOptimizer(TaxiOptimizerContext optimContext,
            ConfigGroup optimizerConfigGroup);
}
