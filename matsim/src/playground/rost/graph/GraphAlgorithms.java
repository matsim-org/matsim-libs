/******************************************************************************
 *project: org.matsim.*
 * GraphAlgorithms.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.graph;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;

public class GraphAlgorithms {
	
	private static final Logger log = Logger.getLogger(GraphAlgorithms.class);
	
	public static double dY = 111.32;
	public static double dX = 66.7;
	
	public static double getDistance(Node n1, Node n2)
	{
		 return Math.sqrt(Math.pow((n2.getCoord().getY() - n1.getCoord().getY()),2) + Math.pow((n2.getCoord().getX() - n1.getCoord().getX()),2));
	}
	
	public static void setDX(double averageLatitude)
	{
		dX = 111.32*Math.cos(averageLatitude);
	}
	
	public static double getDistanceMeter(Node n1, Node n2)
	{
		return 1000*Math.sqrt(Math.pow(dY*(n2.getCoord().getY() - n1.getCoord().getY()),2) + Math.pow(dX*(n2.getCoord().getX() - n1.getCoord().getX()),2));		
	}
	
	public static double getDistanceMeter(double x, double y, double x2, double y2)
	{
		return 1000*Math.sqrt(Math.pow(dY*(y2 - y),2) + Math.pow(dX*(x2 - x),2));		
	}
	
	
	
	public static double getMaxDistance(Collection<Node> collection)
	{
		double dist;
		double maxDist = - Double.MAX_VALUE;
		for(Node node : collection)
		{
			for(Node other : collection)
			{
				if(!node.equals(other))
				{
					dist = getDistance(node, other);
					if(dist > maxDist)
						maxDist = dist;
				}
			}
		}
		return maxDist;
	}
	
	public static boolean pointIsInPolygon(List<Node> polygon, Node point, BoundingBox bBox)
	{
		if(bBox.outOfBox(point))
			return false;
		else
			return pointIsInPolygon(polygon, point);
		
	}
	
	public static boolean pointIsInPolygon(List<Node> polygon, Node point)
	{
		//if the border already contains this point, its included
		if(polygon.contains(point))
			return true;
		//else we have to check whether its "inside"
		int countCut = 0;
		for(int i = 0; i < polygon.size()-1; ++i)
		{
			Node from = polygon.get(i);
			Node to = polygon.get(i+1);
			double xPoint = point.getCoord().getX();
			double yPoint = point.getCoord().getY();
			double xFrom = from.getCoord().getX();
			double yFrom = from.getCoord().getY();
			double xTo = to.getCoord().getX();
			double yTo = to.getCoord().getY();
			double m = (yTo - yFrom) / (xTo - xFrom);
			double n = yFrom;
			double x = (yPoint + m*xFrom - n) / m;
			if(x >= xPoint && ((x > xFrom && x < xTo) || (x < xFrom && x > xTo) || x == xTo) )
				++countCut;
			
			//geradengleichung aufstellen von from nach to:
			//  y = mx + n
			//wir betrachten den Punkt from, an der Stelle xFrom muss
			//der Funktionswert yFrom sein:
			//  y = yFrom = mx + n = m(x-xFrom) + n = n
			// ==> n = yFrom
			//Steigung m = (yTo - yFrom) / (xTo - xFrom)
			//da wir eine Waagerechte nach links zeichnen, setzen wir y = yPoint
			// ==> y = yPoint = m(x-xFrom) + yFrom
			// yPoint-yFrom + mxFrom = mx..
		}
		if(countCut % 2 == 0)
			return false;
		else
			return true;
	}	
	
	public static double calcAngle(Node n1, Node n2, Node n3)
	{
		double dX1 =  dX * (n2.getCoord().getX() - n1.getCoord().getX());
		double dY1 =  dY * (n2.getCoord().getY() - n1.getCoord().getY());
		double dX2 =  dX * (n3.getCoord().getX() - n2.getCoord().getX());
		double dY2 =  dY * (n3.getCoord().getY() - n2.getCoord().getY());
		double quo = Math.sqrt(dX1*dX1 + dY1 * dY1) * Math.sqrt(dX2*dX2 + dY2 * dY2);
		double result;
		if(quo == 0)
			if(dX1 == 0 && dY1 == 0)
				result = Math.atan(dY2 / dX2);
			else
				result = Math.atan(dY1 / dX1);
		else
		{
			
			result = ((dX1*dX2) + (dY1*dY2)) / quo;
			if(Math.abs(result) > 1)
				result = Math.signum(result);
			result = Math.acos(result);
		}
		result *= 180/Math.PI;
		if(((dX1*dY2) - (dX2*dY1)) > 0)
			result *= -1;
		
		log.debug(n1.getId().toString() + ", " + n2.getId().toString() + ", " + n3.getId().toString() + "; " +"("+n1.getCoord().getX() + "," + n1.getCoord().getY() + ")" + "("+n2.getCoord().getX() + "," + n2.getCoord().getY()+ ")" + "("+n3.getCoord().getX() + "," + n3.getCoord().getY()+ ")" + "m1: " + dY1/ dX1 + ", m2: " + dY2 / dX2 + "; w: " + result);
		return result;
	}
	
	public static double getSimplePolygonArea(List<Node> polygon)
	{
		double area = 0;
		double xLength;
		double yLength;
		for(int i = 0; i < polygon.size(); ++i)
		{
			Node current = polygon.get(i);
			Node next = polygon.get((i+1) % polygon.size());
			
			area += (current.getCoord().getX() + next.getCoord().getX())*(next.getCoord().getY() - current.getCoord().getY());
		}
		area *= dX*dY/2.0;
		return Math.abs(area);	
	}
}
