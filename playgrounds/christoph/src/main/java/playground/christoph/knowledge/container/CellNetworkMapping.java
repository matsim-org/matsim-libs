package playground.christoph.knowledge.container;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;

public class CellNetworkMapping {

	private final static Logger log = Logger.getLogger(TestCellKnowledge.class);

	private NetworkLayer network;
	private Map <Node, Cell> nodesMapping;
	private Map <Integer, Cell> cells;

	private double xMin;
	private double xMax;
	private double yMin;
	private double yMax;
	private int xZones = 2;
	private int yZones = 2;
	private int maxNodesPerZone = 25;

	public CellNetworkMapping(NetworkLayer network)
	{
		this.network = network;
	}

	public void createMapping()
	{
		createCoreMapping();
	}

	public Cell getCell(Node node)
	{
		return nodesMapping.get(node);
	}

	public NetworkLayer getNetwork()
	{
		return network;
	}

	private void createCoreMapping()
	{
		nodesMapping = new HashMap<Node, Cell>();

		getNetworkDimensions();

		double xLength = (xMax - xMin) / xZones;
		double yLength = (yMax - yMin) / yZones;

		// creating Core Cells
		cells = new HashMap<Integer, Cell>();

		for (int i = 0; i < xZones; i++)
		{
			for (int j = 0; j < yZones; j++)
			{
				Cell cell = new Cell();

				cell.setXMin(xMin + xLength * i);
				cell.setXMax(xMin + xLength * (i + 1));
				cell.setYMin(yMin + yLength * j);
				cell.setYMax(yMin + yLength * (j + 1));

				cell.setMaxNodesPerZone(this.maxNodesPerZone);

				cells.put(cell.getCellId(), cell);
			}
		}

		// assign Nodes to their Core Cells
		for (Node node : network.getNodes().values())
		{
			Cell coreCell = getCoreCell(node);
			if (coreCell != null) coreCell.addNode(node);
		}

		log.info("Created " + cells.size() + " Core Cells");

		// split up Core Cells, if they contain to many Children
		Map<Integer, Cell> childrenCells = new HashMap<Integer,Cell>();
		for (Cell cell : cells.values())
		{
			childrenCells.putAll(cell.createChildrenCells());
		}
		cells.putAll(childrenCells);

		log.info("Found " + cells.size() + " Cells after splitting up them.");

		// create Nodes Mapping
		for (Cell cell : cells.values())
		{
			List<Node> nodes = cell.getNodes();

			// otherwise cell.getNodes() would return null
			if (!cell.hasChildren())
			{
				for (Node node : nodes)
				{
					nodesMapping.put(node, cell);
				}
			}
		}

		log.info("Mapped " + nodesMapping.size() + " Nodes to Cells");
		log.info("Network contains " + network.getNodes().size() + " Nodes");

/*
		for (Cell cell : cells.values())
		{
			if (cell.hasChildren()) log.info("CellId: " + cell.getCellId() + ", Size: null (has Children)");
			else log.info("CellId: " + cell.getCellId() + ", Size: " + cell.getNodes().size());
		}
*/
	}

	private Cell getCoreCell(Node node)
	{
		//return nodesMapping.get(node);
		for (Cell cell : cells.values())
		{
			Coord coord = node.getCoord();
			double xCoord = coord.getX();
			double yCoord = coord.getY();

			if (xCoord >= cell.getXMin() && xCoord <= cell.getXMax())
			{
				if (yCoord >= cell.getYMin() && yCoord <= cell.getYMax())
				{
					return cell;
				}
			}
		}
		log.error("Core Cell was not found!");

		return null;
	}

	private void getNetworkDimensions()
	{
		Collection<Node> nodes = network.getNodes().values();

		Node firstNode = nodes.iterator().next();
		xMin = firstNode.getCoord().getX();
		xMax = firstNode.getCoord().getX();
		yMin = firstNode.getCoord().getY();
		yMax = firstNode.getCoord().getY();

		for (Node node : nodes)
		{
			Coord coord = node.getCoord();
			double xCoord = coord.getX();
			double yCoord = coord.getY();

			if (xCoord < xMin) xMin = xCoord;
			if (xCoord > xMax) xMax = xCoord;
			if (yCoord < yMin) yMin = yCoord;
			if (yCoord > yMax) yMax = yCoord;
		}
	}
}
