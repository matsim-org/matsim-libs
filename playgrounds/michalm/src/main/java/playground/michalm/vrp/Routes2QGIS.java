package playground.michalm.vrp;

import java.util.*;

import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;

import org.geotools.factory.*;
import org.geotools.feature.*;
import org.matsim.api.core.v01.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.geotools.*;
import org.matsim.core.utils.gis.*;
import org.opengis.referencing.crs.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.*;

import com.vividsolutions.jts.geom.*;


public class Routes2QGIS
{
    private Route[] routes;
    private String filename;
    private FeatureType featureType;
    private GeometryFactory geofac;
    private MATSimVRPData data;
    private Collection<Feature> features;


    public Routes2QGIS(Route[] routes, MATSimVRPData data, String filename)
    {
        this.routes = routes;
        this.data = data;
        this.filename = filename;

        geofac = new GeometryFactory();
        initFeatureType(data.coordSystem);
    }


    public void write()
    {
        for (Route route : routes) {
            List<Request> reqs = route.getRequests();

            if (reqs.size() == 0) {
                continue;
            }

            features = new ArrayList<Feature>();

            // starting from the depot
            Node depotNode = route.vehicle.depot.node;
            Node prevNode = depotNode;
            int departTime = route.beginTime;

            for (int i = 0; i < reqs.size(); i++) {
                Request req = reqs.get(i);
                Node currNode = req.fromNode;

                addLineString(route, i, prevNode, currNode, departTime);

                if (req.fromNode != req.toNode) { // i.e. taxi service
                    currNode = req.toNode;
                    addLineString(route, i, req.fromNode, currNode, req.startTime);
                }

                prevNode = currNode;
                departTime = req.departureTime;
            }

            addLineString(route, reqs.size(), prevNode, depotNode, departTime);

            ShapeFileWriter.writeGeometries(features, filename + route.id + ".shp");
        }
    }


    private void addLineString(Route route, int arcIdx, Node fromNode, Node toNode, int departTime)
    {
        LineString ls = createLineString(fromNode, toNode, departTime);

        if (ls != null) {
            try {
                Vehicle veh = route.vehicle;
                features.add(featureType.create(new Object[] { ls, veh.id, veh.name, route.id,
                        arcIdx }));
            }
            catch (IllegalAttributeException e) {
                e.printStackTrace();
            }
        }
    }


    private LineString createLineString(Node fromNode, Node toNode, int departTime)
    {
        Path path = data.shortestPaths[fromNode.id][toNode.id].getPath(departTime);

        if (path == ShortestPath.ZERO_PATH) {
            return null;
        }

        List<Coordinate> coordList = new ArrayList<Coordinate>();

        // starting coordinate
        Coord c = data.nodeToLinks[fromNode.id].getFromNode().getCoord();
        coordList.add(new Coordinate(c.getX(), c.getY()));

        // path coordinates
        for (org.matsim.api.core.v01.network.Node node : path.nodes) {
            c = node.getCoord();
            coordList.add(new Coordinate(c.getX(), c.getY()));
        }

        return geofac.createLineString(coordList.toArray(new Coordinate[coordList.size()]));
    }


    private void initFeatureType(final String coordinateSystem)
    {
        CoordinateReferenceSystem crs = MGC.getCRS(coordinateSystem);
        AttributeType[] attribs = new AttributeType[5];
        attribs[0] = DefaultAttributeTypeFactory.newAttributeType("LineString", LineString.class,
                true, null, null, crs);
        attribs[1] = AttributeTypeFactory.newAttributeType("VEH_ID", Integer.class);
        attribs[2] = AttributeTypeFactory.newAttributeType("VEH_NAME", String.class);
        attribs[3] = AttributeTypeFactory.newAttributeType("ROUTE_ID", Integer.class);
        attribs[4] = AttributeTypeFactory.newAttributeType("ARC_IDX", Integer.class);

        try {
            featureType = FeatureTypeBuilder.newFeatureType(attribs, "vrp_route");
        }
        catch (FactoryRegistryException e) {
            e.printStackTrace();
        }
        catch (SchemaException e) {
            e.printStackTrace();
        }
    }
}
