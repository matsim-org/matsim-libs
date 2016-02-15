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

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.util.random.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;

import com.vividsolutions.jts.geom.*;

import playground.michalm.zone.*;
import playground.michalm.zone.util.RandomPointUtils;


public class DefaultActivityCreator
    implements ActivityCreator
{
    protected final UniformRandom uniform = RandomUtils.getGlobalUniform();
    protected final Network network;
    protected final PopulationFactory pf;

    protected final GeometryProvider geometryProvider;
    protected final PointAcceptor pointAcceptor;


    public DefaultActivityCreator(Scenario scenario)
    {
        this(scenario, DEFAULT_GEOMETRY_PROVIDER, DEFAULT_POINT_ACCEPTOR);
    }


    public DefaultActivityCreator(Scenario scenario, GeometryProvider geometryProvider,
            PointAcceptor pointAcceptor)
    {
        this.network = scenario.getNetwork();
        this.pf = scenario.getPopulation().getFactory();
        this.geometryProvider = geometryProvider;
        this.pointAcceptor = pointAcceptor;
    }


    @Override
    public Activity createActivity(Zone zone, String actType)
    {
        Geometry geometry = geometryProvider.getGeometry(zone, actType);
        Point p = null;
        do {
            p = RandomPointUtils.getRandomPointInGeometry(geometry);
        }
        while (!pointAcceptor.acceptPoint(zone, actType, p));

        Coord coord = new Coord(p.getX(), p.getY());
        Link link = NetworkUtils.getNearestLink(network, coord);

        ActivityImpl activity = (ActivityImpl)pf.createActivityFromCoord(actType, coord);
        activity.setLinkId(link.getId());
        return activity;
    }


    public static final GeometryProvider DEFAULT_GEOMETRY_PROVIDER = new GeometryProvider() {
        public Geometry getGeometry(Zone zone, String actType)
        {
            return zone.getMultiPolygon();
        }
    };


    public interface GeometryProvider
    {
        Geometry getGeometry(Zone zone, String actType);
    }


    public static final PointAcceptor DEFAULT_POINT_ACCEPTOR = new PointAcceptor() {
        public boolean acceptPoint(Zone zone, String actType, Point point)
        {
            return true;
        }
    };


    public interface PointAcceptor
    {
        boolean acceptPoint(Zone zone, String actType, Point point);
    }
}
