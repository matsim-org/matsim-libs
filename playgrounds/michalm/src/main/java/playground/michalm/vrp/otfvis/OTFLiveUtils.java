/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.vrp.otfvis;

import javax.swing.SwingUtilities;

import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;

import pl.poznan.put.vrp.dynamic.data.VrpData;


public class OTFLiveUtils
{
    /**
     * All fleet vehicles are selected (i.e. presented in circles)
     * 
     * @param qSim
     * @param vrpData
     */
    public static void initQueryHandler(QSim qSim, final VrpData vrpData)
    {
        qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
            public void notifyMobsimInitialized(
                    @SuppressWarnings("rawtypes") MobsimInitializedEvent e)
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        // OTFQueryControl control = (OTFQueryControl)OTFClientControl.getInstance()
                        // .getMainOTFDrawer().getQueryHandler();
                        //
                        // for (Vehicle v : vrpData.getVehicles()) {
                        // QueryAgentPlan query = new QueryAgentPlan();
                        // query.setId(v.getName());
                        // control.createQuery(query);
                        // }
                    }
                });
            }
        });
    }
}
