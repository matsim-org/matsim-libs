package playground.mmoyo.input;

import java.util.ArrayList;
import org.matsim.api.basic.v01.TransportMode;
import java.util.List;
import java.util.Map;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import playground.mmoyo.PTRouter.PTLine;
import playground.mmoyo.PTRouter.PTNode;
import playground.mmoyo.PTRouter.PTTimeTable2;
import java.util.TreeMap;

import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;

/**
 * Adds in memory new PTLines from an independent file to the existing network and PTTimetable
 * @param filePath path of the file containing new PTLines description
 * @param net existing network
 * @param timeTable existing Departure information
 */
public class PTLineAggregator {
	private NetworkLayer net;
	private PTTimeTable2 timeTable;
	private String filePath;

	public PTLineAggregator(String filePath, NetworkLayer net, PTTimeTable2 timeTable) {
		this.net = net;
		this.timeTable = timeTable;
		this.filePath = filePath;
	}

	/**
	 * Add new nodes for every new PTline and assign them new Id with affixes
	 */
	public void addLine() {
		PTNodeReader ptNodeReader = new PTNodeReader();
		ptNodeReader.readFile(filePath);

		// List<List<BasicNode>> nodeListList = ptNodeReader.getNodeLists();
		Map<String, List<BasicNode>> lineMap = ptNodeReader.lineMap;
		PTNodeFactory ptNodeFactory = new PTNodeFactory(this.net, this.timeTable);
		PTLinkFactory ptLinkFactory = new PTLinkFactory(this.net);

		// System.out.println(nodeListList.size());

		int iniNodes = net.getNodes().size();
		int iniLinks = net.getLinks().size();
		System.out.println("creating new nodes and links...");

		BasicNode[] basicNodeArr = new BasicNode[2];
		for (List<BasicNode> basicNodeList : lineMap.values()) {

			List<BasicNode> basicNodeList1 = new ArrayList<BasicNode>();
			List<BasicNode> basicNodeList2 = new ArrayList<BasicNode>();
			for (BasicNode basicNode : basicNodeList) {
				basicNodeArr = ptNodeFactory.createPTNodes(basicNode);
				basicNodeList1.add(basicNodeArr[0]);
				basicNodeList2.add(basicNodeArr[1]);
			}
			ptLinkFactory.addNewLinks(basicNodeList1);
			ptLinkFactory.addNewLinks(basicNodeList2);
		}

		System.out.println("Done.");

		int finNodes = net.getNodes().size() - iniNodes;
		int finLinks = net.getLinks().size() - iniLinks;

		System.out.println("created Nodes:" + finNodes);
		System.out.println("created Links:" + finLinks);

		// -->and after creating the new nodes and links, transfers and detached
		// must be created again
	}

	/**
	 * Add new nodes for every new PTline and creates for them: new id`s,
	 * standard links and ficticious constant travel times.
	 */
	public void addLines() {
		PTNodeReader ptNodeReader = new PTNodeReader();
		ptNodeReader.readFile(filePath);

		List<PTLine> ptLineList = new ArrayList<PTLine>();

		Map<String, List<PTNode>> ptnodeMap = new TreeMap<String, List<PTNode>>();
		Map<String, Character> charMap = new TreeMap<String, Character>();

		int intLinkId = 0;
		Map<Id, Double> linkTravelTimeMap = new TreeMap<Id, Double>();
		for (Map.Entry<String, List<BasicNode>> entry : ptNodeReader.lineMap
				.entrySet()) {
			String strIdPtLine = entry.getKey();
			List<BasicNode> nodeList = entry.getValue();

			List<Id> route1 = new ArrayList<Id>();
			List<Id> route2 = new ArrayList<Id>();
			List<Double> minute = new ArrayList<Double>();
			List<String> departures = new ArrayList<String>();

			int seqIndex1 = 0;
			int seqIndex2 = nodeList.size();
			double min = 0;
			boolean first = true;

			PTNode lastPTNode1 = null;
			PTNode lastPTNode2 = null;

			for (BasicNode bNode : nodeList) {
				String originalId = bNode.getId().toString();

				String strNode1 = "~" + originalId;
				String strNode2 = "_" + originalId;

				if (!ptnodeMap.containsKey(originalId)) {
					List<PTNode> ptnodeList = new ArrayList<PTNode>();
					ptnodeMap.put(originalId, ptnodeList);
					charMap.put(originalId, 'a');
				} else {
					char sufix = charMap.get(originalId).charValue();
					sufix++;
					charMap.put(originalId, sufix);

					strNode1 = strNode1 + sufix;
					strNode2 = strNode2 + sufix;
				}

				Id newId1 = new IdImpl(strNode1);
				Id newId2 = new IdImpl(strNode2);

				PTNode ptNode1 = new PTNode(newId1, bNode.getCoord(),
						new IdImpl(originalId), new IdImpl(strIdPtLine),
						seqIndex1++);
				ptnodeMap.get(originalId).add(ptNode1);
				net.getNodes().put(newId1, ptNode1);

				PTNode ptNode2 = new PTNode(newId2, bNode.getCoord(),
						new IdImpl(originalId), new IdImpl(strIdPtLine),
						seqIndex2--);
				ptnodeMap.get(originalId).add(ptNode2);
				net.getNodes().put(newId2, ptNode2);

				if (!first) {
					LinkImpl link;

					/** create links between nodes */
					double length = CoordUtils.calcDistance(lastPTNode1.getCoord(), ptNode1.getCoord());
					Id idLink1 = new IdImpl(intLinkId++);
					link = net.createLink(idLink1, lastPTNode1, ptNode1, length, 1.0, 1.0, 1.0, "1", "Standard");
					timeTable.putNextDTLink(lastPTNode1.getId(), link);

					Id idLink2 = new IdImpl(intLinkId++);
					link = net.createLink(idLink2, ptNode2, lastPTNode2, length, 1.0, 1.0, 1.0, "1", "Standard");
					timeTable.putNextDTLink(ptNode2.getId(), link);

					double travelTime = length * 0.5;
					linkTravelTimeMap.put(idLink1, travelTime);
					linkTravelTimeMap.put(idLink2, travelTime);
				}

				lastPTNode1 = ptNode1;
				lastPTNode2 = ptNode2;
				route1.add(newId1);
				route2.add(newId2);
				min = (double) seqIndex1++;
				minute.add(min);
				first = false;
			}

			Id id1 = new IdImpl(strIdPtLine + "H");
			Id id2 = new IdImpl(strIdPtLine + "R");
			TransportMode transportMode = TransportMode.bus; // ->this should be read
			// char lineType = 'b';
			String direction1 = "H";
			String direction2 = "R";
			/** fictitious timetable for the time being*/
			for (int time = 18000; time < 82800; time = time + 900) {
				departures.add(String.valueOf(time));
			}
			PTLine ptLine = new PTLine(id1, transportMode, direction1, route1,
					minute, departures);
			PTLine ptLine2 = new PTLine(id2, transportMode, direction2, route2,
					minute, departures);
			ptLineList.add(ptLine);
			ptLineList.add(ptLine2);
		}

		timeTable.setptLineList(ptLineList);
		timeTable.setLinkTravelTimeMap(linkTravelTimeMap);
		timeTable.calculateTravelTimes(net);

		/*
		 * int iniNodes= net.getNodes().size(); int iniLinks=
		 * net.getLinks().size(); System.out.println("creating new nodes and
		 * links...");
		 * 
		 * BasicNode[] basicNodeArr = new BasicNode[2]; for(List<PTNode>
		 * basicNodeList: nodeListList){ List<BasicNode> basicNodeList1 = new
		 * ArrayList<BasicNode>(); List<BasicNode> basicNodeList2 = new
		 * ArrayList<BasicNode>(); for (BasicNode basicNode : basicNodeList){
		 * basicNodeArr = ptNodeFactory.CreatePTNodes(basicNode);
		 * basicNodeList1.add(basicNodeArr[0]);
		 * basicNodeList2.add(basicNodeArr[1]); }
		 * ptLinkFactory.AddNewLinks(basicNodeList1);
		 * ptLinkFactory.AddNewLinks(basicNodeList2); }
		 * 
		 * System.out.println("Done.");
		 * 
		 * int finNodes= net.getNodes().size() - iniNodes; int finLinks=
		 * net.getLinks().size() - iniLinks;
		 * 
		 * System.out.println("created Nodes:" + finNodes);
		 * System.out.println("created Links:" + finLinks); }
		 */
	}

}
