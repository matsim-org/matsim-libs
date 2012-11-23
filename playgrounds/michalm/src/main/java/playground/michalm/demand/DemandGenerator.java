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

package playground.michalm.demand;

import java.io.IOException;
import java.util.Arrays;

import javax.naming.ConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.xml.sax.SAXException;

import playground.michalm.demand.Zone.Act;
import playground.michalm.demand.Zone.Group;


public class DemandGenerator
    extends AbstractDemandGenerator
{
    public DemandGenerator(String networkFileName, String zonesXMLFileName,
            String zonesShpFileName, String idField)
        throws IOException, SAXException, ParserConfigurationException

    {
        super(networkFileName, zonesXMLFileName, zonesShpFileName, idField);
    }


    public void generate()
    {
        // activityPlaces - for random generation
        Distribution<Zone> sDistrib = new Distribution<Zone>();
        Distribution<Zone> wDistrib = new Distribution<Zone>();
        Distribution<Zone> lDistrib = new Distribution<Zone>();

        for (Zone z : zones.values()) {
            sDistrib.add(z, z.getActPlaces(Act.S).intValue());
            wDistrib.add(z, z.getActPlaces(Act.W).intValue());
            lDistrib.add(z, z.getActPlaces(Act.L).intValue());
        }

        PopulationFactory pf = getPopulationFactory();

        for (Zone z : zones.values()) {
            // S - students: h-s-h-l-h
            int sCount = z.getGroupSize(Group.S);
            for (int s = 0; s < sCount; s++) {
                Plan plan = createPlan();

                int timeShift = uniform.nextIntFromTo(-3600, 3600);

                Coord home = getRandomCoordInZone(z);
                Activity act = createActivity(plan, "h", home);
                act.setEndTime(8 * 3600 + timeShift);

                plan.addLeg(pf.createLeg(TransportMode.car));

                act = createActivity(plan, "s", getRandomCoordInZone(sDistrib.draw()));
                act.setStartTime(9 * 3600 + timeShift);
                act.setEndTime(15 * 3600 + timeShift);

                plan.addLeg(pf.createLeg(TransportMode.car));

                act = createActivity(plan, "l", getRandomCoordInZone(lDistrib.draw()));
                act.setStartTime(16 * 3600 + timeShift);
                act.setEndTime(18 * 3600 + timeShift);

                plan.addLeg(pf.createLeg(TransportMode.car));

                act = createActivity(plan, "h", home);
                act.setStartTime(19 * 3600 + timeShift);

                createAndInitPerson(plan);
            }

            // W - workers: h-w-l-h
            int wCount = z.getGroupSize(Group.W);
            for (int w = 0; w < wCount; w++) {
                Plan plan = createPlan();

                int timeShift = uniform.nextIntFromTo(-3600, 7200);

                if (uniform.nextDouble() > 0.5) {// 50%
                    Coord home = getRandomCoordInZone(z);
                    Activity act = createActivity(plan, "h", home);
                    act.setEndTime(7 * 3600 + timeShift);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    act = createActivity(plan, "w", getRandomCoordInZone(wDistrib.draw()));
                    act.setStartTime(8 * 3600 + timeShift);
                    act.setEndTime(12 * 3600 + timeShift);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    act = createActivity(plan, "l", getRandomCoordInZone(lDistrib.draw()));
                    act.setStartTime(13 * 3600 + timeShift);
                    act.setEndTime(14 * 3600 + timeShift);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    act = createActivity(plan, "w", getRandomCoordInZone(wDistrib.draw()));
                    act.setStartTime(15 * 3600 + timeShift);
                    act.setEndTime(18 * 3600 + timeShift);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    act = createActivity(plan, "h", home);
                    act.setStartTime(19 * 3600 + timeShift);
                }
                else {
                    Coord home = getRandomCoordInZone(z);
                    Activity act = createActivity(plan, "h", home);
                    act.setEndTime(8 * 3600 + timeShift);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    act = createActivity(plan, "w", getRandomCoordInZone(wDistrib.draw()));
                    act.setStartTime(9 * 3600 + timeShift);
                    act.setEndTime(17 * 3600 + timeShift);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    act = createActivity(plan, "l", home);
                    act.setStartTime(18 * 3600 + timeShift);
                    act.setEndTime(19 * 3600 + timeShift);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    act = createActivity(plan, "h", home);
                    act.setStartTime(20 * 3600 + timeShift);
                }
                
                createAndInitPerson(plan);
            }

            // O - others: h-l-h
            int oCount = z.getGroupSize(Group.O);
            for (int o = 0; o < oCount; o++) {
                Plan plan = createPlan();

                int timeShift = uniform.nextIntFromTo(-3600, 3600);

                Coord home = getRandomCoordInZone(z);
                Activity act = createActivity(plan, "h", home);
                act.setEndTime(11 * 3600 + timeShift);

                plan.addLeg(pf.createLeg(TransportMode.car));

                act = createActivity(plan, "l", getRandomCoordInZone(lDistrib.draw()));
                act.setStartTime(12 * 3600 + timeShift);
                act.setEndTime(15 * 3600 + timeShift);

                plan.addLeg(pf.createLeg(TransportMode.car));

                act = createActivity(plan, "h", home);
                act.setStartTime(16 * 3600 + timeShift);
                
                createAndInitPerson(plan);
            }
        }
    }


    public static void main(String[] args)
        throws ConfigurationException, IOException, SAXException, ParserConfigurationException
    {
        String dirName;

        String networkFileName;
        String zonesXMLFileName;
        String zonesShpFileName;
        String plansFileName;
        String idField;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec1\\";
            networkFileName = dirName + "network.xml";
            zonesXMLFileName = dirName + "zones1.xml";
            zonesShpFileName = dirName + "zones1.shp";
            plansFileName = dirName + "plans1.xml";
            idField = "ID";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
            // networkFileName = dirName + "network2.xml";
            // zonesXMLFileName = dirName + "zones2.xml";
            // zonesShpFileName = dirName + "zones2.shp";
            // plansFileName = dirName + "plans2.xml";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\Paj\\";
            // networkFileName = dirName + "network.xml";
            // zonesXMLFileName = dirName + "zone.xml";
            // zonesShpFileName = dirName + "zone.shp";
            // plansFileName = dirName + "plans.xml";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
            // networkFileName = dirName + "network2.xml";
            // zonesXMLFileName = dirName + "zones2.xml";
            // zonesShpFileName = dirName + "zone.shp";
            // plansFileName = dirName + "plans.xml";
        }
        else if (args.length == 6) {
            dirName = args[0];
            networkFileName = dirName + args[1];
            zonesXMLFileName = dirName + args[2];
            zonesShpFileName = dirName + args[3];
            plansFileName = dirName + args[4];
            idField = args[5];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        DemandGenerator dg = new DemandGenerator(networkFileName, zonesXMLFileName,
                zonesShpFileName, idField);
        dg.generate();
        dg.write(plansFileName);
    }
}
