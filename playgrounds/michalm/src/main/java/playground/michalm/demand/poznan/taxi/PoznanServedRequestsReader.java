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

package playground.michalm.demand.poznan.taxi;

import java.io.*;
import java.text.*;
import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.*;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


public class PoznanServedRequestsReader
{
    public static class ServedRequest
    {
        public final Date accepted;
        public final Date assigned;
        public final Coord from;
        public final Coord to;
        public final Id taxiId;


        public ServedRequest(Date accepted, Date assigned, Coord from, Coord to, Id taxiId)
        {
            super();
            this.accepted = accepted;
            this.assigned = assigned;
            this.from = from;
            this.to = to;
            this.taxiId = taxiId;
        }
    }


    private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
    private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
            TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);

    private final Scenario scenario;
    private final List<ServedRequest> requests;

    private Scanner scanner;


    public PoznanServedRequestsReader(Scenario scenario, List<ServedRequest> requests)
    {
        this.scenario = scenario;
        this.requests = requests;
    }


    public void readFile(String file)
        throws FileNotFoundException, ParseException
    {
        scanner = new Scanner(new File(file));

        // Przyjęte    Wydane  Skąd-dług   Skąd-szer   Dokąd-dług  Dokąd-szer  Id.taxi
        scanner.nextLine();//skip the header line

        while (scanner.hasNext()) {
            // 01-02-2014 00:00:26 01-02-2014 00:00:22 16.964106   52.401409   16.898370   52.428270   329
            Date accepted = getNextDate();
            Date assigned = getNextDate();
            Coord from = getNextCoord();
            Coord to = getNextCoord();
            Id taxiId = scenario.createId(scanner.next());
            requests.add(new ServedRequest(accepted, assigned, from, to, taxiId));
        }

        scanner.close();
    }


    private Date getNextDate()
        throws ParseException
    {
        String day = scanner.next();
        String time = scanner.next();
        return sdf.parse(day + " " + time);
    }


    private Coord getNextCoord()
    {
        double x = scanner.nextDouble();
        double y = scanner.nextDouble();
        return ct.transform(new CoordImpl(x, y));
    }


    public static void main(String[] args)
        throws FileNotFoundException, ParseException
    {
        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        List<ServedRequest> requests = new ArrayList<ServedRequest>();

        String commonPath = "d:/PP-rad/taxi/poznan-supply/dane/zlecenia_obsluzone/Zlecenia_obsluzone_";
        String input_2014_02 = commonPath + "2014-02.csv";
        String input_2014_03 = commonPath + "2014-03.csv";
        String input_2014_04 = commonPath + "2014-04.csv";
        new PoznanServedRequestsReader(scenario, requests).readFile(input_2014_02);
        new PoznanServedRequestsReader(scenario, requests).readFile(input_2014_03);
        new PoznanServedRequestsReader(scenario, requests).readFile(input_2014_04);
    }
}