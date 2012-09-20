package playground.wdoering.grips.evacuationanalysis;

import org.matsim.core.utils.collections.QuadTree;

public class EventData {
	
	private double cellSize;
	private double timeSum;
	private int arrivals;
	private QuadTree<Cell> cellTree;
	
	public EventData(QuadTree<Cell> cellTree, double cellSize, double timeSum, int arrivals) {
		this.cellTree = cellTree;
		this.cellSize = cellSize;
		this.timeSum = timeSum;
		this.arrivals = arrivals;
	}
	
	
	public double getCellSize() {
		return cellSize;
	}
	public void setCellSize(double cellSize) {
		this.cellSize = cellSize;
	}
	public double getTimeSum() {
		return timeSum;
	}
	public void setTimeSum(double timeSum) {
		this.timeSum = timeSum;
	}
	public int getArrivals() {
		return arrivals;
	}
	public void setArrivals(int arrivals) {
		this.arrivals = arrivals;
	}
	
	public QuadTree<Cell> getCellTree() {
		return cellTree;
	}
	
	public void setCellTree(QuadTree<Cell> cellTree) {
		this.cellTree = cellTree;
	}
	
	
	

}
