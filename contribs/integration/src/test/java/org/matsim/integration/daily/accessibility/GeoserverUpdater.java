package org.matsim.integration.daily.accessibility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

class GeoserverUpdater implements FacilityDataExchangeInterface {

	private static boolean lockedForAdditionalFacilityData = false;

	static Logger log = Logger.getLogger(GeoserverUpdater.class);

	private String crs;
	private String name;


	public GeoserverUpdater (String crs, String name) {
		this.crs = crs;
		this.name = name;
	}


	private Map< Tuple<ActivityFacility, Double> , Map<Modes4Accessibility,Double > > map = new HashMap<>() ;

	private Collection<ActivityFacilities> additionalFacilityData = new ArrayList<>() ;

	@Override
	public void setFacilityAccessibilities(ActivityFacility measurePoint, Double timeOfDay,
			Map<Modes4Accessibility, Double> accessibilities) {
		map.put( new Tuple<>( measurePoint, timeOfDay), accessibilities ) ;
	}



//	public final void setAndProcessSpatialGrids(Map<Modes4Accessibility, SpatialGrid> spatialGrids) 
	public final void setAndProcessSpatialGrids( List<Modes4Accessibility> modes ) { 
		lockedForAdditionalFacilityData = true ;
		
		log.info("starting setAndProcessSpatialGrids ...");
		GeometryFactory fac = new GeometryFactory();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName(name);
		b.setCRS(MGC.getCRS(TransformationFactory.WGS84));
		b.add("the_geom", Point.class);
		b.add("x", Double.class);
		b.add("y", Double.class);
		b.add("time", Double.class);
		for (Modes4Accessibility mode : modes ) {
			b.add(mode.toString(), Double.class);
		}
//		for ( ActivityFacilities facilities : additionalFacilityData ) {
//			b.add( facilities.getName(), Double.class );
//		}
		// yyyyyy add population here
		SimpleFeatureType featureType = b.buildFeatureType();
		DefaultFeatureCollection collection = new DefaultFeatureCollection("internal", featureType);

		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
//		final SpatialGrid spatialGrid = spatialGrids.get(Modes4Accessibility.freeSpeed);
		// yy for time being, have to assume that this is always there
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(this.crs, TransformationFactory.WGS84);

		for ( Entry<Tuple<ActivityFacility, Double>, Map<Modes4Accessibility, Double>> entry : map.entrySet() ) {
			ActivityFacility facility = entry.getKey().getFirst();
			Coord origCoord = facility.getCoord() ;

			featureBuilder.add(fac.createPoint(MGC.coord2Coordinate(transformation.transform(origCoord))));

			featureBuilder.add(origCoord.getX()) ;  featureBuilder.add( origCoord.getY() ) ;

			Double timeOfDay = entry.getKey().getSecond() ;
			featureBuilder.add( timeOfDay );

			Map<Modes4Accessibility, Double> accessibilities = entry.getValue() ;
			for ( Modes4Accessibility mode : modes ) {
				double accessibility = accessibilities.get(mode) ;
				if (Double.isNaN(accessibility)) {
					featureBuilder.add(null);
				} else {
					featureBuilder.add(accessibility);
				}
			}
			
			// yyyyyy write population density here.  Probably not aggregated to grid.
			
			SimpleFeature feature = featureBuilder.buildFeature(null);
			collection.add(feature);
		}

		//		for(double y = spatialGrid.getYmin(); y <= spatialGrid.getYmax(); y += spatialGrid.getResolution()) {
		//			for(double x = spatialGrid.getXmin(); x <= spatialGrid.getXmax(); x += spatialGrid.getResolution()) {
		//				Coord saAlbersCoord = new Coord(x + 0.5 * spatialGrid.getResolution(), y + 0.5 * spatialGrid.getResolution());
		//				Coord wgs84Coord = transformation.transform(saAlbersCoord);
		//				featureBuilder.add(fac.createPoint(MGC.coord2Coordinate(wgs84Coord)));
		//				featureBuilder.add(x);
		//				featureBuilder.add(y);
		//				for (Modes4Accessibility mode : spatialGrids.keySet()) {
		//					final SpatialGrid theSpatialGrid = spatialGrids.get(mode);
		//					final double value = theSpatialGrid.getValue(x, y);
		//					if (Double.isNaN(value)) {
		//						featureBuilder.add(null);
		//					} else {
		//						featureBuilder.add(value);
		//					}
		//				}
		//				SimpleFeature feature = featureBuilder.buildFeature(null);
		//				collection.add(feature);
		//			}
		//		}

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
		log.info("ending setAndProcessSpatialGrids.");

		// re-publish layer using the REST api (of geoserver; the above is the postgis db) if we want to automatically recompute the 
		// bounding box.  mz & kai, nov'15
	}



	/**
	 * I wanted to plot something like (max(acc)-acc)*population.  For that, I needed "population" at the x/y coordinates.
	 * This is the mechanics via which I inserted that. (The computation is then done in postprocessing.)
	 * <p/>
	 * You can add arbitrary ActivityFacilities containers here.  They will be aggregated to the grid points, and then written to
	 * file as additional column.
	 */
	public void addAdditionalFacilityData(ActivityFacilities facilities ) {
		log.warn("changed this data flow (by adding the _cnt_ column) but did not test.  If it works, please remove this warning. kai, mar'14") ;

		if ( this.lockedForAdditionalFacilityData ) {
			throw new RuntimeException("too late for adding additional facility data; spatial grids have already been generated.  Needs"
					+ " to be called before generating the spatial grids.  (This design should be improved ..)") ;
		}
		if ( facilities.getName()==null || facilities.getName().equals("") ) {
			throw new RuntimeException("cannot add unnamed facility containers here since we need a key to find them again") ;
		}
		for ( ActivityFacilities existingFacilities : this.additionalFacilityData ) {
			if ( existingFacilities.getName().equals( facilities.getName() ) ) {
				throw new RuntimeException("additional facilities under the name of + " + facilities.getName() + 
						" already exist; cannot add additional facilities under the same name twice.") ;
			}
		}

		this.additionalFacilityData.add( facilities ) ;
	}

}
