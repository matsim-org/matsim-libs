package playground.michalm.vrp;

import java.io.*;
import java.util.*;

import org.geotools.factory.*;
import org.geotools.feature.*;
import org.matsim.core.utils.geometry.geotools.*;
import org.matsim.core.utils.geometry.transformations.*;
import org.matsim.core.utils.gis.*;
import org.opengis.referencing.crs.*;

import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.file.*;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.*;

import com.vividsolutions.jts.geom.*;


// taken from org.matsim.utils.gis.matsim2esri.network.Nodes2ESRIShape
public class Localizables2QGIS
{
    private Localizable[] localizables;
    private String filename;
    private FeatureType featureType;


    public Localizables2QGIS(Localizable[] localizables, String filename, String coordinateSystem)
    {
        this.localizables = localizables;
        this.filename = filename;
        initFeatureType(coordinateSystem);
    }


    public void write()
    {
        Collection<Feature> features = new ArrayList<Feature>();

        for (Localizable localizable : localizables) {
            features.add(getFeature(localizable.getVertex()));
        }

        ShapeFileWriter.writeGeometries(features, filename);
    }


    private Feature getFeature(Vertex vertex)
    {
        Point p = MGC.xy2Point(vertex.getX(), vertex.getY());

        try {
            return featureType.create(new Object[] { p, vertex.getId(), vertex.getName() });
        }
        catch (IllegalAttributeException e) {
            throw new RuntimeException(e);
        }
    }


    private void initFeatureType(final String coordinateSystem)
    {
        CoordinateReferenceSystem crs = MGC.getCRS(coordinateSystem);
        AttributeType[] attribs = new AttributeType[3];
        attribs[0] = DefaultAttributeTypeFactory.newAttributeType("Point", Point.class, true, null,
                null, crs);
        attribs[1] = AttributeTypeFactory.newAttributeType("ID", Integer.class);
        attribs[2] = AttributeTypeFactory.newAttributeType("Name", String.class);

        try {
            featureType = FeatureTypeBuilder.newFeatureType(attribs, "vrp_node");
        }
        catch (FactoryRegistryException e) {
            e.printStackTrace();
        }
        catch (SchemaException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args)
        throws IOException
    {
        String dirName;
        String vrpDirName;
        String vrpStaticFileName;
        String outFileNameCust;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            dirName = "d:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
            vrpDirName = dirName + "dvrp\\";
            vrpStaticFileName = "A102.txt";
            outFileNameCust = vrpDirName + "customers.shp";
        }
        else if (args.length == 4) {
            dirName = args[0];
            vrpDirName = dirName + args[1];
            vrpStaticFileName = args[2];
            outFileNameCust = vrpDirName + args[3];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        VRPData data = LacknerReader.parseStaticFile(vrpDirName, vrpStaticFileName,
                new VertexImpl.Builder());
        String coordSystem = TransformationFactory.WGS84_UTM33N;

        new Localizables2QGIS(data.customers, outFileNameCust, coordSystem).write();
    }
}
