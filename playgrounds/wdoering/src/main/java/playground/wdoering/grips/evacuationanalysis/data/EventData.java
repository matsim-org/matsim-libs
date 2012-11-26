/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


package playground.wdoering.grips.evacuationanalysis.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.collections.Tuple;

import playground.wdoering.grips.evacuationanalysis.EvacuationAnalysis.Mode;

public class EventData {
	
	private String eventName;	
	private double cellSize;
	private double timeSum;
	private double maxCellTimeSum;
	private int arrivals;
	private QuadTree<Cell> cellTree;
	private List<Tuple<Double, Integer>> arrivalTimes;
	private Rect boundingBox;
	private HashMap<Id, List<Tuple<Id,Double>>> linkLeaveTimes;
	private HashMap<Id, List<Tuple<Id,Double>>> linkEnterTimes;
	private int maxUtilization;
	private double maxClearingTime;
	
	private AttributeData<Color> evacuationTimeVisData;
	private AttributeData<Color> clearingTimeVisData;
	private AttributeData<Tuple<Float,Color>> linkUtilizationVisData;
	
//	private HashMap<Mode, List<Tuple<Id,String>>> colorClasses;
	
	//TODO getLinkUtilization(linkid) -> anz agenten die link verlassen haben
	
	public EventData(String eventName, QuadTree<Cell> cellTree, double cellSize, double timeSum, double maxCellTimeSum, int arrivals, List<Tuple<Double,Integer>> arrivalTimes, Rect boundingBox) {
		this.eventName = eventName;
		this.cellTree = cellTree;
		this.cellSize = cellSize;
		this.timeSum = timeSum;
		this.maxCellTimeSum = maxCellTimeSum;
		this.arrivals = arrivals;
		this.arrivalTimes = arrivalTimes;
		this.boundingBox = boundingBox;
	}
	
	public EventData(String eventName)
	{
		this.eventName = eventName;
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
	
	public double getMaxCellTimeSum() {
		return maxCellTimeSum;
	}
	
	public void setMaxCellTimeSum(double maxCellTimeSum) {
		this.maxCellTimeSum = maxCellTimeSum;
	}
	
	public List<Tuple<Double, Integer>> getArrivalTimes() {
		return arrivalTimes;
	}
	
	public void setArrivalTimes(List<Tuple<Double, Integer>> arrivalTimes) {
		this.arrivalTimes = arrivalTimes;
	}
	
	public String getEventName() {
		return eventName;
	}
	
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}
	
	public Rect getBoundingBox() {
		return boundingBox;
	}
	
	public void setBoundingBox(Rect boundingBox) {
		this.boundingBox = boundingBox;
	}


	public void setLinkEnterTimes(HashMap<Id, List<Tuple<Id,Double>>> linkEnterTimes) {
		this.linkEnterTimes = linkEnterTimes;
	}


	public void setLinkLeaveTimes(HashMap<Id, List<Tuple<Id,Double>>> linkLeaveTimes) {
		this.linkLeaveTimes = linkLeaveTimes;
	}
	
	public HashMap<Id, List<Tuple<Id,Double>>> getLinkEnterTimes() {
		return linkEnterTimes;
	}
	
	public HashMap<Id, List<Tuple<Id,Double>>> getLinkLeaveTimes() {
		return linkLeaveTimes;
	}


	public void setMaxUtilization(int maxUtilization)
	{
		this.maxUtilization = maxUtilization;
	}
	
	public int getMaxUtilization() {
		return maxUtilization;
	}
	
	public void setMaxClearingTime(double maxClearingTime) {
		this.maxClearingTime = maxClearingTime;
	}

	public double getMaxClearingTime() {
		
		return this.maxClearingTime;
	}
	
	public AttributeData<Color> getEvacuationTimeVisData() {
		return evacuationTimeVisData;
	}
	
	public AttributeData<Color> getClearingTimeVisData() {
		return clearingTimeVisData;
	}
	
	public AttributeData<Tuple<Float,Color>> getLinkUtilizationVisData() {
		return linkUtilizationVisData;
	}
	
	public void setEvacuationTimeVisData(AttributeData<Color> evacuationTimeVisData) {
		this.evacuationTimeVisData = evacuationTimeVisData;
	}
	
	public void setClearingTimeVisData(AttributeData<Color> clearingTimeVisData) {
		this.clearingTimeVisData = clearingTimeVisData;
	}
	
	public void setLinkUtilizationVisData(AttributeData<Tuple<Float,Color>> linkUtilizationVisData) {
		this.linkUtilizationVisData = linkUtilizationVisData;
	}
	
	public LinkedList<Cell> getCells()
	{
		LinkedList<Cell> cells = new LinkedList<Cell>();
		getCellTree().get(new Rect(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), cells);
		return cells;
	}
	
//	public ArrayList<Tuple<Color,String>> getColorClasses(Mode mode)
//	{
//		ArrayList<Tuple<Color,String>> currentColorClasses = new ArrayList<Tuple<Color,String>>(); 
//		
//		for (Tuple<Id,String> colorClass : colorClasses.get(mode))
//		{
//			Tuple<Color,String> colorStringTuple;
//			
//			if (mode.equals(Mode.EVACUATION))
//				colorStringTuple = new Tuple<Color,String>(evacuationTimeVisData.getAttribute((IdImpl)colorClass.getFirst()), colorClass.getSecond());
//			else if (mode.equals(Mode.CLEARING))
//				colorStringTuple = new Tuple<Color,String>(clearingTimeVisData.getAttribute((IdImpl)colorClass.getFirst()),  colorClass.getSecond());
//			else
//				colorStringTuple = new Tuple<Color,String>(linkUtilizationVisData.getAttribute((IdImpl)colorClass.getFirst()).getSecond(), colorClass.getSecond());
//			
//			currentColorClasses.add(colorStringTuple);
//		}
//		
//		return currentColorClasses;
//	}
//
//	public void setColorClasses(Mode mode, List<Tuple<Id, String>> colorClasses)
//	{
//		if (this.colorClasses==null)
//			this.colorClasses = new HashMap<Mode, List<Tuple<Id,String>>>();
//		
//		this.colorClasses.put(mode, colorClasses);
//	}
	

}
