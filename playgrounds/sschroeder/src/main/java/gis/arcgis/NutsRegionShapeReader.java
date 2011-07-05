package gis.arcgis;

import java.io.IOException;
import java.util.List;

import kid.GeotoolsTransformation;
import kid.filter.DefaultFeatureFilter;
import kid.filter.SimpleFeatureFilter;

import org.apache.log4j.Logger;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import utils.RegionSchema;


public class NutsRegionShapeReader {
	
	private static Logger logger = Logger.getLogger(NutsRegionShapeReader.class);
	
	private List<SimpleFeature> features;
	
	private SimpleFeatureFilter filter = new DefaultFeatureFilter();
	
	private GeotoolsTransformation transformation;

	public NutsRegionShapeReader(List<SimpleFeature> features, GeotoolsTransformation transformation) {
		this.features = features;
		this.transformation = transformation;
	}
	
	public NutsRegionShapeReader(List<SimpleFeature> features, SimpleFeatureFilter filter, GeotoolsTransformation transformation) {
		this.features = features;
		this.filter = filter;
		this.transformation = transformation;
	}
	
	public void read(String fileName){
		logger.info("read regions");
		ShapeFileReader reader = new ShapeFileReader();
		try {
			reader.readFileAndInitialize(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		}
		FeatureCollection<SimpleFeatureType,SimpleFeature> featureCollection = reader.getFeatureCollection();
		FeatureIterator<SimpleFeature> featureIter = featureCollection.features();
		while(featureIter.hasNext()){
			SimpleFeature f = featureIter.next();
			SimpleFeature newFeature;
			if(transformation != null){
				newFeature = transform(f);
			}
			else{
				newFeature = f;
			}
			if(filter.judge(newFeature)){
				logger.debug(newFeature.getProperty(RegionSchema.REGION_NAME).getValue().toString());
				features.add(newFeature);
			}
		}
	}

	private SimpleFeature transform(SimpleFeature f) {
		return transformation.transformFeature(f);
	}

	public void setFilter(SimpleFeatureFilter filter) {
		this.filter = filter;
	}
}
