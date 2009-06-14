package playground.gregor.flooding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class FloodLineShape {

	private final String netcdf;
	private final String output;
	private FeatureType featureType;
	private GeometryFactory geofac;

	public FloodLineShape(String netcdf, String output) {
		this.netcdf = netcdf;
		this.output = output;
	}

	private void run() {
		initFeatureType();
		FloodingLine fl = new FloodingLine(this.netcdf);
		Collection<Feature> fts = new ArrayList<Feature>();
		int singletons = 0;
		for (int i = 0; i <= 120; i += 1) {
			List<ArrayList<Coordinate>> allCoords = fl.getFloodLine(i);
			for (ArrayList<Coordinate> coords : allCoords) {

				if (coords.size() == 1) {
					singletons++;
					continue;
				}
				Coordinate[] coordinates = new Coordinate[coords.size()];
				for (int j = 0; j < coords.size(); j++) {
					coordinates[j] = coords.get(j);
				}
				LineString ls = this.geofac.createLineString(coordinates);
				try {
					fts.add(this.featureType.create(new Object[] { ls, i }));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("singletons:" + singletons);
		try {
			ShapeFileWriter.writeGeometries(fts, this.output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initFeatureType() {
		CoordinateReferenceSystem crs = MGC.getCRS(Gbl.getConfig().global()
				.getCoordinateSystem());
		AttributeType[] attribs = new AttributeType[2];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("LineString",
				LineString.class, true, null, null, crs);
		attribs[1] = AttributeTypeFactory.newAttributeType("time",
				Integer.class);

		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attribs,
					"node");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		this.geofac = new GeometryFactory();
	}

	public static void main(String[] args) {
		// String netcdf = "../../inputs/flooding/flooding_old.sww";
		String netcdf = "test/input/playground/gregor/data/flooding.sww";
		String output = "./plans/floodLineSmall.shp";
		Scenario scenario = new ScenarioImpl();
		scenario.getConfig().global().setCoordinateSystem("WGS84_UTM47S");
		FloodLineShape fls = new FloodLineShape(netcdf, output);
		fls.run();
	}

}
