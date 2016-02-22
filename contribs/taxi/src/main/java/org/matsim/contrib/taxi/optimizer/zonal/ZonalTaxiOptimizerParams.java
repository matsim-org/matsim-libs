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

package org.matsim.contrib.taxi.optimizer.zonal;

import org.apache.commons.configuration.Configuration;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;


public class ZonalTaxiOptimizerParams
    extends RuleBasedTaxiOptimizerParams
{
    public static final String ZONES_XML_FILE = "zonesXmlFile";
    public static final String ZONES_SHP_FILE = "zonesShpFile";
    public static final String EXPANSION_DISTANCE = "expansionDistance";

    public final String zonesXmlFile;
    public final String zonesShpFile;
    public final double expansionDistance;


    public ZonalTaxiOptimizerParams(Configuration optimizerConfig)
    {
        super(optimizerConfig);

        zonesXmlFile = optimizerConfig.getString(ZONES_XML_FILE);
        zonesShpFile = optimizerConfig.getString(ZONES_SHP_FILE);
        expansionDistance = optimizerConfig.getDouble(EXPANSION_DISTANCE);
    }
    
    
    @Override
    public ZonalTaxiOptimizer createTaxiOptimizer(TaxiOptimizerContext optimContext)
    {
        return new ZonalTaxiOptimizer(optimContext);
    }
}
