package playground.christoph.knowledge.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class CellKnowledge extends BasicNodeKnowledge {

	private CellNetworkMapping cellNetworkMapping;
	private Map<Cell, CellData> cellDataMap;		// Cell, CellData	
	
	private boolean whiteList;
	
	public CellKnowledge(CellNetworkMapping cellNetworkMapping)
	{
		this.cellNetworkMapping = cellNetworkMapping;
		cellDataMap = new HashMap<Cell, CellData>();
	}
	
	protected Map<Cell, CellData> getCellDataMap()
	{
		return cellDataMap;
	}
	
	public void findFullCells()
	{	
		int listCount = 0;
		
		for (CellData cellData : cellDataMap.values())
		{
			//System.out.println("Cellsize: " + cellData.getList().size());
			//if (cellData.getList().size() == 0) System.out.println("full cell");
			listCount = listCount + cellData.getList().size();
		}
		System.out.println("Listsize: " + listCount);
	}
	
	
	@Override
	public Map<Id, Node> getKnownNodes()
	{
		Map<Id, Node> nodes = new HashMap<Id, Node>();
		
		for (CellData cellData : cellDataMap.values())
		{
			for (Node node : cellData.getWhiteList())
			{
				nodes.put(node.getId(), node);
			}
		}
		
		if (whiteList) return nodes;
		else
		{
			Map<Id, Node> blackNodes = new HashMap<Id, Node>();
			blackNodes.putAll(cellNetworkMapping.getNetwork().getNodes());

			for (Node node : nodes.values())
			{
				blackNodes.remove(node.getId());
			}
			return blackNodes;
		}
	}
	
	@Override
	public void setKnownNodes(Map<Id, Node> nodes) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean isWhiteList()
	{
		return whiteList;
	}
	
	public void setWhiteList(boolean value)
	{
		whiteList = value;
	}
	
	
	public boolean knowsNode(Node node)
	{
		Cell cell = getKnowledgeCell(node);
		
		// cell is not part of the knowledge
		if(cell == null)
		{
			if (whiteList) return false;
			else return true;
		}
		
		CellData cellData = cellDataMap.get(cell.getCellId());
	
		// Node is on the Black or WhiteList of the Cell
		if(cellData.getList().contains(node.getId()))
		{
			if (cellData.isWhiteList())
			{
				if (whiteList) return true;
				else return false;
			}
			else
			{
				if (whiteList) return false;
				else return true;
			}
		}
		
		// Cell is part of the Knowledge and not on the Black or WhiteList
		if (whiteList) return true;
		else return false;
	}
	
	
	public boolean knowsLink(Link link)
	{
		// if no Map found or the Map is empty -> Person knows the entire network, return true
		if ( cellDataMap == null ) return true;
		if ( cellDataMap.size() == 0) return true;
				
		if ( this.knowsNode(link.getFromNode()) && this.knowsNode(link.getToNode()) ) return true;
		else return false;
	}
	
	/*
	 * If the Cell or one of its Parents is part of the Knowledge
	 * return the known Cell. 
	 */
	private Cell getKnowledgeCell(Node node)
	{
		Cell cell = cellNetworkMapping.getCell(node);
		
		if (cellDataMap.containsKey(cell)) return cell;
		
		while (cell.getParentCell() != null)
		{			
			cell = cell.getParentCell();
			if (cellDataMap.containsKey(cell)) return cell;
		}
				
		return null;
	}

	public class CellData
	{
		Cell cell;					// Cell...
		List<Node> nodesList; 		// List of Nodes on the Black or WhiteList
		boolean whiteList;			// true if WhiteList, false if BlackList
		
		public CellData(Cell cell)
		{
			this.cell = cell;
			nodesList = new ArrayList<Node>();
			whiteList = true;	// by default
		}
		
		public void addNode(Node node)
		{
			nodesList.add(node);
		}

		/*
		 * Decides if a Black- or WhiteList is the smaller Storage Type
		 * and do the changes if needed. 
		 */
		public void chooseListType()
		{
			int cellNodeCount = cell.getNodes().size();
			int thisNodeCount = nodesList.size();
			
			// if more than half of all CellNodes are known -> create BlackList
			if (2 * thisNodeCount > cellNodeCount)
			{
				whiteList = false;
				List<Node> blackList = new ArrayList<Node>();
				for (Node node : cell.getNodes())
				{
					if (!nodesList.contains(node)) blackList.add(node);
				}
			
				nodesList = null;
				nodesList = blackList;
			}
		}
		
/*
		public CellData(List<Id> blackWhiteList, boolean whiteList)
		{
			this.blackWhiteList = blackWhiteList;
			this.whiteList = whiteList;
		}
*/		
		public Cell getCell()
		{
			return cell;
		}
		
		public List<Node> getWhiteList()
		{
			if (whiteList) return nodesList;
			else
			{
				List<Node> revertNodes = new ArrayList<Node>(); 
				revertNodes.addAll(cell.getNodes());
				
				for(Node node : nodesList)
				{
					revertNodes.remove(node);
				}
				return revertNodes;	
			}
		}
	
		/*
		 * List maybe a Black- or a WhiteList - check via isWhiteList()!
		 */
		public List<Node> getList()
		{
			return nodesList;
		}
		
		public boolean isWhiteList()
		{
			return whiteList;
		}
	}

	public void clearKnowledge()
	{
		this.whiteList = true;
		this.cellDataMap.clear();
	}
}
