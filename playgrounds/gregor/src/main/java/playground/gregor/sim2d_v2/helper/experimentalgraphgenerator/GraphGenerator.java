package playground.gregor.sim2d_v2.helper.experimentalgraphgenerator;


import java.util.ArrayList;
import java.util.Collection;
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



import com.vividsolutions.jts.geom.Envelope;
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
	private final Scenario sc;

	private final Envelope envelope;
	private final Collection<Geometry> geometries;


	public GraphGenerator(Scenario sc, Collection<Geometry> geometries, Envelope envelope) {
		this.sc = sc;
		this.geometries = geometries;
		this.envelope = envelope;
	}


	public void run() {

		List<LineString> ls =  extractLineStrings();

		MultiPoint mp = new DenseMultiPointFromLineString().getDenseMultiPointFromLineString(ls);
		Geometry boundary = new MultiPolygonFromLineStrings().getMultiPolygon(ls, this.envelope);

		VoronoiDiagramBuilder vdb = new VoronoiDiagramBuilder();
		vdb.setSites(mp);
		GeometryCollection dia = (GeometryCollection) vdb.getDiagram(geofac);
		boundary = boundary.buffer(0.01);

		Skeleton skeleton = new SkeletonExtractor().extractSkeleton(dia, boundary);

		new SkeletonSimplifier().simplifySkeleton(skeleton,boundary);

		new SkeletonLinksContraction().contractShortSkeletonLinks(skeleton, boundary);

		new Puncher().punchSkeleton(skeleton, this.envelope);

		createNetwork(skeleton);

		new NetworkCleaner().run(this.sc.getNetwork());

	}


	private void createNetwork(Skeleton skeleton) {
		NetworkFactory fac = this.sc.getNetwork().getFactory();
		for (SkeletonNode n : skeleton.getNodes()) {
			Node mn = fac.createNode(n.getId(), MGC.coordinate2Coord(n.getCoord()));
			this.sc.getNetwork().addNode(mn);
		}
		int linkNums = 0;
		for (SkeletonLink l : skeleton.getLinks()) {
			Node fromNode = this.sc.getNetwork().getNodes().get(l.getFromNode().getId());
			Node toNode = this.sc.getNetwork().getNodes().get(l.getToNode().getId());

			Link ml1 = fac.createLink(this.sc.createId(Integer.toString(linkNums++)), fromNode, toNode);
			ml1.setFreespeed(1.34);
			ml1.setLength(l.getLength());
			this.sc.getNetwork().addLink(ml1);

			Link ml2 = fac.createLink(this.sc.createId(Integer.toString(linkNums++)), toNode, fromNode);
			ml2.setFreespeed(1.34);
			ml2.setLength(l.getLength());
			this.sc.getNetwork().addLink(ml2);
		}

	}


	private List<LineString> extractLineStrings() {
		List<LineString> ret = new ArrayList<LineString>();
		for (Geometry geo : this.geometries) {
			if (geo instanceof LineString) {
				ret.add((LineString) geo);
			} else if (geo instanceof MultiLineString) {
				MultiLineString ml = (MultiLineString)geo;
				for (int i = 0; i < ml.getNumGeometries(); i++) {
					Geometry ggeo = ml.getGeometryN(i);
					ret.add((LineString) ggeo);
				}
			} else {
				throw new RuntimeException("Geometry type: " +geo.getGeometryType() +" not (yet) supported.");
			}
		}

		return ret;
	}


	public static void main(String [] args) throws FactoryRegistryException, SchemaException, IllegalAttributeException {

		String floorplan = "/Users/laemmel/devel/sim2dDemo/raw_input/floorplan.shp";
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(floorplan);
		Collection<Geometry> geos = new ArrayList<Geometry>();
		for (Feature ft : reader.getFeatureSet()) {
			geos.add(ft.getDefaultGeometry());
		}

		new GraphGenerator(sc,geos,reader.getBounds()).run();
		new NetworkWriter(sc.getNetwork()).write("/Users/laemmel/devel/sim2dDemo/input/network.xml");
		String [] argsII = {"/Users/laemmel/devel/sim2dDemo/input/network.xml","/Users/laemmel/devel/sim2dDemo/raw_input/networkL.shp","/Users/laemmel/devel/sim2dDemo/raw_input/networkLP.shp","EPSG:3395"};
		Links2ESRIShape.main(argsII);
	}


}
