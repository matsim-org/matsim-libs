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

package playground.jbischoff.taxi.sim;

import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import playground.jbischoff.energy.charging.taxi.ElectricTaxiChargingHandler;
import playground.jbischoff.taxi.optimizer.rank.NOSRankTaxiOptimizer;


/**
 * @author jbischoff
 */
public class ElectricTaxiSimEngine
    implements MobsimEngine
{
    private ElectricTaxiChargingHandler dac;
    private NOSRankTaxiOptimizer optimizer;


    public ElectricTaxiSimEngine(NOSRankTaxiOptimizer optimizer, ElectricTaxiChargingHandler dac)
    {
        this.optimizer = optimizer;
        this.dac = dac;
    }


    @Override
    public void doSimStep(double time)
    {

        this.optimizer.doSimStep(time);
        this.dac.doSimStep(time);
    }


    @Override
    public void onPrepareSim()
    {}


    @Override
    public void afterSim()
    {}


    @Override
    public void setInternalInterface(InternalInterface internalInterface)
    {}
}
