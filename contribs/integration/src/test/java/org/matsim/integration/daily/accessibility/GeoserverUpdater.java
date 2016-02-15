package org.matsim.integration.daily.accessibility;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacility;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

class GeoserverUpdater implements FacilityDataExchangeInterface {

	static Logger log = Logger.getLogger(GeoserverUpdater.class);

	private String crs;
	private String name;


	public GeoserverUpdater (String crs, String name) {
		this.crs = crs;
		this.name = name;
	}

	private Map<Tuple<ActivityFacility, Double>, Map<Modes4Accessibility,Double>> accessibilitiesMap = new HashMap<>() ;

	@Override
	public void setFacilityAccessibilities(ActivityFacility measurePoint, Double timeOfDay,
			Map<Modes4Accessibility, Double> accessibilities) {
		accessibilitiesMap.put( new Tuple<>(measurePoint, timeOfDay), accessibilities ) ;
	}

	@Override
	public void finish() {
		// lockedForAdditionalFacilityData = true;

//		log.info("starting setAndProcessSpatialGrids ...");
		log.info("starting setAndProcess ??? ...");

		GeometryFactory geometryFactory = new GeometryFactory();
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName(name);
		builder.setCRS(MGC.getCRS(TransformationFactory.WGS84));

		builder.add("the_geom", Point.class);
		builder.add("x", Double.class);
		builder.add("y", Double.class);
		builder.add("time", Double.class); // new since 2015-12-02

		for (Modes4Accessibility mode : Modes4Accessibility.values()) {
			builder.add(mode.toString(), Double.class);
		}

//		for ( ActivityFacilities facilities : additionalFacilityData ) {
//			b.add( facilities.getName(), Double.class );
//		}
		// yyyyyy add population here

		SimpleFeatureType featureType = builder.buildFeatureType();
		DefaultFeatureCollection collection = new DefaultFeatureCollection("internal", featureType);

		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

//		final SpatialGrid spatialGrid = spatialGrids.get(Modes4Accessibility.freeSpeed);
		// yy for time being, have to assume that this is always there
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(this.crs, TransformationFactory.WGS84);

		for (Entry<Tuple<ActivityFacility, Double>, Map<Modes4Accessibility, Double>> entry : accessibilitiesMap.entrySet()) {
			ActivityFacility facility = entry.getKey().getFirst();
			Double timeOfDay = entry.getKey().getSecond();
			Coord coord = facility.getCoord() ;

			featureBuilder.add(geometryFactory.createPoint(MGC.coord2Coordinate(transformation.transform(coord))));
			featureBuilder.add(coord.getX());
			featureBuilder.add(coord.getY());
			featureBuilder.add(timeOfDay);

			Map<Modes4Accessibility, Double> accessibilities = entry.getValue();
			for (Modes4Accessibility mode : Modes4Accessibility.values()) {
				Double accessibility = accessibilities.get(mode);
				if (accessibility != null && !Double.isNaN(accessibility)) {
					featureBuilder.add(accessibility);
				} else {
					featureBuilder.add(null);
				}
			}

			// yyyyyy write population density here.  Probably not aggregated to grid.


			SimpleFeature feature = featureBuilder.buildFeature(null);
			collection.add(feature);
		}

		try {
			Map<String,Object> params = new HashMap<>();
			params.put( "dbtype", "postgis");
			params.put( "host", "geo.vsp.tu-berlin.de");
			params.put( "port", 5432);
			params.put( "schema", "public");
			params.put( "database", "vspgeodb");
			params.put( "user", "vsppostgres");
			params.put( "passwd", "jafs30_A");
			DataStore dataStore = DataStoreFinder.getDataStore(params);
			try {
				dataStore.removeSchema(name);
			} catch (IllegalArgumentException e) {
				log.warn("Could not remove schema. Perhaps it does not exist. Probably doesn't matter.");
			}
			dataStore.createSchema(featureType);
			SimpleFeatureStore featureStore = (SimpleFeatureStore) dataStore.getFeatureSource(name);
			// ---
			Transaction t = new DefaultTransaction(); // new
			featureStore.setTransaction(t); // new
			// ---
			featureStore.addFeatures(collection);
			// ---
			// new below this line
			try {
				t.commit();
			} catch ( IOException ex ) {
				// something went wrong;
				ex.printStackTrace();
				t.rollback();
			} finally {
				t.close();
			}
			dataStore.dispose() ;
			// new above this line
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
//		log.info("ending setAndProcessSpatialGrids.");
		log.info("ending setAndProcess ???.");

		// re-publish layer using the REST api (of geoserver; the above is the postgis db) if we want to automatically recompute the
		// bounding box.  mz & kai, nov'15

	}

}