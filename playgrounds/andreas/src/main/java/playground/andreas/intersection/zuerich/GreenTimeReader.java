package playground.andreas.intersection.zuerich;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;


public class GreenTimeReader implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(GreenTimeReader.class);
	
//	private static final String[] HEADER = {"PersonId", "HomeLocation", "Age", "IsFemale", "Income", "PrimaryActivityType", "PrimaryActivityLocation"};
	
	private TabularFileParserConfig tabFileParserConfig;
		
//	private boolean isFirstLine = true;
	
	private HashMap<Integer, SignalSystemControllerData> lsaConfigMap = new HashMap<Integer, SignalSystemControllerData>();

	private SignalControlData lsaConfigs = new SignalControlDataImpl();
	
	public void startRow(String[] row) throws IllegalArgumentException {
				
		if(row[0].contains("LSAID")){
			log.info("Found header: " + row.toString());
		} else {
//			log.info("Added: " + row.toString());
			
			SignalSystemControllerData lsa = this.lsaConfigMap.get(Integer.valueOf(row[0]));
			SignalPlanData plan = lsaConfigs.getFactory().createSignalPlanData(new IdImpl("1"));
			
			if(lsa == null){
				lsa = lsaConfigs.getFactory().createSignalSystemControllerData(new IdImpl(Integer.parseInt(row[0])));
				plan.setCycleTime(new Integer(99));
				plan.setStartTime(0.0);
				plan.setEndTime(86399.);
				plan.setOffset(new Integer(0));
				lsa.addSignalPlanData(plan);
				
				this.lsaConfigMap.put(Integer.valueOf(row[0]), lsa);
			}
			
			SignalGroupSettingsData sgConfig = this.lsaConfigs.getFactory().createSignalGroupSettingsData(new IdImpl(row[1]));
			sgConfig.setOnset(0);
			sgConfig.setDropping(Integer.parseInt(row[2]));
			plan.addSignalGroupSettings(sgConfig);

		} 
		
	}
	
	
	public HashMap<Integer, SignalSystemControllerData> readFile(String filename) throws IOException {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setFileName(filename);
		this.tabFileParserConfig.setDelimiterTags(new String[] {" ", "\t"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(this.tabFileParserConfig, this);
		return this.lsaConfigMap;
	}
	
	public static HashMap<Integer, SignalSystemControllerData> readBasicLightSignalSystemDefinition(String filename){
		
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
