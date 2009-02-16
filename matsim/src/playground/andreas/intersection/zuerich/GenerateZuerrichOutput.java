package playground.andreas.intersection.zuerich;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalSystemConfiguration;
import org.matsim.basic.signalsystems.BasicLane;
import org.matsim.basic.signalsystems.BasicLanesToLinkAssignment;
import org.matsim.basic.signalsystems.BasicLightSignalGroupDefinition;
import org.matsim.basic.signalsystems.BasicLightSignalSystemDefinition;
import org.matsim.basic.signalsystems.BasicLightSignalSystems;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.lightsignalsystems.MatsimLightSignalSystemConfigurationWriter;
import org.matsim.lightsignalsystems.MatsimLightSignalSystemsWriter;

public class GenerateZuerrichOutput {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// rausschreiben
		HashMap<Integer, BasicLightSignalSystemDefinition> basicLightSignalSystemDefinition = LSASystemsReader.readBasicLightSignalSystemDefinition("D:/ampel/generateOutputFile/lsa_tu.txt");
		
		HashMap<Integer, BasicLightSignalSystemConfiguration> basicLightSignalSystemConfiguration = GreenTimeReader.readBasicLightSignalSystemDefinition("D:/ampel/generateOutputFile/sg_greentime.txt");
		MatsimLightSignalSystemConfigurationWriter matsimLightSignalSystemConfigurationWriter = new MatsimLightSignalSystemConfigurationWriter(new ArrayList<BasicLightSignalSystemConfiguration>(basicLightSignalSystemConfiguration.values()));
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
					assignment = new BasicLanesToLinkAssignment(new IdImpl(laneLinkMapping.get(nodeId).get(fromLaneId)));
				}
				
//				assignment = basicLaneToLinkAs.get(new IdImpl(laneLinkMapping.get(nodeId).get(fromLaneId)));
				
				List<Integer> toLanes = nodeCombos.get(fromLaneId);
				
				BasicLane lane = new BasicLane(new IdImpl(fromLaneId.intValue()));
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
		
		HashMap<Id, BasicLightSignalGroupDefinition> basicSGs = new HashMap<Id, BasicLightSignalGroupDefinition>();
		
		for (Integer nodeId : lsaSpurMapping.keySet()) {
			
			HashMap<Integer,  List<Integer>> nodeCombos = lsaSpurMapping.get(nodeId);
			
			for (Integer fromSGId : nodeCombos.keySet()) {

				if (laneLinkMapping.get(nodeId).get(fromSGId) != null){

					BasicLightSignalGroupDefinition sg = basicSGs.get(new IdImpl(laneLinkMapping.get(nodeId).get(fromSGId)));

					if (sg == null){
						sg = new BasicLightSignalGroupDefinition(new IdImpl(laneLinkMapping.get(nodeId).get(lsaSpurMapping.get(nodeId).get(fromSGId).get(0))), new IdImpl(fromSGId.intValue()));
						sg.setLightSignalSystemDefinitionId(new IdImpl(fromSGId.intValue()));
					}

//					sg = basicSGs.get(new IdImpl(laneLinkMapping.get(nodeId).get(fromSGId)));

					List<Integer> toLanes = nodeCombos.get(fromSGId);

					for (Integer toLaneId : toLanes) {
						if (!laneLinkMapping.get(nodeId).get(toLaneId).equalsIgnoreCase("-")){
							
							if(sg.getLaneIds() == null || !sg.getLaneIds().contains(new IdImpl(toLaneId.intValue()))){
								sg.addLaneId(new IdImpl(toLaneId.intValue()));
							}
							if(sg.getToLinkIds() == null || !sg.getToLinkIds().contains(new IdImpl(laneLinkMapping.get(nodeId).get(toLaneId)))){
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
		
		BasicLightSignalSystems basicLightSignalSystems = new BasicLightSignalSystems();
		for (BasicLanesToLinkAssignment basicAssignment : basicLaneToLinkAs.values()) {
			if (!basicAssignment.getLinkId().toString().equalsIgnoreCase("-")){
				basicLightSignalSystems.addLanesToLinkAssignment(basicAssignment);
			} else System.err.println("Removed an assignment");			
		}
		for (BasicLightSignalGroupDefinition basicSignalGroup : basicSGs.values()) {
//			if (basicSignalGroup.getLinkRefId() != null){
				basicLightSignalSystems.addLightSignalGroupDefinition(basicSignalGroup);
//			} else System.err.println("Removed a SignalGroup");
		}
		for (BasicLightSignalSystemDefinition basicSignalSystem : basicLightSignalSystemDefinition.values()) {
			basicLightSignalSystems.addLightSignalSystemDefinition(basicSignalSystem);
		}

		
		MatsimLightSignalSystemsWriter signalSytemswriter = new MatsimLightSignalSystemsWriter(basicLightSignalSystems);
		signalSytemswriter.writeFile("lsa.xml");
		
		RemoveDuplicates.readBasicLightSignalSystemDefinition("lsa_config.xml");
		RemoveDuplicates.readBasicLightSignalSystemDefinition("lsa.xml");
		
		System.out.println("Everything finshed");

	}

}
