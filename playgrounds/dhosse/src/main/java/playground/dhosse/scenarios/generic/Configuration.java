package playground.dhosse.scenarios.generic;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class Configuration {

	//TAGS
	private static final String SEP = "\t";
	private static final String COMMENT = "#";
	
	private static final String OSM_FILE = "osmFile";
	private static final String SURVEY_AREA_IDS = "surveyAreaIds";
	private static final String CRS = "coordinateSystem";
	private static final String WORKING_DIR = "workingDirectory";
	private static final String INPUT_COMMUTER_FILE = "inputCommuterFile";
	private static final String INPUT_REVERSE_COMMUTER_FILE = "inputReverseCommuterFile";
	private static final String POPULATION_TYPE = "populationType";
	private static final String USE_HOUSEHOLDS = "useHouseholds";
	private static final String DATABASE_URL = "databaseUrl";
	private static final String MID_DATABASE = "midDatabase";
	private static final String MID_HH_TABLE = "midHouseholds";
	private static final String MID_PERSONS_TABLE = "midPersons";
	private static final String DATABASE_USER = "databaseUser";
	private static final String DATABASE_PASSWD = "password";
	private static final String PERSONS_SQL_QUERY = "personsQuery";
	private static final String WAYS_SQL_QUERY = "waysQuery";
	
	//MEMBERS
	private String osmFile;
	private String[] surveyAreaIds;
	private String crs;
	private String workingDirectory;
	private String inputCommuterFile;
	private String inputReverseCommuterFile;
	private PopulationType popType;
	private int personsInSurveyArea;
	private boolean useHouseholds = false;
	
	private String databaseUrl;
	private String midDatabase;
	private String midHouseholdsTable;
	private String midPersonsTable;
	private String midWaysTable;
	private String databaseUser;
	private String userPassword;
	
	private String personsSqlQuery;
	private String waysSqlQuery;
	
	public enum PopulationType{dummy,commuter,complete};
	
	public Configuration(String file){
		
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
						
					} else if(SURVEY_AREA_IDS.equals(lineParts[0])){
						
						this.surveyAreaIds = lineParts[1].split(",");
						
					} else if(CRS.equals(lineParts[0])){
						
						this.crs = lineParts[1];
						
					} else if(WORKING_DIR.equals(lineParts[0])){
						
						this.workingDirectory = lineParts[1];
						
					} else if(INPUT_COMMUTER_FILE.equals(lineParts[0])){
						
						this.inputCommuterFile = lineParts[1];
						
					} else if(INPUT_REVERSE_COMMUTER_FILE.equals(lineParts[0])){
						
						this.inputReverseCommuterFile = lineParts[1];
						
					} else if(POPULATION_TYPE.equals(lineParts[0])){
						
						this.popType = PopulationType.valueOf(lineParts[1]);
						
					} else if(USE_HOUSEHOLDS.equals(lineParts[0])){
						
						this.useHouseholds = Boolean.parseBoolean(lineParts[1]);
						
					} else if(DATABASE_URL.equals(lineParts[0])){
						
						this.databaseUrl = lineParts[1];
						
					} else if(MID_HH_TABLE.equals(lineParts[0])){
						
						this.midHouseholdsTable = lineParts[1];
						
					} else if(MID_PERSONS_TABLE.equals(lineParts[0])){
						
						this.midPersonsTable = lineParts[1];
						
					} else if(DATABASE_USER.equals(lineParts[0])){
						
						this.databaseUser = lineParts[1];
						
					} else if(DATABASE_PASSWD.equals(lineParts[0])){
						
						this.userPassword = lineParts[1];
						
					} else if(MID_DATABASE.equals(lineParts[0])){
						
						this.midDatabase = lineParts[1];
						
					} else if(PERSONS_SQL_QUERY.equals(lineParts[0])){
						
						this.personsSqlQuery = lineParts[1];
						
					} else if(WAYS_SQL_QUERY.equals(lineParts[0])){
						
						this.waysSqlQuery = lineParts[1];
						
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

	public String[] getSurveyAreaIds() {
		return this.surveyAreaIds;
	}

	public String getCrs() {
		return crs;
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}
	
	public String getCommuterFile() {
		return this.inputCommuterFile;
	}
	
	public String getReverseCommuterFile() {
		return this.inputReverseCommuterFile;
	}
	
	public PopulationType getPopulationType() {
		return this.popType;
	}
	
	public int getNumberOfPersons(){
		return this.personsInSurveyArea;
	}
	
	public boolean isUsingHouseholds(){
		return this.useHouseholds;
	}
	
	public String getHouseholdDatabaseUrl(){
		return (this.databaseUrl + this.midHouseholdsTable);
	}
	
	public String getPersonDatabaseUrl(){
		return (this.databaseUrl + this.midPersonsTable);
	}
	
	public String getWayDatabaseUrl(){
		return (this.databaseUrl + this.midWaysTable);
	}
	
	public String getDatabaseUsername(){
		return this.databaseUser;
	}
	
	public String getPassword(){
		return this.userPassword;
	}

	public String getMidDatabase() {
		return (this.databaseUrl + this.midDatabase);
	}

	public String getPersonsSqlQuery() {
		return personsSqlQuery;
	}
	
	public String getWaysSqlQuery() {
		return waysSqlQuery;
	}
	
}