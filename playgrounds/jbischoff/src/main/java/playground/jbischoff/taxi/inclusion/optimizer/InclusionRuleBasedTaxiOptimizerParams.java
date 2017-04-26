/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.taxi.inclusion.optimizer;

import org.apache.commons.configuration.Configuration;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class InclusionRuleBasedTaxiOptimizerParams extends RuleBasedTaxiOptimizerParams {

    public final String INCLUSION_TAXI_PREFIX = "hc_";
    public final String INCLUSION_CUSTOMER_PREFIX = "hc_";

	/**
	 * @param optimizerConfig
	 */
	public InclusionRuleBasedTaxiOptimizerParams(Configuration optimizerConfig) {
		super(optimizerConfig);
	}

}
