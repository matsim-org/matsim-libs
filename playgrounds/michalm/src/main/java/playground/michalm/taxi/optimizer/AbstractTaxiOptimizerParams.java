/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.michalm.taxi.optimizer;

import java.lang.reflect.Constructor;

import org.apache.commons.configuration.Configuration;


public abstract class AbstractTaxiOptimizerParams
    implements TaxiOptimizerParams
{
    public static <T extends AbstractTaxiOptimizerParams> T createParams(
            Configuration optimizerConfig)
    {
        try {
            @SuppressWarnings("unchecked")
            Class<T> algoParamsClass = (Class<T>)Class
                    .forName(optimizerConfig.getString(PARAMS_CLASS));

            Constructor<T> constructor = algoParamsClass.getConstructor(Configuration.class);
            return constructor.newInstance(optimizerConfig);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public enum TravelTimeSource
    {
        FREE_FLOW_SPEED, EVENTS;
    }


    public static final String PARAMS_CLASS = "paramsClass";

    public static final String ID = "id";
    public static final String TRAVEL_TIME_SOURCE = "travelTimeSource";

    public static final String REOPTIMIZATION_TIME_STEP = "reoptimizationTimeStep";

    protected final Configuration optimizerConfig;
    public final String id;
    public final TravelTimeSource travelTimeSource;

    //usually 1 s; however, the assignment strategy for TaxiBerlin used 10 s (IEEE IS paper)
    public final int reoptimizationTimeStep;


    public AbstractTaxiOptimizerParams(Configuration optimizerConfig)
    {
        this.optimizerConfig = optimizerConfig;

        id = optimizerConfig.getString(ID);
        travelTimeSource = TravelTimeSource.valueOf(optimizerConfig.getString(TRAVEL_TIME_SOURCE));

        reoptimizationTimeStep = optimizerConfig.getInt(REOPTIMIZATION_TIME_STEP, 1);
    }


    public Configuration getOptimizerConfig()
    {
        return optimizerConfig;//TODO: convert into ImmutableConfiguration (possible in ver. 2.0)
    }


    public abstract AbstractTaxiOptimizer createTaxiOptimizer(TaxiOptimizerContext optimContext);
}
