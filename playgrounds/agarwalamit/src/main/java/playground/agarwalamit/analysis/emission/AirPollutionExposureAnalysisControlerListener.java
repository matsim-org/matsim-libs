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
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.emission.experienced.ExperiencedEmissionCostHandler;
import playground.agarwalamit.utils.MapUtils;
import playground.kai.usecases.combinedEventsReader.CombinedMatsimEventsReader;
import playground.vsp.airPollution.exposure.GridTools;
import playground.vsp.airPollution.exposure.IntervalHandler;
import playground.vsp.airPollution.exposure.ResponsibilityGridTools;

/**
 * Created by amit on 01/12/2016.
 */

public class AirPollutionExposureAnalysisControlerListener implements  ShutdownListener {

    private static final Logger LOG = Logger.getLogger(AirPollutionExposureAnalysisControlerListener.class);

    @Inject
    private OutputDirectoryHierarchy controlerIO;

    @Inject
    private ControlerConfigGroup controlerConfigGroup;

    @Inject
    private QSimConfigGroup qSimConfigGroup;

    @Inject
    private GridTools gridTools;

    @Inject
    private ResponsibilityGridTools responsibilityGridTools;

    @Inject
    private ExperiencedEmissionCostHandler emissionCostHandler;

    @Override
    public void notifyShutdown(ShutdownEvent event) {

        int lastIt = controlerConfigGroup.getLastIteration();
        String eventsFile = controlerIO.getIterationFilename(lastIt, "events.xml.gz");

        IntervalHandler intervalHandler = new IntervalHandler(responsibilityGridTools.getTimeBinSize(), qSimConfigGroup.getEndTime(), gridTools);
        {
            intervalHandler.reset(0);
            EventsManager eventsManager = EventsUtils.createEventsManager();
            eventsManager.addHandler(intervalHandler);
            new MatsimEventsReader(eventsManager).readFile(eventsFile);
        }

        responsibilityGridTools.resetAndcaluculateRelativeDurationFactors(intervalHandler.getDuration());

        if (! new File(eventsFile).exists()) {
            LOG.error("The emission events file for last iteration does not exists.");
            return;
        }

        this.emissionCostHandler.reset(0); // resetting everything.

        {
            EventsManager events = EventsUtils.createEventsManager();
            events.addHandler(this.emissionCostHandler);
            CombinedMatsimEventsReader reader = new CombinedMatsimEventsReader(events);
            reader.readFile(eventsFile);
        }

        Map<String, Double> userGrp2cost = this.emissionCostHandler.getUserGroup2TotalEmissionCosts();
        try (BufferedWriter writer = IOUtils.getBufferedWriter(controlerIO.getIterationFilename(lastIt,"emissionsCostsMoneyUnits.txt")) ) {
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
