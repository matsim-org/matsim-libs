package playground.dhosse.scenarios.generic;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class Configuration {

	//TAGS
	private static final String SEP = "\t";
	private static final String COMMENT = "#";
	private static final String OSM_FILE = "osmFile";
	private static final String SURVEY_AREA_ID = "surveyAreaId";
	private static final String VICINITY_IDS = "vicinityIds";
	private static final String CRS = "coordinateSystem";
	private static final String WORKING_DIR = "workingDirectory";
	
	//MEMBERS
	private String osmFile;
	private String surveyAreaId;
	private String[] vicinityIds;
	private String crs;
	private String workingDirectory;
	
	Configuration(String file){
		
		readConfigurationFile(file);
		
	}
	
	private void readConfigurationFile(String file){
		
		BufferedReader reader = IOUtils.getBufferedReader(file);
		
		String line = null;
		
		try {
			
			while((line = reader.readLine()) != null){
				
				if(!line.startsWith(COMMENT)){
					
					String[] lineParts = line.split(SEP);
					
					if(OSM_FILE.equals(lineParts[0])){
						
						this.osmFile = lineParts[1];
						
					} else if(SURVEY_AREA_ID.equals(lineParts[0])){
						
						this.surveyAreaId = lineParts[1];
						
					} else if(VICINITY_IDS.equals(lineParts[0])){
						
						this.vicinityIds = lineParts[1].split(",");
						
					} else if(CRS.equals(lineParts[0])){
						
						this.crs = lineParts[1];
						
					} else if(WORKING_DIR.equals(lineParts[0])){
						
						this.workingDirectory = lineParts[1];
						
					}
					
				}
				
			}
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	public String getOsmFile(){
		return this.osmFile;
	}

	public String getSurveyAreaId() {
		return this.surveyAreaId;
	}

	public String[] getVicinityIds() {
		return this.vicinityIds;
	}

	public String getCrs() {
		return crs;
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}
	
}
