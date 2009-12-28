package playground.andreas.intersection.zuerich;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.signalsystems.config.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicSignalGroupSettings;
import org.matsim.signalsystems.config.BasicSignalSystemConfiguration;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurationsImpl;
import org.matsim.signalsystems.config.BasicSignalSystemPlan;


public class GreenTimeReader implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(GreenTimeReader.class);
	
//	private static final String[] HEADER = {"PersonId", "HomeLocation", "Age", "IsFemale", "Income", "PrimaryActivityType", "PrimaryActivityLocation"};
	
	private TabularFileParserConfig tabFileParserConfig;
		
//	private boolean isFirstLine = true;
	
	private HashMap<Integer, BasicSignalSystemConfiguration> lsaConfigMap = new HashMap<Integer, BasicSignalSystemConfiguration>();

	private BasicSignalSystemConfigurations lsaConfigs = new BasicSignalSystemConfigurationsImpl();
	
	public void startRow(String[] row) throws IllegalArgumentException {
				
		if(row[0].contains("LSAID")){
			log.info("Found header: " + row.toString());
		} else {
//			log.info("Added: " + row.toString());
			
			BasicSignalSystemConfiguration lsa = this.lsaConfigMap.get(Integer.valueOf(row[0]));
			
			if(lsa == null){
				lsa = lsaConfigs.getFactory().createSignalSystemConfiguration(new IdImpl(Integer.parseInt(row[0])));
				BasicPlanBasedSignalSystemControlInfo controlInfo = lsaConfigs.getFactory().createPlanBasedSignalSystemControlInfo();
				BasicSignalSystemPlan plan = lsaConfigs.getFactory().createSignalSystemPlan(new IdImpl("1"));
				plan.setCycleTime(new Integer(99));
				plan.setStartTime(0);
				plan.setEndTime(86399);
				plan.setSynchronizationOffset(new Integer(0));
				controlInfo.addPlan(plan);
				lsa.setSignalSystemControlInfo(controlInfo);
				
				this.lsaConfigMap.put(Integer.valueOf(row[0]), lsa);
			}
			
			BasicSignalGroupSettings sgConfig = this.lsaConfigs.getFactory().createSignalGroupSettings(new IdImpl(row[1]));
			sgConfig.setRoughCast(0);
			sgConfig.setDropping(Integer.parseInt(row[2]));
			sgConfig.setInterGreenTimeRoughcast(new Integer(2));
			sgConfig.setInterGreenTimeDropping(new Integer(3));
			((BasicPlanBasedSignalSystemControlInfo) lsa.getControlInfo()).getPlans().get(new IdImpl(1)).addLightSignalGroupConfiguration(sgConfig);

		} 
		
	}
	
	
	public HashMap<Integer, BasicSignalSystemConfiguration> readFile(String filename) throws IOException {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setFileName(filename);
		this.tabFileParserConfig.setDelimiterTags(new String[] {" ", "\t"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(this.tabFileParserConfig, this);
		return this.lsaConfigMap;
	}
	
	public static HashMap<Integer, BasicSignalSystemConfiguration> readBasicLightSignalSystemDefinition(String filename){
		
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
