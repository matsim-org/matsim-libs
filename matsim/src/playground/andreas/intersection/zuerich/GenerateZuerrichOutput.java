package playground.andreas.intersection.zuerich;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.lanes.MatsimLaneDefinitionsWriter;
import org.matsim.lanes.basic.BasicLaneDefinitions;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsWriter;
import org.matsim.signalsystems.MatsimSignalSystemsWriter;
import org.matsim.signalsystems.basic.BasicSignalGroupDefinition;
import org.matsim.signalsystems.basic.BasicSignalSystems;
import org.matsim.signalsystems.basic.BasicSignalSystemsImpl;
import org.matsim.signalsystems.config.BasicSignalSystemConfiguration;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurationsImpl;

import playground.dgrether.DgPaths;
import playground.dgrether.lanes.LanesConsistencyChecker;

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
	//node id \t lsanr??? \t spurnr
	private 	String lsaSpurMappingFile = inputDir + "lsa_spur_mapping.txt";
	//node id \t vonspur \t nachspur
	private 	String spurSpurMappingFile = inputDir + "spur_spur_mapping.txt";
	
	private 	String lanesOutputFile = outputDir + "laneDefinitions.xml";
	private 	String signalConfigOutputFile = outputDir +  "signalSystemsConfig.xml";
	private 	String signalSystemsOutputFile = outputDir + "signalSystems.xml";
	
	private 	boolean generateLanes = true;
	private 	boolean generateSignalSystems = false;
	private 	boolean generateSignalSystemsConfig = false;
	private 	boolean removeDuplicates = false;
	
	public GenerateZuerrichOutput() {
		Network net = new NetworkLayer();
		MatsimNetworkReader netReader = new MatsimNetworkReader((NetworkLayer) net);
		netReader.readFile(DgPaths.IVTCHNET);
		
		Map<Integer, Map<Integer,  List<Integer>>> spurSpurMapping = null;
		Map<Integer, Map<Integer, String>> spurLinkMapping = null;
		BasicLaneDefinitions laneDefs = null;
		if (generateLanes){
			//knotennummer -> (vonspur 1->n nachspur)
			spurSpurMapping = SpurSpurMappingReader.readBasicLightSignalSystemDefinition(spurSpurMappingFile);
			//knotennummer -> (spurnummer -> linkid)
			spurLinkMapping = new SpurLinkMappingReader().readBasicLightSignalSystemDefinition(spurLinkMappingFile);
			
			//create the lanes
			LanesGenerator laneGeneratior = new LanesGenerator();
			laneGeneratior.setNetwork(net);
			laneDefs = laneGeneratior.processLaneDefinitions(spurSpurMapping, spurLinkMapping);
			
			new LanesConsistencyChecker(net, laneDefs).checkConsistency();
			
			//write data
			MatsimLaneDefinitionsWriter laneWriter = new MatsimLaneDefinitionsWriter(laneDefs);
			laneWriter.writeFile(lanesOutputFile);
		}

		if (generateSignalSystems){
			BasicSignalSystems signalSystems = new BasicSignalSystemsImpl();
			//read the mappings
			//read the system id <-> cycle time mapping
			new LSASystemsReader(signalSystems).readBasicLightSignalSystemDefinition(lsaTu);			
			//node id \t lsanr??? \t spurnr
			Map<Integer, Map<Integer,  List<Integer>>> lsaSpurMapping = LSASpurMappingReader.readBasicLightSignalSystemDefinition(lsaSpurMappingFile);
			//create the signals
			processSignalSystems(lsaSpurMapping, spurLinkMapping, signalSystems);
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
		log.info("Everything finshed");

	}
	
	private void processSignalSystems(Map<Integer, Map<Integer, List<Integer>>> lsaSpurMapping, Map<Integer, Map<Integer, String>> laneLinkMapping, BasicSignalSystems signalSystems){
		//create the signal groups
		for (Integer nodeId : lsaSpurMapping.keySet()) {
			Map<Integer,  List<Integer>> nodeCombos = lsaSpurMapping.get(nodeId);
			
			for (Integer fromSGId : nodeCombos.keySet()) {
				String signalGroupDefIdString = laneLinkMapping.get(nodeId).get(fromSGId);
				if ((signalGroupDefIdString != null) && signalGroupDefIdString.matches("[\\d]+")){
					Id signalGroupDefId = new IdImpl(signalGroupDefIdString);
					BasicSignalGroupDefinition sg = signalSystems.getSignalGroupDefinitions().get(signalGroupDefId);
					if (sg == null){
						Id linkRefId = new IdImpl(laneLinkMapping.get(nodeId).get(lsaSpurMapping.get(nodeId).get(fromSGId).get(0)));
						//old signalGroupDefId was new IdImpl(fromSGId.intValue())
						sg = signalSystems.getBuilder().createSignalGroupDefinition(linkRefId, signalGroupDefId);
						//TODO check if exists
						sg.setSignalSystemDefinitionId(new IdImpl(fromSGId.intValue()));
					}
					
					//add lanes and toLinks
//					sg = basicSGs.get(new IdImpl(laneLinkMapping.get(nodeId).get(fromSGId)));
					List<Integer> toLanes = nodeCombos.get(fromSGId);
					for (Integer toLaneId : toLanes) {
						if (!laneLinkMapping.get(nodeId).get(toLaneId).equalsIgnoreCase("-")){
							if((sg.getLaneIds() == null) || !sg.getLaneIds().contains(new IdImpl(toLaneId.intValue()))){
								sg.addLaneId(new IdImpl(toLaneId.intValue()));
							}
							if((sg.getToLinkIds() == null) || !sg.getToLinkIds().contains(new IdImpl(laneLinkMapping.get(nodeId).get(toLaneId)))){
								sg.addToLinkId(new IdImpl(laneLinkMapping.get(nodeId).get(toLaneId)));	
							}
						}
					}
					if(sg.getLaneIds() != null){
							signalSystems.addSignalGroupDefinition(sg);
					}
				} //end if
				else {
					log.error("Cannot create signalGroupDefinition for id string " + signalGroupDefIdString);
				}
			} //end for			
		}
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
