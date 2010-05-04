package playground.gregor.evacuation.riskmap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.evacuation.riskaversion.RiskCostFromFloodingData;
import org.matsim.evacuation.travelcosts.PluggableTravelCostCalculator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class RiskMap {
	private static final Logger log = Logger.getLogger(RiskMap.class);

	private static final double PI_HALF = Math.PI / 2.0;
	private static final double TWO_PI = 2.0 * Math.PI;

	private List<FloodingReader> netcdfReaders = null;
	private final ScenarioImpl scenarioData;
	private HashMap<Id, Feature> streetMap;
	private ArrayList<Feature> features;
	private FeatureType ftRunCompare;
	private final CoordinateReferenceSystem crs;
	private final String outfile;
	private final String outfile2;
	private RiskCostFromFloodingData rc;


	private FeatureType ftPoint;
	private static final String network = "/net/ils/data/countries/id/padang/gis/network_v20080618/links_v20090728.shp";
//	private static final String network = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/gis/network_v20080618/links_v20090728.shp";

	public RiskMap(ScenarioImpl scenario, String outfile, String outfile2, CoordinateReferenceSystem crs) {
		this.scenarioData = scenario;
		this.crs = crs;
		this.outfile = outfile;
		this.outfile2 = outfile2;

	}

	private void run() {
		loadNetcdfReaders();
		this.rc = new RiskCostFromFloodingData(this.scenarioData.getNetwork(), this.netcdfReaders, null, this.scenarioData.getConfig().evacuation().getBufferSize());
		try {
			buildStreetMap();
		} catch (IOException e) {
			e.printStackTrace();
		}
		initFeatures();
		createRiskCostMap();
		createRiskCostDirections();

	}

	private void createRiskCostDirections() {
		FreespeedTravelTimeCost tt = new FreespeedTravelTimeCost(-6,0,0);
		PluggableTravelCostCalculator tc = new PluggableTravelCostCalculator(tt);
		tc.addTravelCost(this.rc);
    	LeastCostPathCalculator router = new Dijkstra(this.scenarioData.getNetwork(),tc,tt);
    	Node saveNode = this.scenarioData.getNetwork().getNodes().get(new IdImpl("en2"));
		List<Feature> fts = new ArrayList<Feature>();
		log.info("directions!!");
		for (Link l : this.scenarioData.getNetwork().getLinks().values()) {
			if (l.getId().toString().contains("l")) {
				continue;
			}
			double cost = this.rc.getLinkTravelCost(l, Time.UNDEFINED_TIME);
			if (cost <= 0) {
				continue;
			}
			Path r = router.calcLeastCostPath(l.getToNode(), saveNode, 0);
			List<Node> nodes = r.nodes;
			if (nodes.get(1) == l.getFromNode()) {
//				System.out.println("YES");
//			}
//			if (cost > 0) {
				double angle = getAngle(l);
				Id id = l.getId();
				int intId = Integer.parseInt(id.toString());
				if (intId > 100000) {
					id = new IdImpl(intId - 100000);
				}
				Feature ft = this.streetMap.get(id);
				if (ft == null) {
					continue;
				}
				Point p = ft.getDefaultGeometry().getCentroid();

				try {
					fts.add(this.ftPoint.create(new Object[]{p,angle,cost,l.getLength()}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			}

		}
		try {
			log.info("going to save!!");
			ShapeFileWriter.writeGeometries(fts, this.outfile2);
			log.info("done!!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private double getAngle(Link l) {
		final double dx = -l.getFromNode().getCoord().getX()   + l.getToNode().getCoord().getX();
		final double dy = -l.getFromNode().getCoord().getY()   + l.getToNode().getCoord().getY();


		double theta = 0.0;
		if (dx > 0) {
			theta = Math.atan(dy/dx);
		} else if (dx < 0) {
			theta = Math.PI + Math.atan(dy/dx);
		} else { // i.e. DX==0
			if (dy > 0) {
				theta = PI_HALF;
			} else {
				theta = -PI_HALF;
			}
		}
		if (theta < 0.0) theta += TWO_PI;
		double tmp = 360 - theta/TWO_PI * 360 + 270;
		if (tmp > 360) { tmp -= 360; }
		return tmp;
	}

	private void createRiskCostMap() {
		for (Link l : this.scenarioData.getNetwork().getLinks().values()) {
			if (l.getId().toString().contains("l")) {
				continue;
			}
			int id = Integer.parseInt(l.getId().toString());
			if (id >= 100000) {
				continue;
			}
			Feature ft = this.streetMap.get(l.getId());
			if (ft == null) {
				continue;
			}
			IdImpl id2 = new IdImpl(id + 100000);
			Link l2 = this.scenarioData.getNetwork().getLinks().get(id2);
			double rc1 = this.rc.getLinkTravelCost(l, Time.UNDEFINED_TIME);
			double rc2 = this.rc.getLinkTravelCost(l2, Time.UNDEFINED_TIME);

			try {
				this.features.add(this.ftRunCompare.create(new Object [] {ft.getDefaultGeometry(),Math.max(rc1, rc2)/l.getLength()}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}


		}
		try {
			ShapeFileWriter.writeGeometries(this.features, this.outfile);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void initFeatures() {
		this.features = new ArrayList<Feature>();

		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.crs);
		AttributeType risk = AttributeTypeFactory.newAttributeType("risk", Double.class);

		AttributeType geom2 = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, this.crs);
		AttributeType angle = AttributeTypeFactory.newAttributeType("angle", Double.class);
		AttributeType nodeDiff = AttributeTypeFactory.newAttributeType("nodeDiff", Double.class);
		AttributeType length = AttributeTypeFactory.newAttributeType("length", Double.class);
		try {
			this.ftPoint = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom2, angle, risk,length}, "arrows");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}


		try {
			this.ftRunCompare = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, risk}, "links");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}


	}

	private void buildStreetMap() throws IOException {
		this.streetMap = new HashMap<Id,Feature>();
		FeatureSource fts = ShapeFileReader.readDataFile(network);
		Iterator it = fts.getFeatures().iterator();
		while ( it.hasNext()) {
			Feature ft = (Feature) it.next();
			Long intId = (Long) ft.getAttribute("ID");
			Id id = new IdImpl(intId);
			if (this.streetMap.get(id) != null) {
				throw new RuntimeException("Id already exists!");
			}
			this.streetMap.put(id, ft);
		}
	}


	private void loadNetcdfReaders() {
		if (this.netcdfReaders != null) {
			return;
		}
		log.info("loading netcdf readers");
		int count = this.scenarioData.getConfig().evacuation().getSWWFileCount();
		if (count <= 0) {
			return;
		}
		this.netcdfReaders  = new ArrayList<FloodingReader>();
		double offsetEast = this.scenarioData.getConfig().evacuation().getSWWOffsetEast();
		double offsetNorth = this.scenarioData.getConfig().evacuation().getSWWOffsetNorth();
		for (int i = 0; i < count; i++) {
			String netcdf = this.scenarioData.getConfig().evacuation().getSWWRoot() + "/" + this.scenarioData.getConfig().evacuation().getSWWFilePrefix() + i + this.scenarioData.getConfig().evacuation().getSWWFileSuffix();
			FloodingReader fr = new FloodingReader(netcdf);
			fr.setReadTriangles(true);
			fr.setReadFloodingSeries(false);
			fr.setOffset(offsetEast, offsetNorth);
			this.netcdfReaders.add(fr);
		}
		log.info("done.");
	}

	public static void main(String [] args) {

		String config = "./output/output_config.xml.gz";
		String network = "./output/output_network.xml.gz";
//		String config = "/home/laemmel/devel/inputs/configs/shapeFileEvac.xml";
//		String network = "/home/laemmel/arbeit/svn/runs-svn/run1014/output/output_network.xml.gz";
		String outfile = "./link_risk_costs.shp";
		String outfile2 = "./link_risk_arrows.shp";




		ScenarioImpl scenario = new ScenarioImpl();
		Config conf = scenario.getConfig();
		new MatsimConfigReader(conf).readFile(config);
		new MatsimNetworkReader(scenario).readFile(network);

		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM47S);

		new RiskMap(scenario,outfile,outfile2,crs).run();
	}



}
