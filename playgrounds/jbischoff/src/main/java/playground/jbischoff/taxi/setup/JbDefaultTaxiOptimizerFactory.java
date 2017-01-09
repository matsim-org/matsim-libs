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

package playground.jbischoff.taxi.setup;

import org.apache.commons.configuration.*;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerFactory;
import org.matsim.contrib.taxi.optimizer.assignment.*;
import org.matsim.contrib.taxi.optimizer.fifo.*;
import org.matsim.contrib.taxi.optimizer.rules.*;
import org.matsim.core.config.ConfigGroup;

import playground.jbischoff.taxi.inclusion.optimizer.InclusionRuleBasedTaxiOptimizer;
import playground.jbischoff.taxi.inclusion.optimizer.InclusionRuleBasedTaxiOptimizerParams;


public class JbDefaultTaxiOptimizerFactory
    implements TaxiOptimizerFactory
{
    public static final String TYPE = "type";


    public enum OptimizerType
    {
        ASSIGNMENT, FIFO, RULE_BASED, ZONAL, INCLUSION;
    }


    @Override
    public TaxiOptimizer createTaxiOptimizer(TaxiOptimizerContext optimContext,
            ConfigGroup optimizerConfigGroup)
    {
        Configuration optimizerConfig = new MapConfiguration(optimizerConfigGroup.getParams());
        OptimizerType type = OptimizerType.valueOf(optimizerConfig.getString(TYPE));

        switch (type) {
            case ASSIGNMENT:
                return new AssignmentTaxiOptimizer(optimContext,
                        new AssignmentTaxiOptimizerParams(optimizerConfig));

            case FIFO:
                return new FifoTaxiOptimizer(optimContext,
                        new FifoTaxiOptimizerParams(optimizerConfig));

            case RULE_BASED:
                return new RuleBasedTaxiOptimizer(optimContext,
                        new RuleBasedTaxiOptimizerParams(optimizerConfig));
                
            case INCLUSION:
                return new InclusionRuleBasedTaxiOptimizer(optimContext,
                        new InclusionRuleBasedTaxiOptimizerParams(optimizerConfig));    

            case ZONAL:
                return new RuleBasedTaxiOptimizer(optimContext,
                        new RuleBasedTaxiOptimizerParams(optimizerConfig));
            default:
                throw new IllegalStateException();
        }
    }
}
