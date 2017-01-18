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

package playground.agarwalamit.analysis.emission;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.utils.MapUtils;

/**
 * Created by amit on 01/12/2016.
 */

public class EmissionAnalysisControlerListener implements  ShutdownListener {

    private static final Logger LOG = Logger.getLogger(EmissionAnalysisControlerListener.class);

    public EmissionAnalysisControlerListener(final EmissionCostHandler emissionCostHandler) {
        this.emissionCostHandler = emissionCostHandler;
    }

    private final EmissionCostHandler emissionCostHandler;

    @Override
    public void notifyShutdown(ShutdownEvent event) {

        ControlerConfigGroup controlerConfigGroup = event.getServices().getConfig().controler();
        String iterationDir = controlerConfigGroup.getOutputDirectory()+"/ITERS/it."+controlerConfigGroup.getLastIteration()+"/";

        String emissionEventsFile = iterationDir+controlerConfigGroup.getLastIteration()+".emission.events.xml.gz";
        if (! new File(emissionEventsFile).exists()) {
            LOG.error("The emission events file for last iteration does not exists.");
            return;
        }

        this.emissionCostHandler.reset(0); // resetting everything.
        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(this.emissionCostHandler);
        EmissionEventsReader reader = new EmissionEventsReader(events);
        reader.readFile(emissionEventsFile);

        Map<String, Double> userGrp2cost = this.emissionCostHandler.getUserGroup2TotalEmissionCosts();

        try (BufferedWriter writer = IOUtils.getBufferedWriter(iterationDir+controlerConfigGroup.getLastIteration()+".emissionsCostsMoneyUnits.txt")) {
            if(this.emissionCostHandler.isFiltering()) {
                writer.write("userGroup \t costsInMoneyUnits \n");
                for ( String ug : userGrp2cost.keySet()) {
                    writer.write(ug+"\t");
                    writer.write(userGrp2cost.get(ug) + "\n");
                }
            }
            writer.write( "allPersons \t" + MapUtils.doubleValueSum(userGrp2cost)+"\n");
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }
}
