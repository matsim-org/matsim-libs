package org.matsim.integration.daily.accessibility;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class GeoserverUpdater implements SpatialGridDataExchangeInterface {

	static Logger log = Logger.getLogger(GeoserverUpdater.class);

	@Override
	public void setAndProcessSpatialGrids(Map<Modes4Accessibility, SpatialGrid> spatialGrids) {
		GeometryFactory fac = new GeometryFactory();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("accessibilities");
		b.setCRS(MGC.getCRS(TransformationFactory.WGS84));
		b.add("the_geom", Point.class);
		b.add("x", Double.class);
		b.add("y", Double.class);
		for (Modes4Accessibility mode : spatialGrids.keySet()) {
			b.add(mode.toString(), Double.class);
		}
		SimpleFeatureType featureType = b.buildFeatureType();
		DefaultFeatureCollection collection = new DefaultFeatureCollection("internal", featureType);

		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		final SpatialGrid spatialGrid = spatialGrids.get(Modes4Accessibility.freeSpeed);
		// yy for time being, have to assume that this is always there
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SA_Albers, TransformationFactory.WGS84);

		for(double y = spatialGrid.getYmin(); y <= spatialGrid.getYmax(); y += spatialGrid.getResolution()) {
			for(double x = spatialGrid.getXmin(); x <= spatialGrid.getXmax(); x += spatialGrid.getResolution()) {
				Coord saAlbersCoord = new Coord(x + 0.5 * spatialGrid.getResolution(), y + 0.5 * spatialGrid.getResolution());
				Coord wgs84Coord = transformation.transform(saAlbersCoord);
				featureBuilder.add(fac.createPoint(MGC.coord2Coordinate(wgs84Coord)));
				featureBuilder.add(x);
				featureBuilder.add(y);
				for (Modes4Accessibility mode : spatialGrids.keySet()) {
					final SpatialGrid theSpatialGrid = spatialGrids.get(mode);
					final double value = theSpatialGrid.getValue(x, y);
					if (Double.isNaN(value)) {
						featureBuilder.add(null);
					} else {
						featureBuilder.add(value);
					}
				}
				SimpleFeature feature = featureBuilder.buildFeature(null);
				collection.add(feature);
			}
		}

		try {
			Map<String,Object> params = new HashMap<>();
			params.put( "dbtype", "postgis");
			params.put( "host", "wiki.vsp.tu-berlin.de");
			params.put( "port", 5432);
			params.put( "schema", "public");
			params.put( "database", "vspgeo");
			params.put( "user", "postgres");
			params.put( "passwd", "jafs30_A");
			DataStore dataStore = DataStoreFinder.getDataStore(params);
			try {
				dataStore.removeSchema("accessibilities");
			} catch (IllegalArgumentException e) {
				log.warn("Could not remove schema. Perhaps it does not exist. Probably doesn't matter.");
			}
			dataStore.createSchema(featureType);
			SimpleFeatureStore featureStore = (SimpleFeatureStore) dataStore.getFeatureSource("accessibilities");
			featureStore.addFeatures(collection);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

}
