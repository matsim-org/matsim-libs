package playground.andreas.intersection.zuerich;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsImpl;
import org.matsim.lanes.MatsimLaneDefinitionsReader;
import org.matsim.lanes.MatsimLaneDefinitionsWriter;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsWriter;
import org.matsim.signalsystems.MatsimSignalSystemsWriter;
import org.matsim.signalsystems.config.BasicSignalSystemConfiguration;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurationsImpl;
import org.matsim.signalsystems.systems.SignalSystems;
import org.matsim.signalsystems.systems.SignalSystemsImpl;

import playground.dgrether.DgPaths;
import playground.dgrether.lanes.LanesConsistencyChecker;
import playground.dgrether.signalsystems.SignalSystemsConsistencyChecker;

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
		Network net = new NetworkLayer();
		MatsimNetworkReader netReader = new MatsimNetworkReader((NetworkLayer) net);
		netReader.readFile(DgPaths.IVTCHNET);
		
		Map<Integer, Map<Integer,  List<Integer>>> knotenVonSpurNachSpurMap = null;
		Map<Integer, Map<Integer, String>> knotenSpurLinkMap = null;
		LaneDefinitions laneDefs = null;
		
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
			
			new LanesConsistencyChecker(net, laneDefs).checkConsistency();
			
			//write data
			MatsimLaneDefinitionsWriter laneWriter = new MatsimLaneDefinitionsWriter(laneDefs);
			laneWriter.writeFile(lanesOutputFile);
		}
		else {
			laneDefs = new LaneDefinitionsImpl();
			MatsimLaneDefinitionsReader laneReader = new MatsimLaneDefinitionsReader(laneDefs);
			laneReader.readFile(lanesOutputFile);
		}

		if (generateSignalSystems){
			//first generate the signal systems itself 
			SignalSystems signalSystems = new SignalSystemsImpl();
			//read the mappings
			//read the system id <-> cycle time mapping
			new LSASystemsReader(signalSystems).readBasicLightSignalSystemDefinition(lsaTu);
			
//			Map<Integer, Integer> knotenLsaMap = new KnotenLsaMapReader().readFile(knotenLsaMappingFile);
			
			//next generate the signal group definitions
			//node id \t lsanr??? \t spurnr
			Map<Integer, Map<Integer,  List<Integer>>> knotenLsaSpurMap = LSASpurMappingReader.readBasicLightSignalSystemDefinition(lsaSpurMappingFile);
			
			//create the signals
			SignalSystemsGenerator signalsGenerator = new SignalSystemsGenerator(net, laneDefs, signalSystems);
			signalsGenerator.processSignalSystems(knotenLsaSpurMap, knotenSpurLinkMap);
			
			new SignalSystemsConsistencyChecker(net, laneDefs, signalSystems).checkConsistency();
			
			MatsimSignalSystemsWriter signalSytemswriter = new MatsimSignalSystemsWriter(signalSystems);
			signalSytemswriter.writeFile(signalSystemsOutputFile );

		}

		if (generateSignalSystemsConfig){
			// signal system configs
			BasicSignalSystemConfigurations signalSystemConfig = processSignalSystemConfigurations(sgGreentime);
			MatsimSignalSystemConfigurationsWriter matsimLightSignalSystemConfigurationWriter 
			= new MatsimSignalSystemConfigurationsWriter(signalSystemConfig);
			matsimLightSignalSystemConfigurationWriter.writeFile(signalConfigOutputFile);
		}

		if (removeDuplicates){
			//remove duplicates
			RemoveDuplicates.readBasicLightSignalSystemDefinition(signalConfigOutputFile);
			RemoveDuplicates.readBasicLightSignalSystemDefinition(signalSystemsOutputFile);
		}
		log.info("Everything finished");

	}
	

	
	private BasicSignalSystemConfigurations processSignalSystemConfigurations(String sgGreentime) {
		Map<Integer, BasicSignalSystemConfiguration> basicLightSignalSystemConfiguration = GreenTimeReader.readBasicLightSignalSystemDefinition(sgGreentime);
		BasicSignalSystemConfigurations bssc = new BasicSignalSystemConfigurationsImpl();
		for (BasicSignalSystemConfiguration ssc : basicLightSignalSystemConfiguration.values()){
			bssc.getSignalSystemConfigurations().put(ssc.getSignalSystemId(), ssc);
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
