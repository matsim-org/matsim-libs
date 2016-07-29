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

import org.apache.commons.configuration.*;
import org.matsim.contrib.taxi.optimizer.assignment.*;
import org.matsim.contrib.taxi.optimizer.fifo.*;
import org.matsim.contrib.taxi.optimizer.mip.*;
import org.matsim.contrib.taxi.optimizer.rules.*;
import org.matsim.core.config.ConfigGroup;


public class DefaultTaxiOptimizerFactory
    implements TaxiOptimizerFactory
{
    public static final String TYPE = "type";


    public enum OptimizerType
    {
        ASSIGNMENT, FIFO, MIP, RULE_BASED, ZONAL;
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

            case MIP:
                return new MIPTaxiOptimizer(optimContext,
                        new MIPTaxiOptimizerParams(optimizerConfig));

            case RULE_BASED:
                return new RuleBasedTaxiOptimizer(optimContext,
                        new RuleBasedTaxiOptimizerParams(optimizerConfig));

            case ZONAL:
                return new RuleBasedTaxiOptimizer(optimContext,
                        new RuleBasedTaxiOptimizerParams(optimizerConfig));
            default:
                throw new IllegalStateException();
        }
    }
}
