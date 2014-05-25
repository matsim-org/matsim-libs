/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ClonesModule.java
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

package playground.mzilske.clones;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.matsim.core.controler.listener.ControlerListener;

public class ClonesModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<ControlerListener> controlerListenerBinder = Multibinder.newSetBinder(binder(), ControlerListener.class);
        controlerListenerBinder.addBinding().toProvider(ClonesControlerListener.class);
        controlerListenerBinder.addBinding().toProvider(CloneHistogramControlerListener.class);
    }

}
