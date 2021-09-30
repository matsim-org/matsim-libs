/* *********************************************************************** *
 * project: org.matsim.*
 * LinkPaxVolumesAnalysisModule.java
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

package org.matsim.analysis.linkpaxvolumes;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;


public final class LinkPaxVolumesAnalysisModule extends AbstractModule {
    @Override
    public void install() {
        bind(LinkPaxVolumesControlerListener.class).in(Singleton.class);
        addControlerListenerBinding().to(LinkPaxVolumesControlerListener.class);
    }
}
