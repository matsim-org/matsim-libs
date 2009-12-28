package playground.christoph.knowledge.container;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import playground.christoph.knowledge.container.CellKnowledge.CellData;

public class CellKnowledgeCreator {

	private CellNetworkMapping cellNetworkMapping;
		
	public CellKnowledgeCreator(CellNetworkMapping cellNetworkMapping)
	{
		this.cellNetworkMapping = cellNetworkMapping;
	}
	
	public CellKnowledge createCellKnowledge(Map<Id, Node> knownNodes)
	{	
		/*
		 * - find all involved Cells (iterate over all known Nodes)
		 * - Combine Cells if possible (later...)
		 * - Combine Black/White Lists (later...)
		 */	
		CellKnowledge cellKnowledge = new CellKnowledge(cellNetworkMapping);
		Map<Cell, CellData> cellDataMap = cellKnowledge.getCellDataMap();
		
		/*
		 *  Decide, whether a Black or WhiteList should be used.
		 *  Of course, this is just a very simple criteria to decide
		 *  which kind of data structure would be more efficient.
		 */
		if (knownNodes.size() < cellNetworkMapping.getNetwork().getNodes().size() / 2)
		{
			cellKnowledge.setWhiteList(true);
		}
		else
		{
			cellKnowledge.setWhiteList(false);
			Map<Id, Node> unknownNodes = new HashMap<Id, Node>();
			unknownNodes.putAll(cellNetworkMapping.getNetwork().getNodes());
			
			for (Node node : knownNodes.values())
			{
				unknownNodes.remove(node.getId());
			}
			// replace the known Nodes Map
			knownNodes = unknownNodes;
		}
		
		// find all involved Cells, create CellData Object for them and add the known Nodes
		for (Node node : knownNodes.values())
		{
			Cell cell = cellNetworkMapping.getCell(node);
			
			CellData cellData = cellDataMap.get(cell);
			
			if (cellData == null)
			{
				cellData = cellKnowledge.new CellData(cell);
				cellDataMap.put(cell, cellData);
			}
			
			cellData.addNode(node);
		}
		
		/*
		 *  Let the CellData decide, whether using a Black- or WhiteList
		 *  would be better...
		 */
		for (CellData cellData : cellDataMap.values())
		{
			cellData.chooseListType();
		}
			
		return cellKnowledge;
	}
	
}
