package gis.mapinfo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;



public class MapInfoFileReader {
	
	private static Logger logger = Logger.getLogger(MapInfoFileReader.class);
	
	private String featureDataFile;
	
	private String featureGeoFile;

	private FeatureCollection features;
	
	private List<FeatureData> featureDataList = new ArrayList<FeatureData>();
	
	private List<FeatureGeo> featureGeoList = new ArrayList<FeatureGeo>();
	
	private MapInfoMetaData metaData;
	
	public MapInfoFileReader(FeatureCollection features){
		this.features = features;
		metaData = features.getMetaData();
	}
		
	public void setDatafile(String dataFile) {
		this.featureDataFile = dataFile;
	}

	public void setGeofile(String geoFile) {
		this.featureGeoFile = geoFile;
	}

	public void run() throws FileNotFoundException, IOException{
		readGeoFile();
		readDataFile();
		makeFeature();
		
	}

	private void makeFeature() {
		verify();
		for(int i=0;i<featureDataList.size();i++){
			Feature feature = new Feature(featureGeoList.get(i), featureDataList.get(i), new SimpleLayout());
			features.getFeatures().add(feature);
		}
	}

	private void verify() {
		if(featureDataList.size() != featureGeoList.size()){
			throw new RuntimeException("number of data not equal to number of geoData");
		}
		return;
	}

	private void readGeoFile() throws FileNotFoundException, IOException {
		logger.info("read geoData from " + featureGeoFile);
		BufferedReader reader = IOUtils.getBufferedReader(featureGeoFile);
		String line = null;
		String head = null;
		int featureCounter = 0;
		Integer nOfColumns = null;
		boolean readColumns = false;
		boolean readHead = true;
		boolean readFeature = false;
		FeatureGeo currentFeatureGeo = null;
		int nOfFeatureNodes=0;
		int columnsCounter = 0;
		
		while((line = reader.readLine()) != null){
			line = line.replace("\n", "");
			line = line.trim();
			String[] lineElements = line.split(" ");
			if(lineElements.length > 0){
				if(readColumns && columnsCounter<nOfColumns){
					metaData.getColumnNames().add(lineElements[0].toUpperCase());
					String columnValueType = null;
					for(int i=1;i<lineElements.length;i++){
						if(columnValueType == null){
							columnValueType = lineElements[i];
						}
						else{
							columnValueType += lineElements[i];
						}
					}
					metaData.getColumnValueTypes().add(columnValueType);
					columnsCounter++;
					continue;
				}
				if(lineElements[0].equals((String)"CoordSys")){
					
				}
				if(lineElements[0].equals((String)"Columns")){
					nOfColumns = Integer.parseInt(lineElements[1]);
					readColumns = true;
					readHead =false;
					continue;
				}
				if(readHead){
					if(head == null){
						head = line + "\n";
					}
					else{
						head += line + "\n";
					}
					continue;
				}
				if(lineElements[0].equals((String)"Data")){
					readHead = false;
					readColumns = false;
					continue;
				}
				if(lineElements[0].equals((String)"Point")){
					Coord firstNodeCoord = new CoordImpl(Double.parseDouble(lineElements[1]),Double.parseDouble(lineElements[2]));
					Point point = new Point();
					point.getNodes().add(new Node(firstNodeCoord));
					featureGeoList.add(point);
					featureCounter++;
					continue;
				}
				if(lineElements[0].equals((String)"Line")){
					Coord firstNodeCoord = new CoordImpl(Double.parseDouble(lineElements[1]),Double.parseDouble(lineElements[2]));
					Coord secondNodeCoord = new CoordImpl(Double.parseDouble(lineElements[3]),Double.parseDouble(lineElements[4]));
					Line lineGeo = new Line();
					lineGeo.getNodes().add(new Node(firstNodeCoord));
					lineGeo.getNodes().add(new Node(secondNodeCoord));
					featureGeoList.add(lineGeo);
					featureCounter++;
					continue;
				}
				if(lineElements[0].equals((String)"Pline")){
					readFeature = true;
					nOfFeatureNodes = Integer.valueOf(lineElements[1]);
					currentFeatureGeo = new PLine();
					featureGeoList.add(currentFeatureGeo);
					featureCounter++;
					continue;
				}
				if(readFeature && currentFeatureGeo.getNodes().size()<nOfFeatureNodes){
					Coord nodeCoord = new CoordImpl(Double.parseDouble(lineElements[0]),Double.parseDouble(lineElements[1]));
					currentFeatureGeo.getNodes().add(new Node(nodeCoord));
					continue;
				}
			}
		}
		metaData.setHead(head);
		reader.close();
		logger.info(featureCounter + " geoData read");
	}

	private void readDataFile() throws FileNotFoundException, IOException {
		logger.info("read data from " + featureDataFile);
		BufferedReader reader = IOUtils.getBufferedReader(featureDataFile);
		String line = null;
		int counter = 0;
		while((line = reader.readLine()) != null){
			line = line.replace("\n", "");
			line = line.replace("\"", "");
			line = line.trim();
			String[] tokens = line.split(",");
			FeatureData edgeData = new FeatureData();
			for(int i=0; i<metaData.getColumnNames().size();i++){
				edgeData.getAttributes().put(metaData.getColumnNames().get(i), tokens[i]);
			}
			featureDataList.add(edgeData);
			counter++;
		}
		reader.close();
		logger.info(counter + " data read");
	}
	
	
	
}
