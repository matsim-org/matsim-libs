package playground.yu.utils.qgis;

import java.io.IOException;
import java.util.ArrayList;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.CRS;
import org.jfree.util.Log;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.gis.ShapeFileWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.yu.analysis.RouteSummaryTest.RouteSummary;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * This class is a copy of main() from
 * org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape of Mr.
 * Laemmeland can convert a MATSim-population to a QGIS .shp-file (acts or legs)
 * 
 * @author ychen
 * 
 */
public class Route2QGIS extends SelectedPlans2ESRIShape implements X2QGIS {
	private RouteSummary rs;
	protected NetworkLayer network;

	public Route2QGIS(Population population, CoordinateReferenceSystem crs,
			String outputDir, NetworkLayer network, RouteSummary routeSummary) {
		super(population, crs, outputDir);
		this.network = network;
		rs = routeSummary;
	}

	@Override
	protected void initFeatureType() {
		AttributeType[] attrAct = new AttributeType[7];
		attrAct[0] = DefaultAttributeTypeFactory.newAttributeType("Point",
				Point.class, true, null, null, this.getCrs());
		attrAct[1] = AttributeTypeFactory.newAttributeType("PERS_ID",
				String.class);
		attrAct[2] = AttributeTypeFactory
				.newAttributeType("TYPE", String.class);
		attrAct[3] = AttributeTypeFactory.newAttributeType("LINK_ID",
				String.class);
		attrAct[4] = AttributeTypeFactory.newAttributeType("START_TIME",
				Double.class);
		attrAct[5] = AttributeTypeFactory.newAttributeType("DUR", Double.class);
		attrAct[6] = AttributeTypeFactory.newAttributeType("END_TIME",
				Double.class);

		AttributeType[] attrLeg = new AttributeType[9];
		attrLeg[0] = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, this
						.getCrs());
		attrLeg[1] = AttributeTypeFactory.newAttributeType("PERS_ID",
				String.class);
		attrLeg[2] = AttributeTypeFactory
				.newAttributeType("NUM", Integer.class);
		attrLeg[3] = AttributeTypeFactory
				.newAttributeType("MODE", String.class);
		attrLeg[4] = AttributeTypeFactory.newAttributeType("DEP_TIME",
				Double.class);
		attrLeg[5] = AttributeTypeFactory.newAttributeType("TRAV_TIME",
				Double.class);
		attrLeg[6] = AttributeTypeFactory.newAttributeType("ARR_TIME",
				Double.class);
		attrLeg[7] = AttributeTypeFactory
				.newAttributeType("DIST", Double.class);
		attrLeg[8] = AttributeTypeFactory.newAttributeType("ROUTE_FLOW",
				Double.class);
		try {
			this.setFeatureTypeAct(FeatureTypeBuilder.newFeatureType(attrAct,
					"activity"));
			this.setFeatureTypeLeg(FeatureTypeBuilder.newFeatureType(attrLeg,
					"leg"));
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected Feature getLegFeature(Leg leg, String id) {
		Integer itg = rs.getRouteCountors().remove(leg.getRoute().getLinkIds());
		if (itg != null) {
			if (itg > 1) {
				Integer num = leg.getNum();
				BasicLeg.Mode mode = leg.getMode();
				Double depTime = leg.getDepartureTime();
				Double travTime = leg.getTravelTime();
				Double arrTime = leg.getArrivalTime();
				Double dist = leg.getRoute().getDist();
				Double routeFlows = itg.doubleValue();

				org.matsim.network.Link[] links = leg.getRoute().getLinkRoute();
				Coordinate[] coords = new Coordinate[(links.length + 1) * 2 + 1];
				double width = Math.min(500.0, itg.doubleValue());
				for (int i = 0; i < links.length; i++) {
					Coord c = links[i].getFromNode().getCoord();
					Coordinate cc = new Coordinate(c.getX(), c.getY());
					coords[i] = cc;
					Coord toCoord = links[i].getToNode().getCoord();
					Coordinate to = new Coordinate(toCoord.getX(), toCoord
							.getY());
					double xdiff = to.x - cc.x;
					double ydiff = to.y - cc.y;
					double denominator = Math.sqrt(xdiff * xdiff + ydiff
							* ydiff);
					double xwidth = width * ydiff / denominator;
					double ywidth = -width * xdiff / denominator;
					coords[coords.length - 2 - i] = new Coordinate(cc.x
							+ xwidth, cc.y + ywidth);
				}

				Coord c = links[links.length - 1].getToNode().getCoord();
				Coordinate cc = new Coordinate(c.getX(), c.getY());
				coords[links.length] = cc;
				Coordinate from = coords[links.length - 1];
				double xdiff = cc.x - from.x;
				double ydiff = cc.y - from.y;
				double denominator = Math.sqrt(xdiff * xdiff + ydiff * ydiff);
				double xwidth = width * ydiff / denominator;
				double ywidth = -width * xdiff / denominator;

				coords[links.length + 1] = new Coordinate(from.x + xwidth,
						from.y + ywidth);
				coords[coords.length - 1] = coords[0];
				LinearRing ls = this.getGeofac().createLinearRing(coords);
				Polygon p = new Polygon(ls, null, this.getGeofac());
				MultiPolygon mp = new MultiPolygon(new Polygon[] { p }, this
						.getGeofac());
				try {
					return this.getFeatureTypeLeg().create(
							new Object[] { mp, id, num, mode, depTime,
									travTime, arrTime, dist, routeFlows });
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	@Override
	protected void writeLegs() throws IOException {
		String outputFile = getOutputDir() + "/legs.shp";
		ArrayList<Feature> fts = new ArrayList<Feature>();
		for (Plan plan : this.getOutputSamplePlans()) {
			String id = plan.getPerson().getId().toString();
			LegIterator iter = plan.getIteratorLeg();
			while (iter.hasNext()) {
				Leg leg = (Leg) iter.next();
				if (leg.getRoute().getDist() > 0) {
					Feature ft = getLegFeature(leg, id);
					if (ft != null)
						fts.add(ft);
				}
			}
		}
		ShapeFileWriter.writeGeometries(fts, outputFile);
	}

	public static void main(final String[] args) {
		final String networkFilename = args[0];
		final String populationFilename = args[1];
		final String outputDir = args[2];

		Gbl.createConfig(null);
		Gbl.createWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFilename);

		Gbl.getWorld().setNetworkLayer(network);

		Population population = new Population();

		RouteSummary rs = new RouteSummary(outputDir + "/routeCompare.txt.gz");
		population.addAlgorithm(rs);

		System.out.println("-->reading plansfile: " + populationFilename);
		new MatsimPopulationReader(population).readFile(populationFilename);

		population.runAlgorithms();
		rs.write();
		rs.end();

		CoordinateReferenceSystem crs;
		try {
			crs = CRS.parseWKT(ch1903);
			Route2QGIS sp = new Route2QGIS(population, crs, outputDir, network,
					rs);
			sp.setOutputSample(// 0.05
					1);
			sp.setActBlurFactor(100);
			sp.setLegBlurFactor(100);
			sp.setWriteActs(false);
			sp.setWriteLegs(true);
			sp.write();
		} catch (FactoryException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			Log.error(e.getMessage(), e);
		}
	}
}
