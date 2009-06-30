package playground.andreas.intersection.zuerich;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.lanes.basic.BasicLaneDefinitions;
import org.matsim.lanes.basic.BasicLaneDefinitionsImpl;
import org.matsim.lanes.basic.BasicLaneImpl;
import org.matsim.lanes.basic.BasicLanesToLinkAssignment;
import org.matsim.lanes.basic.BasicLanesToLinkAssignmentImpl;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsWriter;
import org.matsim.signalsystems.MatsimSignalSystemsWriter;
import org.matsim.signalsystems.basic.BasicSignalGroupDefinition;
import org.matsim.signalsystems.basic.BasicSignalGroupDefinitionImpl;
import org.matsim.signalsystems.basic.BasicSignalSystemDefinition;
import org.matsim.signalsystems.basic.BasicSignalSystems;
import org.matsim.signalsystems.basic.BasicSignalSystemsImpl;
import org.matsim.signalsystems.config.BasicSignalSystemConfiguration;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurationsImpl;

public class GenerateZuerrichOutput {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// rausschreiben
		HashMap<Integer, BasicSignalSystemDefinition> basicLightSignalSystemDefinition = LSASystemsReader.readBasicLightSignalSystemDefinition("D:/ampel/generateOutputFile/lsa_tu.txt");
		
		Map<Integer, BasicSignalSystemConfiguration> basicLightSignalSystemConfiguration = GreenTimeReader.readBasicLightSignalSystemDefinition("D:/ampel/generateOutputFile/sg_greentime.txt");
		BasicSignalSystemConfigurations bssc = new BasicSignalSystemConfigurationsImpl();
		for (BasicSignalSystemConfiguration ssc : basicLightSignalSystemConfiguration.values()){
			bssc.getSignalSystemConfigurations().put(ssc.getSignalSystemId(), ssc);
		}
		MatsimSignalSystemConfigurationsWriter matsimLightSignalSystemConfigurationWriter 
		= new MatsimSignalSystemConfigurationsWriter(bssc);
		matsimLightSignalSystemConfigurationWriter.writeFile("lsa_config.xml");
		
		// sortieren
		HashMap<Integer, HashMap<Integer, String>> laneLinkMapping = LaneLinkMappingReader.readBasicLightSignalSystemDefinition("D:/ampel/generateOutputFile/spur_link_mapping_ivtch.txt");
		
		HashMap<Integer, HashMap<Integer,  List<Integer>>> lsaSpurMapping = LSASpurMappingReader.readBasicLightSignalSystemDefinition("D:/ampel/generateOutputFile/lsa_spur_mapping.txt");
		HashMap<Integer, HashMap<Integer,  List<Integer>>> spurSpurMapping = SpurSpurMappingReader.readBasicLightSignalSystemDefinition("D:/ampel/generateOutputFile/spur_spur_mapping.txt");
	
		
				
		HashMap<Id, BasicLanesToLinkAssignment> basicLaneToLinkAs = new HashMap<Id, BasicLanesToLinkAssignment>();
		
		for (Integer nodeId : spurSpurMapping.keySet()) {
			
			HashMap<Integer,  List<Integer>> nodeCombos = spurSpurMapping.get(nodeId);
			
			for (Integer fromLaneId : nodeCombos.keySet()) {

				BasicLanesToLinkAssignment assignment = basicLaneToLinkAs.get(new IdImpl(laneLinkMapping.get(nodeId).get(fromLaneId)));
				
				if (assignment == null){
					assignment = new BasicLanesToLinkAssignmentImpl(new IdImpl(laneLinkMapping.get(nodeId).get(fromLaneId)));
				}
				
//				assignment = basicLaneToLinkAs.get(new IdImpl(laneLinkMapping.get(nodeId).get(fromLaneId)));
				
				List<Integer> toLanes = nodeCombos.get(fromLaneId);
				
				BasicLaneImpl lane = new BasicLaneImpl(new IdImpl(fromLaneId.intValue()));
				lane.setLength(45.0);
				lane.setNumberOfRepresentedLanes(1);


				for (Integer toLaneId : toLanes) {
					if (!laneLinkMapping.get(nodeId).get(toLaneId).equalsIgnoreCase("-")){
						lane.addToLinkId(new IdImpl(laneLinkMapping.get(nodeId).get(toLaneId)));	
					}								
				}
				
				if(lane.getToLinkIds() != null){
					assignment.addLane(lane);
				}
				
				if (assignment.getLanes() != null){
					basicLaneToLinkAs.put(new IdImpl(laneLinkMapping.get(nodeId).get(fromLaneId)), assignment);
				}

			}			
		}
		
		HashMap<Id, BasicSignalGroupDefinition> basicSGs = new HashMap<Id, BasicSignalGroupDefinition>();
		
		for (Integer nodeId : lsaSpurMapping.keySet()) {
			
			HashMap<Integer,  List<Integer>> nodeCombos = lsaSpurMapping.get(nodeId);
			
			for (Integer fromSGId : nodeCombos.keySet()) {

				if (laneLinkMapping.get(nodeId).get(fromSGId) != null){

					BasicSignalGroupDefinition sg = basicSGs.get(new IdImpl(laneLinkMapping.get(nodeId).get(fromSGId)));

					if (sg == null){
						sg = new BasicSignalGroupDefinitionImpl(new IdImpl(laneLinkMapping.get(nodeId).get(lsaSpurMapping.get(nodeId).get(fromSGId).get(0))), new IdImpl(fromSGId.intValue()));
						sg.setLightSignalSystemDefinitionId(new IdImpl(fromSGId.intValue()));
					}

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
							basicSGs.put(new IdImpl(laneLinkMapping.get(nodeId).get(fromSGId)), sg);
					}
				}
			}			
		}
		BasicLaneDefinitions laneDefs = new BasicLaneDefinitionsImpl();
		BasicSignalSystems basicLightSignalSystems = new BasicSignalSystemsImpl();
		for (BasicLanesToLinkAssignment basicAssignment : basicLaneToLinkAs.values()) {
			if (!basicAssignment.getLinkId().toString().equalsIgnoreCase("-")){
				laneDefs.addLanesToLinkAssignment(basicAssignment);
			} else System.err.println("Removed an assignment");			
		}
		for (BasicSignalGroupDefinition basicSignalGroup : basicSGs.values()) {
//			if (basicSignalGroup.getLinkRefId() != null){
				basicLightSignalSystems.addSignalGroupDefinition(basicSignalGroup);
//			} else System.err.println("Removed a SignalGroup");
		}
		for (BasicSignalSystemDefinition basicSignalSystem : basicLightSignalSystemDefinition.values()) {
			basicLightSignalSystems.addSignalSystemDefinition(basicSignalSystem);
		}

		
		MatsimSignalSystemsWriter signalSytemswriter = new MatsimSignalSystemsWriter(laneDefs, basicLightSignalSystems);
		signalSytemswriter.writeFile("lsa.xml");
		
		RemoveDuplicates.readBasicLightSignalSystemDefinition("lsa_config.xml");
		RemoveDuplicates.readBasicLightSignalSystemDefinition("lsa.xml");
		
		System.out.println("Everything finshed");

	}

}
