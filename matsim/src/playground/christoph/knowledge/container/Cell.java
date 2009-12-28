package playground.christoph.knowledge.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.network.NodeImpl;

/*
 * Unterste Zellenebene: keine Children mehr und daf�r eine Liste mit Nodes 
 */
public class Cell {

	private final static Logger log = Logger.getLogger(Cell.class);
	
	static int idCount = 0;

	private double xMin;
	private double xMax;
	private double yMin;
	private double yMax;
	private int id;
	private int maxNodesPerZone = 50;	// by default...
	
	private Cell parentCell = null;
	private Cell[] childrenCells = null;
	private boolean hasChildren = false;
	
	private List<NodeImpl> nodes;
	
	public Cell()
	{
		id = getNextId();
		nodes = new ArrayList<NodeImpl>();
	}
	
	private static int getNextId()
	{
		idCount++;
		return idCount - 1;
	}
	
	public int getCellId()
	{
		return id;
	}
	
	public void setParentCell(Cell cell)
	{
		parentCell = cell;
	}
	
	public Cell getParentCell()
	{
		return parentCell;
	}
	
	public void setMaxNodesPerZone(int value)
	{
		maxNodesPerZone = value;
	}
	
	public int getMaxNodesPerZone()
	{
		return maxNodesPerZone;
	}
	
	public boolean hasChildren()
	{
		return hasChildren;
	}
	
	/* 
	 * Anordnung der Zellen:
	 * 2  3
	 * 0  1
	 */
	public Map<Integer, Cell> createChildrenCells()
	{	
		Map<Integer, Cell> newCells = new HashMap<Integer, Cell>();
		if (nodes.size() > maxNodesPerZone)
		{
			this.hasChildren = true;
//			log.info("Cell Id: " + this.id + ", NodesCount: " + nodes.size() + " " + xMin + " " + xMax + " " + yMin + " " + yMax);
			childrenCells = new Cell[4];
			
			childrenCells[0] = new Cell();
			childrenCells[1] = new Cell();
			childrenCells[2] = new Cell();
			childrenCells[3] = new Cell();
					
			childrenCells[0].xMin = xMin;
			childrenCells[0].xMax = xMin + (xMax - xMin) / 2;
			childrenCells[0].yMin = yMin;
			childrenCells[0].yMax = yMin + (yMax - yMin) / 2;
						
			childrenCells[1].xMin = xMin + (xMax - xMin) / 2;
			childrenCells[1].xMax = xMax;
			childrenCells[1].yMin = yMin;
			childrenCells[1].yMax = yMin + (yMax - yMin) / 2;
			
			childrenCells[2].xMin = xMin;
			childrenCells[2].xMax = xMin + (xMax - xMin) / 2;
			childrenCells[2].yMin = yMin + (yMax - yMin) / 2;
			childrenCells[2].yMax = yMax;
			
			childrenCells[3].xMin = xMin + (xMax - xMin) / 2;
			childrenCells[3].xMax = xMax;
			childrenCells[3].yMin = yMin + (yMax - yMin) / 2;
			childrenCells[3].yMax = yMax;
			
			for (Cell cell : childrenCells)
			{
				cell.maxNodesPerZone = maxNodesPerZone;
				cell.setParentCell(this);
			}
			
			for (NodeImpl node : nodes)
			{
				Coord coord = node.getCoord();
				double xCoord = coord.getX();
				double yCoord = coord.getY();
				
				if (xCoord < xMin + (xMax - xMin)/2)
				{
					if (yCoord < yMin + (yMax - yMin)/2)
					{
						childrenCells[0].addNode(node);
					}
					else
					{
						childrenCells[2].addNode(node);
					}
				}
				else
				{
					if (yCoord < yMin + (yMax - yMin)/2)
					{
						childrenCells[1].addNode(node);
					}
					else
					{
						childrenCells[3].addNode(node);
					}
				}
			}
			
			newCells.put(childrenCells[0].getCellId(), childrenCells[0]);
			newCells.put(childrenCells[1].getCellId(), childrenCells[1]);
			newCells.put(childrenCells[2].getCellId(), childrenCells[2]);
			newCells.put(childrenCells[3].getCellId(), childrenCells[3]);
			
			newCells.putAll(childrenCells[0].createChildrenCells());
			newCells.putAll(childrenCells[1].createChildrenCells());
			newCells.putAll(childrenCells[2].createChildrenCells());
			newCells.putAll(childrenCells[3].createChildrenCells());
			
			nodes = null;
		}
		return newCells;
	}
	
	/*
	 * Cell has no more Children
	 */
	public boolean isCoreCell()
	{
		return childrenCells == null;
	}
	
	public void addNode(NodeImpl node)
	{
		nodes.add(node);
	}
	
	public Cell[] getChildrenCells()
	{
		return childrenCells;
	}
	
	/*
	 * Returns the Nodes of this Cell and of its Children
	 */
	public List<NodeImpl> getAllNodes()
	{
		if(nodes != null) return nodes;
		else
		{
			List<NodeImpl> returnNodes = new ArrayList<NodeImpl>();
			for (Cell cell : childrenCells)
			{
				returnNodes.addAll(cell.getNodes());
			}
			return returnNodes;
		}
	}
	
	/*
	 * Returns only the Nodes of this Cell - may return null!
	 */
	public List<NodeImpl> getNodes()
	{
		return nodes;
	}
	
	public Map<NodeImpl, Cell> getNodesMapping()
	{
		Map<NodeImpl, Cell> mapping = new HashMap<NodeImpl, Cell>();
		
		if (nodes != null)
		{
			for (NodeImpl node : nodes)
			{
				mapping.put(node, this);
			}
		}
		else
		{
			for (Cell cell : childrenCells)
			{
				mapping.putAll(cell.getNodesMapping());
			}
		}
		
		return mapping;
	}
	
	
	public double getXMin() {
		return xMin;
	}

	public void setXMin(double min) {
		xMin = min;
	}

	public double getXMax() {
		return xMax;
	}

	public void setXMax(double max) {
		xMax = max;
	}

	public double getYMin() {
		return yMin;
	}

	public void setYMin(double min) {
		yMin = min;
	}

	public double getYMax() {
		return yMax;
	}

	public void setYMax(double max) {
		yMax = max;
	}
}