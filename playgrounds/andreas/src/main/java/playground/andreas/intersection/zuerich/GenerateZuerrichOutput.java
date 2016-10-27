package playground.andreas.intersection.zuerich;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesWriter;
import org.matsim.lanes.data.v11.LaneDefinitions11;
import org.matsim.lanes.data.v11.LaneDefinitionsV11ToV20Conversion;

import playground.andreas.intersection.zuerich.lanes.LanesConsistencyChecker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenerateZuerrichOutput {

	private static final Logger log = Logger.getLogger(GenerateZuerrichOutput.class);
	
	private String inputDir = "/media/data/work/scmWorkspace/shared-svn/studies/dgrether/lsaZurich/mappingNeu/";
	private 	String outputDir = "/media/data/work/scmWorkspace/shared-svn/studies/dgrether/signalSystemsZh/";
	// lsa id \t cycle time
	private 	String lsaTu = inputDir + "lsa_tu.txt";
//	lsa id (=node id) \t signal group id \t start red 
	private 	String sgGreentime = inputDir  + "sg_greentime.txt";
	// node id \t suprnr \t linkid teleatlas \t linkid navteq \t linkid ivtch
	private 	String spurLinkMappingFile = inputDir + "spur_link_mapping_ivtch.txt";
	// knoten id -> lsa id
//	private String knotenLsaMappingFile = inputDir + "LSAs.txt";
	//node id \t lsanr??? \t spurnr
	private 	String lsaSpurMappingFile = inputDir + "lsa_spur_mapping.txt";
	//node id \t vonspur \t nachspur
	private 	String spurSpurMappingFile = inputDir + "spur_spur_mapping.txt";
	
	private 	String lanesOutputFile = outputDir + "laneDefinitions.xml";
	private 	String signalConfigOutputFile = outputDir +  "signalSystemsConfig.xml";
	private 	String signalSystemsOutputFile = outputDir + "signalSystems.xml";
	
	private 	boolean generateLanes = true;
	private 	boolean generateSignalSystems = true;
	private 	boolean generateSignalSystemsConfig = true;
	private 	boolean removeDuplicates = false;
	
	public GenerateZuerrichOutput() {
		Config config = ConfigUtils.createConfig();
		config.qsim().setUseLanes(generateLanes);
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setUseSignalSystems(generateSignalSystems);
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network net = scenario.getNetwork();
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		netReader.readFile(DgPaths.IVTCHNET);
		
		Map<Integer, Map<Integer,  List<Integer>>> knotenVonSpurNachSpurMap = null;
		Map<Integer, Map<Integer, String>> knotenSpurLinkMap = null;
		LaneDefinitions11 laneDefs = null;
		SignalsScenarioWriter writer = new SignalsScenarioWriter();

		
		Lanes lanes20 = null;
		//lane generation
		if (generateLanes){
			//knotennummer -> (vonspur 1->n nachspur)
			knotenVonSpurNachSpurMap = SpurSpurMappingReader.readBasicLightSignalSystemDefinition(spurSpurMappingFile);
			//knotennummer -> (spurnummer -> linkid)
			knotenSpurLinkMap = new SpurLinkMappingReader().readBasicLightSignalSystemDefinition(spurLinkMappingFile);
			//create the lanes
			LanesGenerator laneGeneratior = new LanesGenerator();
			laneGeneratior.setNetwork(net);
			laneDefs = laneGeneratior.processLaneDefinitions(knotenVonSpurNachSpurMap, knotenSpurLinkMap);
			
      lanes20 = LaneDefinitionsV11ToV20Conversion.convertTo20(
			  laneDefs, scenario.getNetwork());

			
			new LanesConsistencyChecker(net, lanes20).checkConsistency();
			
			//write data
			LanesWriter writerDelegate = new LanesWriter(lanes20);
			writerDelegate.write(lanesOutputFile);
		}

		if (generateSignalSystems){
			SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			//first generate the signal systems itself 
			SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
			//read the mappings
			//read the system id <-> cycle time mapping
			new LSASystemsReader(signalSystems).readBasicLightSignalSystemDefinition(lsaTu);
			
//			Map<Integer, Integer> knotenLsaMap = new KnotenLsaMapReader().readFile(knotenLsaMappingFile);
			
			//next generate the signal group definitions
			//node id \t lsanr??? \t spurnr
			Map<Integer, Map<Integer,  List<Integer>>> knotenLsaSpurMap = LSASpurMappingReader.readBasicLightSignalSystemDefinition(lsaSpurMappingFile);
			
			//create the signals
			SignalSystemsGenerator signalsGenerator = new SignalSystemsGenerator(net, lanes20, signalSystems);
			signalsGenerator.processSignalSystems(knotenLsaSpurMap, knotenSpurLinkMap);
			
			//TODO refactor consistency checker
//			new SignalSystemsConsistencyChecker(net, laneDefs, signalSystems).checkConsistency();
			
			writer.setSignalSystemsOutputFilename(signalSystemsOutputFile);
			writer.writeSignalSystemsData(signalSystems);
		}

		if (generateSignalSystemsConfig){
			// signal system configs
			SignalControlData signalSystemConfig = processSignalSystemConfigurations(sgGreentime);
			writer.setSignalControlOutputFilename(signalConfigOutputFile);
			writer.writeSignalControlData(signalSystemConfig);
		}

		if (removeDuplicates){
			//remove duplicates
			RemoveDuplicates.readBasicLightSignalSystemDefinition(signalConfigOutputFile);
			RemoveDuplicates.readBasicLightSignalSystemDefinition(signalSystemsOutputFile);
		}
		log.info("Everything finished");

	}
	

	
	private SignalControlData processSignalSystemConfigurations(String sgGreentime) {
		HashMap<Integer, SignalSystemControllerData> basicLightSignalSystemConfiguration = GreenTimeReader.readBasicLightSignalSystemDefinition(sgGreentime);
		SignalControlData bssc = new SignalControlDataImpl();
		for (SignalSystemControllerData ssc : basicLightSignalSystemConfiguration.values()){
			bssc.addSignalSystemControllerData(ssc);
		}
		return bssc;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new GenerateZuerrichOutput();
	}
}
