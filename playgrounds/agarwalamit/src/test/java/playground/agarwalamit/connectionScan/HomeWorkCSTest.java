/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.connectionScan;

import java.awt.geom.Point2D;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.ConnectionScan;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.PublicTransportRoute;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.RouteSearch;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.TransitNetwork;
import edu.kit.ifv.mobitopp.publictransport.model.*;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by amit on 09.05.17.
 */


public class HomeWorkCSTest {

    @Test
    public void test(){

        // origin-destination
        Station myHomeStation = new DefaultStation(0, Collections.emptyList());
        Stop home = new Stop(0, "ReinickendorferStr_7_13347_Berlin", new Point2D.Double(13.3702911,52.5412206), RelativeTime.ZERO, myHomeStation, 0  );

        Station myWorkStation = new DefaultStation(1, Collections.emptyList());
        Stop work = new Stop(1, "Salzufer_17-19_10587_Berlin",new Point2D.Double(13.32259, 52.51912), RelativeTime.ZERO, myWorkStation, 1);

        // departure-arrival times
        Time departTime = new Time(LocalDateTime.of(2017,5,10,8, 30, 00));
        Time arrivalTime = new Time(LocalDateTime.of(2017,5,10,8, 55, 00));

        // transport system
        TransportSystem bvg = new TransportSystem("BVG");
        Time startOfDay = new Time(LocalDateTime.of(2017,5,10,0, 00, 00));

        //lines
        ModifiableJourney home2WorkLine = new DefaultModifiableJourney(1, startOfDay, bvg, 50 );

        // connections
        RoutePoints route = RoutePoints.from(home, work);
        Connection home2work = Connection.from(0, home, work, departTime, arrivalTime, home2WorkLine, route);

        List<Stop> stopList = Arrays.asList(home, work);
        Connections connections = new Connections();
        connections.add(home2work);

        RouteSearch routeSearch = new ConnectionScan(TransitNetwork.createOf(stopList,connections));
        Optional<PublicTransportRoute> publicTransportRoute = routeSearch.findRoute(home,work,departTime);

        Assert.assertTrue(publicTransportRoute.isPresent());
    }

}
