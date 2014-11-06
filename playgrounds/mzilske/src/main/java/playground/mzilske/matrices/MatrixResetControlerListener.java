/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MatricesUpdater.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.matrices;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.matrices.Entry;

import javax.inject.Inject;
import java.util.Collection;

class MatrixResetControlerListener implements BeforeMobsimListener, AfterMobsimListener {

    @Inject
    TimedMatrices timedMatrices;

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        for (TimedMatrix matrix : timedMatrices.getMatrices()) {
            for (Collection<Entry> entries : matrix.getMatrix().getFromLocations().values()) {
                for (Entry entry : entries) {
                    entry.setValue(0.0);
                }
            }
        }
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        System.out.println("Got matrix!");
    }

}
