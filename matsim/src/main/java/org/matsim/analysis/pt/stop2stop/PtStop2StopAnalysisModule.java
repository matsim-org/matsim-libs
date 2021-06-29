/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.analysis.pt.stop2stop;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;

/**
 *
 *  @author vsp-gleich
 */
public class PtStop2StopAnalysisModule extends AbstractModule {

    @Override
    public void install() {
        bind(PtStop2StopAnalysisControlerListener.class).in(Singleton.class);
        addControlerListenerBinding().to(PtStop2StopAnalysisControlerListener.class);
    }
}
