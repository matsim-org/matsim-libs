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

package org.matsim.contrib.taxi.optimizer.fifo;

import org.apache.commons.configuration.Configuration;
import org.matsim.contrib.taxi.optimizer.*;


public class FifoTaxiOptimizerParams
    extends AbstractTaxiOptimizerParams
{
    public FifoTaxiOptimizerParams(Configuration optimizerConfig)
    {
        super(optimizerConfig);
    }


    @Override
    public FifoTaxiOptimizer createTaxiOptimizer(TaxiOptimizerContext optimContext)
    {
        return new FifoTaxiOptimizer(optimContext);
    }

}
