/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CallControlerListener.java
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

package playground.mzilske.cdr;

import com.google.inject.Provider;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

import javax.inject.Inject;

class CallControlerListener implements Provider<ControlerListener> {

    static interface BeginEndListener extends StartupListener, ShutdownListener {}

    @Inject
    CompareMain compareMain;

    @Override
    public ControlerListener get() {

        return new BeginEndListener() {

            @Override
            public void notifyStartup(StartupEvent event) {
                System.out.println("Comparemain is there: "+ compareMain);
            }

            @Override
            public void notifyShutdown(ShutdownEvent event) {
                compareMain.close();
            }
        };
    }
}
