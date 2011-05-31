/**
 * 
 */
package gis.arcgis;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;


/**
 * @author stefan
 *
 */
public class ShapeFileWriter {
	
	private FeatureCollection<SimpleFeatureType, SimpleFeature> features;
	
	public ShapeFileWriter(
			FeatureCollection<SimpleFeatureType, SimpleFeature> simpleFeatureCollection) {
		super();
		this.features = simpleFeatureCollection;
	}

	public void writeFeatures(String filename) throws IOException{
		 /*
         * Get an output file name and create the new shapefile
         */
        verify(features);
		
		File newFile = new File(filename);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", newFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        
        newDataStore.createSchema(((SimpleFeature)features.iterator().next()).getFeatureType());
        
        /*
         * Write the features to the shapefile
         */
        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        FeatureSource featureSource = newDataStore.getFeatureSource(typeName);

        if (featureSource instanceof FeatureStore) {
        	FeatureStore featureStore = (FeatureStore) featureSource;

            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(features);
                transaction.commit();

            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();

            } finally {
                transaction.close();
            }
            //System.exit(0); 
        } else {
            System.out.println(typeName + " does not support read/write access");
            System.exit(1);
        }
	}

	private static void verify(FeatureCollection<SimpleFeatureType, SimpleFeature> features) {
		if(features.isEmpty()){
			throw new RuntimeException("no features => cannot geocode");
		}
		
	}
	



}
