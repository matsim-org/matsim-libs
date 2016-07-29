/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dynagent.examples.random;

import org.matsim.contrib.dynagent.AbstractDynActivity;
import org.matsim.core.gbl.MatsimRandom;


public class RandomDynActivity
    extends AbstractDynActivity
{
    private double endTime;


    public RandomDynActivity(double now)
    {
        super("RandomActivity");
        doRandomChoice(now);//decision made at time beginTime
    }


    @Override
    public double getEndTime()
    {
        return endTime;
    }


    @Override
    public void doSimStep(double now)
    {
        doRandomChoice(now);//decisions made at times beginTime+1, ..., endTime
    }


    private void doRandomChoice(double now)
    {
        //When do I want to stop the current activity?
        endTime = now + MatsimRandom.getRandom().nextInt(100);//1% chance that endTime == now
    }
}
