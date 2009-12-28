package playground.yu.utils.qgis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.CRS;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.yu.analysis.RouteSummaryTest.RouteSummary;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * This class is a copy of main() from
 * org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape of Mr.
 * Laemmeland can convert a MATSim-population to a QGIS .shp-file (acts or legs)
 * 
 * @author ychen
 * 
 */
public class Route2QGIS extends SelectedPlans2ESRIShapeChanged implements
		X2QGIS {
	protected Map<List<Id>, Integer> routeCounters;
	protected NetworkLayer network;
	private FeatureType featureTypeRoute;
	private boolean writeRoutes = true;

	public Route2QGIS(PopulationImpl population,
			final CoordinateReferenceSystem crs, final String outputDir,
			final NetworkLayer network,
			final Map<List<Id>, Integer> routeCounters) {
		super(population, crs, outputDir);
		this.network = network;
		this.routeCounters = routeCounters;
	}

	@Override
	protected void initFeatureType() {
		AttributeType[] attrRoute = new AttributeType[2];
		attrRoute[0] = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, this
						.getCrs());
		attrRoute[1] = AttributeTypeFactory.newAttributeType("ROUTE_FLOW",
				Double.class);
		try {
			this.setFeatureTypeRoute(FeatureTypeBuilder.newFeatureType(
					attrRoute, "route"));
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}

	public void setFeatureTypeRoute(final FeatureType featureTypeRoute) {
		this.featureTypeRoute = featureTypeRoute;
	}

	protected Feature getRouteFeature(final List<Id> routeLinkIds) {
		Integer routeFlows = this.routeCounters.get(routeLinkIds);
		if (routeFlows != null)
			if (routeFlows.intValue() > 1) {
				Coordinate[] coordinates = new Coordinate[(routeLinkIds.size() + 1) * 2 + 1];
				double width = 5.0 * Math.min(250.0, routeFlows.doubleValue());
				coordinates = calculateCoordinates(coordinates, width,
						routeLinkIds);
				try {
					return getFeatureTypeRoute()
							.create(
									new Object[] {
											new MultiPolygon(
													new Polygon[] { new Polygon(
															getGeofac()
																	.createLinearRing(
																			coordinates),
															null, getGeofac()) },
													this.getGeofac()),
											routeFlows.doubleValue() });
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			}
		return null;
	}

	protected Coordinate[] calculateCoordinates(Coordinate[] coordinates,
			double width, List<Id> routeLinkIds) {
		for (int i = 0; i < routeLinkIds.size(); i++) {
			LinkImpl l = this.network.getLink(routeLinkIds.get(i));
			Coord c = l.getFromNode().getCoord();
			Coordinate cdn = new Coordinate(c.getX(), c.getY());
			coordinates[i] = cdn;
			Coord toCoord = l.getToNode().getCoord();
			Coordinate to = new Coordinate(toCoord.getX(), toCoord.getY());
			double xdiff = to.x - cdn.x;
			double ydiff = to.y - cdn.y;
			double denominator = Math.sqrt(xdiff * xdiff + ydiff * ydiff);
			coordinates[coordinates.length - 2 - i] = new Coordinate(cdn.x
					+ width * ydiff / denominator, cdn.y - width * xdiff
					/ denominator);
		}

		Coord c = this.network.getLink(
				routeLinkIds.get(routeLinkIds.size() - 1)).getToNode()
				.getCoord();
		Coordinate cdn = new Coordinate(c.getX(), c.getY());
		coordinates[routeLinkIds.size()] = cdn;
		Coordinate from = coordinates[routeLinkIds.size() - 1];
		double xdiff = cdn.x - from.x;
		double ydiff = cdn.y - from.y;
		double denominator = Math.sqrt(xdiff * xdiff + ydiff * ydiff);
		coordinates[routeLinkIds.size() + 1] = new Coordinate(from.x + width
				* ydiff / denominator, from.y - width * xdiff / denominator);
		coordinates[coordinates.length - 1] = coordinates[0];

		return coordinates;
	}

	protected FeatureType getFeatureTypeRoute() {
		return this.featureTypeRoute;
	}

	protected void writeRoutes() throws IOException {
		ArrayList<Feature> fts = new ArrayList<Feature>();
		for (List<Id> routeLinkIds : this.routeCounters.keySet()) {
			Feature ft = getRouteFeature(routeLinkIds);
			if (ft != null)
				fts.add(ft);
		}
		ShapeFileWriter.writeGeometries(fts, getOutputDir() + "/routes.shp");
	}

	@Override
	public void write() throws IOException {
		if (this.writeRoutes) {
			writeRoutes();
		}
	}

	public static void main(final String[] args) {
		final String networkFilename = args[0];
		final String populationFilename = args[1];
		final String outputDir = args[2];

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFilename);

		PopulationImpl population = new PopulationImpl();

		RouteSummary rs = new RouteSummary(outputDir + "/routeCompare.txt.gz");

		System.out.println("-->reading plansfile: " + populationFilename);
		new MatsimPopulationReader(population, network)
				.readFile(populationFilename);

		rs.run(population);
		rs.write();
		rs.end();

		CoordinateReferenceSystem crs;
		try {
			crs = CRS.parseWKT(ch1903);
			Route2QGIS r2q = new Route2QGIS(population, crs, outputDir, network, rs
					.getRouteCounters());
			r2q.setOutputSample(// 0.05
					1);
			r2q.setWriteActs(false);
			r2q.setWriteRoutes(true);
			r2q.write();
		} catch (FactoryException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			Log.error(e.getMessage(), e);
		}
	}

	protected void setWriteRoutes(final boolean writeRoutes) {
		this.writeRoutes = writeRoutes;
	}
}
