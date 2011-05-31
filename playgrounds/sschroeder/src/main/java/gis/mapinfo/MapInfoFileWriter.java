/**
 * 
 */
package gis.mapinfo;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;



/**
 * @author stefan
 *
 */
public class MapInfoFileWriter {
	
	private static Logger logger = Logger.getLogger(MapInfoFileWriter.class);
	
	private FeatureCollection features;
	
	private MapInfoMetaData metaData;

	private String midFileName;
	
	private String mifFileName;
	
	public MapInfoFileWriter(FeatureCollection features) {
		super();
		this.features = features;
		this.metaData = features.getMetaData();
	}

	public void run() throws FileNotFoundException, IOException{
		writeMIF();
		writeMID();
	}

	public void setMidFile(String midFile) {
		this.midFileName = midFile;
	}

	public void setMifFile(String mifFile) {
		this.mifFileName = mifFile;
	}
	
	private void writeMID() throws FileNotFoundException, IOException {
		logger.info("write data in " + midFileName);
		BufferedWriter writer = IOUtils.getBufferedWriter(midFileName);
		for(Feature feature : features.getFeatures()){
			FeatureData edgeData = feature.getFeatureData();
			boolean firstElement = true;
			for(String key : metaData.getColumnNames()){
				if(firstElement){
					writer.write(edgeData.getAttributes().get(key));
					firstElement = false;
				}
				else{
					writer.write(",");
					writer.write(edgeData.getAttributes().get(key));
				}	
			}
			writer.write(getLineEnd());
		}
		writer.close();
	}

	private void writeMIF() throws FileNotFoundException, IOException {	
		logger.info("write geoData in " + mifFileName);
		BufferedWriter writer = IOUtils.getBufferedWriter(mifFileName);
		writer.write(metaData.getHead());
		writer.write("Columns " + metaData.getColumnNames().size());
		writer.write(getLineEnd());
		for(int i=0;i<metaData.getColumnNames().size();i++){
			writer.write(metaData.getColumnNames().get(i) + " " + metaData.getColumnValueTypes().get(i));
			writer.write(getLineEnd());
		}
		writer.write("Data");
		writer.write(getLineEnd());
		writer.write(getLineEnd());
		for(Feature feature : features.getFeatures()){
			FeatureGeo featureGeo = feature.getFeatureGeo();
			if(featureGeo instanceof Point){
				writer.write(featureGeo.getTYPE() + " ");
				for(Node node : featureGeo.getNodes()){
					writer.write(node.getCoord().getX() + " " + node.getCoord().getY() + " ");
				}
				writer.write(getLineEnd());
				writer.write(feature.getFeatureLayout().getLayout());
				writer.write(getLineEnd());
				continue;
			}
			if(featureGeo instanceof Line){
				writer.write(featureGeo.getTYPE() + " ");
				for(Node node : featureGeo.getNodes()){
					writer.write(node.getCoord().getX() + " " + node.getCoord().getY() + " ");
				}
				writer.write(getLineEnd());
				writer.write(feature.getFeatureLayout().getLayout());
				writer.write(getLineEnd());
				continue;
			}
			if(featureGeo instanceof PLine){
				writer.write(featureGeo.getTYPE() + " " + featureGeo.getNodes().size() + getLineEnd());
				for(Node node : featureGeo.getNodes()){
					writer.write(node.getCoord().getX() + " " + node.getCoord().getY() + getLineEnd());
				}
				writer.write(feature.getFeatureLayout().getLayout());
				writer.write(getLineEnd());
				continue;
			}
			
		}
		writer.close();
	}

	private String getLineEnd() {
		return "\n";
	}
	
	

}
