package playground.michalm.vrp;

import java.util.*;

import org.geotools.factory.*;
import org.geotools.feature.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.utils.geometry.geotools.*;
import org.matsim.core.utils.gis.*;
import org.opengis.referencing.crs.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.ShortestPath.SPEntry;

import com.vividsolutions.jts.geom.*;


public class Routes2QGIS
{
    private Schedule[] schedules;
    private String filename;
    private FeatureType featureType;
    private GeometryFactory geofac;
    private MATSimVRPData data;
    private Collection<Feature> features;


    public Routes2QGIS(Schedule[] schedules, MATSimVRPData data, String filename)
    {
        this.schedules = schedules;
        this.data = data;
        this.filename = filename;

        geofac = new GeometryFactory();
        initFeatureType(data.getCoordSystem());
    }


    public void write()
    {
        for (Schedule s : schedules) {
            Iterator<DriveTask> driveIter = Schedules.createDriveTaskIter(s);

            if (!driveIter.hasNext()) {
                continue;
            }

            features = new ArrayList<Feature>();

            while (driveIter.hasNext()) {
                DriveTask drive = driveIter.next();
                LineString ls = createLineString(drive);

                if (ls != null) {
                    try {
                        Vehicle veh = s.getVehicle();
                        features.add(featureType.create(new Object[] { ls, veh.id, veh.name,
                                s.getId(), drive.getScheduleIdx() }));
                    }
                    catch (IllegalAttributeException e) {
                        e.printStackTrace();
                    }
                }
            }

            ShapeFileWriter.writeGeometries(features, filename + s.getId() + ".shp");
        }
    }


    private LineString createLineString(DriveTask driveTask)
    {
        SPEntry entry = data.getShortestPaths()[driveTask.getFromVertex().getId()][driveTask
                .getToVertex().getId()].getSPEntry(driveTask.getBeginTime());

        Id[] ids = entry.linkIds;

        if (ids.length == 0) {
            return null;
        }

        List<Coordinate> coordList = new ArrayList<Coordinate>();
        Map<Id, ? extends Link> linksMap = data.getScenario().getNetwork().getLinks();

        Link link = linksMap.get(entry.linkIds[0]);
        Coord c = link.getFromNode().getCoord();
        coordList.add(new Coordinate(c.getX(), c.getY()));

        for (Id l : entry.linkIds) {
            link = linksMap.get(l);
            c = link.getToNode().getCoord();
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
