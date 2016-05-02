package playground.dhosse.scenarios.generic;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class Configuration {

	//TAGS
	private static final String SEP = "\t";
	private static final String COMMENT = "#";
	
	private static final String SURVEY_AREA_IDS = "surveyAreaIds";
	private static final String CRS = "coordinateSystem";
	private static final String WORKING_DIR = "workingDirectory";
	private static final String INPUT_COMMUTER_FILE = "inputCommuterFile";
	private static final String INPUT_REVERSE_COMMUTER_FILE = "inputReverseCommuterFile";
	private static final String POPULATION_TYPE = "populationType";
	private static final String USE_HOUSEHOLDS = "useHouseholds";
	private static final String DATABASE_USER = "databaseUser";
	private static final String DATABASE_PASSWD = "password";
	private static final String SQL_QUERY_MID = "midQuery"; //TODO change this one in configuration files
	private static final String ONLY_WORKING_DAYS = "onlyWorkingDays";
	
	//MEMBERS
	private String[] surveyAreaIds;
	private String crs;
	private String workingDirectory;
	private String inputCommuterFile;
	private String inputReverseCommuterFile;
	private PopulationType popType;
	private int personsInSurveyArea;
	private boolean useHouseholds = false;
	private boolean onlyWorkingDays = false;
	
	private String databaseUrl;
	
	private String midDatabase;
	private String midHouseholdsTable;
	private String midPersonsTable;
	private String midWaysTable;
	private String databaseUser;
	private String userPassword;
	
	private String query;
	
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
					
					if(SURVEY_AREA_IDS.equals(lineParts[0])){
						
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
						
					} else if(DATABASE_USER.equals(lineParts[0])){
						
						this.databaseUser = lineParts[1];
						
					} else if(DATABASE_PASSWD.equals(lineParts[0])){
						
						this.userPassword = lineParts[1];
						
					} else if(SQL_QUERY_MID.equals(lineParts[0])){
						
						this.query = lineParts[1];
						
					} else if(ONLY_WORKING_DAYS.equals(lineParts[0])){
						
						this.onlyWorkingDays = Boolean.parseBoolean(lineParts[1]);
						
					}
					
				}
				
			}
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
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

	public String getSqlQuery() {
		return query;
	}
	
	public boolean isOnlyUsingWorkingDays(){
		return this.onlyWorkingDays;
	}
	
}