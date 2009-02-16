package playground.andreas.intersection.zuerich;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.basic.signalsystemsconfig.BasicLightSignalGroupConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicLightSignalSystemConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicLightSignalSystemPlan;
import org.matsim.basic.signalsystemsconfig.BasicPlanBasedLightSignalSystemControlInfo;
import org.matsim.basic.v01.IdImpl;
import org.matsim.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.utils.io.tabularFileParser.TabularFileParserConfig;


public class GreenTimeReader implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(GreenTimeReader.class);
	
//	private static final String[] HEADER = {"PersonId", "HomeLocation", "Age", "IsFemale", "Income", "PrimaryActivityType", "PrimaryActivityLocation"};
	
	private TabularFileParserConfig tabFileParserConfig;
		
//	private boolean isFirstLine = true;
	
	private HashMap<Integer, BasicLightSignalSystemConfiguration> lsaConfigMap = new HashMap<Integer, BasicLightSignalSystemConfiguration>();

	public void startRow(String[] row) throws IllegalArgumentException {
				
		if(row[0].contains("LSAID")){
			log.info("Found header: " + row.toString());
		} else {
//			log.info("Added: " + row.toString());
			
			BasicLightSignalSystemConfiguration lsa = this.lsaConfigMap.get(Integer.valueOf(row[0]));
			
			if(lsa == null){
				lsa = new BasicLightSignalSystemConfiguration(new IdImpl(Integer.parseInt(row[0])));
				BasicPlanBasedLightSignalSystemControlInfo controlInfo = new BasicPlanBasedLightSignalSystemControlInfo();
				BasicLightSignalSystemPlan plan = new BasicLightSignalSystemPlan(new IdImpl("1"));
				plan.setCirculationTime(new Double(99.0));
				plan.setStartTime(0);
				plan.setEndTime(86399);
				plan.setSyncronizationOffset(new Double(0.0));
				controlInfo.addPlan(plan);
				lsa.setLightSignalSystemControlInfo(controlInfo);
				
				this.lsaConfigMap.put(Integer.valueOf(row[0]), lsa);
			}
			
			BasicLightSignalGroupConfiguration sgConfig = new BasicLightSignalGroupConfiguration(new IdImpl(row[1]));
			sgConfig.setRoughCast(0.0);
			sgConfig.setDropping(Double.parseDouble(row[2]));
			sgConfig.setInterimTimeRoughcast(new Double(2.0));
			sgConfig.setInterimTimeDropping(new Double(3.0));
			((BasicPlanBasedLightSignalSystemControlInfo) lsa.getControlInfo()).getPlans().get(new IdImpl(1)).addLightSignalGroupConfiguration(sgConfig);

		}
		
	}
	
	
	public HashMap<Integer, BasicLightSignalSystemConfiguration> readFile(String filename) throws IOException {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setFileName(filename);
		this.tabFileParserConfig.setDelimiterTags(new String[] {" ", "\t"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(this.tabFileParserConfig, this);
		return this.lsaConfigMap;
	}
	
	public static HashMap<Integer, BasicLightSignalSystemConfiguration> readBasicLightSignalSystemDefinition(String filename){
		
		GreenTimeReader myLSAFileParser = new GreenTimeReader();
		try {			
			log.info("Start reading file...");
			myLSAFileParser.readFile(filename);
			log.info("...finished.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return myLSAFileParser.lsaConfigMap;
	}
	
}
