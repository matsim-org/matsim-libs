package playground.wrashid.oswald.quartiere;

import java.awt.Polygon;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File file = new File("C:/ETHZEclipseData/static data/parking/zürich city/stadtquartiere/Zurich.shp");
		Polygon polygon = null;
		try {
			Map connect = new HashMap();
			connect.put("url", file.toURL());

			DataStore dataStore = DataStoreFinder.getDataStore(connect);
			String[] typeNames = dataStore.getTypeNames();
			String typeName = typeNames[0];

			System.out.println("Reading content " + typeName);

			FeatureSource featureSource = dataStore.getFeatureSource(typeName);
			FeatureCollection collection = featureSource.getFeatures();
			FeatureIterator iterator = collection.features();
			LinkedList<Geometry> list = null;

			try {
				while (iterator.hasNext()) {
					polygon = new Polygon();

					Feature feature = iterator.next();
					Geometry sourceGeometry = feature.getDefaultGeometry();

					// System.out.println(feature.getFeatureType());
					// System.out.println(feature.toString());
					// System.out.println(feature.getID());
					// System.out.println(feature.getAttribute("NAME"));

					Coordinate[] coordinates = sourceGeometry.getCoordinates();
					for (int i = 0; i < coordinates.length; i++) {
						polygon.addPoint((int) Math.round(coordinates[i].x), (int) Math.round(coordinates[i].y));
					}

					// Coordinate[] coordinates =
					// sourceGeometry.getCoordinates();
					// System.out.println("f: " + coordinates[0]);
					// System.out.println("l: " +
					// coordinates[coordinates.length-1]);

					if (polygon.contains(685340, 245700)) {
						System.out.println(feature.getAttribute("NAME"));
					}

				}
			} finally {
				iterator.close();
			}

		} catch (Throwable e) {
			e.printStackTrace();

		}

	}

}
