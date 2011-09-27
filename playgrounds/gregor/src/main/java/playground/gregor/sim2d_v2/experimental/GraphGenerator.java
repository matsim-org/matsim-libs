package playground.gregor.sim2d_v2.experimental;


import java.util.ArrayList;
import java.util.List;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;



import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

public class GraphGenerator {

	private static GeometryFactory  geofac = new GeometryFactory();
	public static void main(String [] args) throws FactoryRegistryException, SchemaException, IllegalAttributeException {

		String floorplan = "/Users/laemmel/devel/sim2dDemo/raw_input/floorplan.shp";
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(floorplan);
		List<LineString> ls = new ArrayList<LineString>();
		for (Feature  ft : reader.getFeatureSet()) {
			Geometry geo = ft.getDefaultGeometry();
			if (geo instanceof LineString) {
				ls.add((LineString) geo);
			} else if (geo instanceof MultiLineString) {
				MultiLineString ml = (MultiLineString)geo;
				for (int i = 0; i < ml.getNumGeometries(); i++) {
					Geometry ggeo = ml.getGeometryN(i);
					ls.add((LineString) ggeo);
				}
			}
		}

		MultiPoint mp = new DenseMultiPointFromLineString().getDenseMultiPointFromLineString(ls);

		MultiPolygon boundary = new MultiPolygonFromLineStrings().getMultiPolygon(ls, reader.getBounds());

		//		DelaunayTriangulationBuilder dtb = new DelaunayTriangulationBuilder();
		VoronoiDiagramBuilder vdb = new VoronoiDiagramBuilder();
		vdb.setClipEnvelope(reader.getBounds());
		vdb.setSites(mp);

		//		GeometryCollection dia = (GeometryCollection) dtb.getTriangles(geofac);
		GeometryCollection dia = (GeometryCollection) vdb.getDiagram(geofac);
		boundary = (MultiPolygon) boundary.buffer(0.01);



		//		int num = dia.getNumGeometries();
		//		for (int i = 0; i < num; i ++) {
		//			Geometry geo = dia.getGeometryN(i);
		//			GisDebugger.addGeometry(geo);
		//		}
		//		GisDebugger.dump("/Users/laemmel/devel/sim2dDemo/raw_input/voronoi_floorplan.shp");

		//skeleton

		Skeleton skeleton = new SkeletonExtractor().extractSkeleton(dia, boundary);
		//		skeleton.dumpLinks("/Users/laemmel/devel/sim2dDemo/raw_input/skeleton_floorplan.shp");
		//		skeleton.dumpIntersectingNodes("/Users/laemmel/devel/sim2dDemo/raw_input/skeleton_nodes_floorplan.shp");

		new SkeletonSimplifier().simplifySkeleton(skeleton);
		new Puncher().punchSkeleton(skeleton, reader.getBounds());
		//		skeleton.dumpLinks("/Users/laemmel/devel/sim2dDemo/raw_input/simplified_skeleton_floorplan.shp");
		//		GisDebugger.dump("/Users/laemmel/devel/sim2dDemo/raw_input/skeleton_floorplan.shp");

		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		NetworkFactory fac = sc.getNetwork().getFactory();
		for (SkeletonNode n : skeleton.getNodes()) {
			Node mn = fac.createNode(n.getId(), MGC.coordinate2Coord(n.getCoord()));
			sc.getNetwork().addNode(mn);
		}
		int linkNums = 0;
		for (SkeletonLink l : skeleton.getLinks()) {
			Node fromNode = sc.getNetwork().getNodes().get(l.getFromNode().getId());
			Node toNode = sc.getNetwork().getNodes().get(l.getToNode().getId());

			Link ml1 = fac.createLink(sc.createId(Integer.toString(linkNums++)), fromNode, toNode);
			ml1.setFreespeed(1.34);
			ml1.setLength(l.getFromNode().getGeometry().distance(l.getToNode().getGeometry()));
			sc.getNetwork().addLink(ml1);

			Link ml2 = fac.createLink(sc.createId(Integer.toString(linkNums++)), toNode, fromNode);
			ml2.setFreespeed(1.34);
			ml2.setLength(l.getFromNode().getGeometry().distance(l.getToNode().getGeometry()));
			sc.getNetwork().addLink(ml2);
		}

		new NetworkCleaner().run(sc.getNetwork());

		new NetworkWriter(sc.getNetwork()).write("/Users/laemmel/devel/sim2dDemo/input/network.xml");

		String [] argsII = {"/Users/laemmel/devel/sim2dDemo/input/network.xml","/Users/laemmel/devel/sim2dDemo/raw_input/networkL.shp","/Users/laemmel/devel/sim2dDemo/raw_input/networkLP.shp","EPSG:3395"};
		Links2ESRIShape.main(argsII);
	}


}
