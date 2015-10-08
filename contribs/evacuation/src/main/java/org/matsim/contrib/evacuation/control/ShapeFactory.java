/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
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


package org.matsim.contrib.evacuation.control;

import org.matsim.contrib.evacuation.model.Constants;
import org.matsim.contrib.evacuation.model.shape.*;
import org.matsim.contrib.evacuation.model.shape.Shape;
import org.matsim.contrib.evacuation.model.shape.Shape.DrawMode;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * helper class for creating standard shapes
 * 
 * @author wdoering
 *
 */
public class ShapeFactory
{
	

	public static BoxShape getNetBoxShape(int shapeRendererId, Rectangle2D bbRect, boolean light)
	{
		//create box shape, set description and id
		BoxShape boundingBox = new BoxShape(shapeRendererId, bbRect);
		boundingBox.setDescription(Constants.DESC_OSM_BOUNDINGBOX);
		boundingBox.setId(Constants.ID_NETWORK_BOUNDINGBOX);
		
		//set style
		ShapeStyle style;
		if (light)
			style = new ShapeStyle(Constants.COLOR_NET_LIGHT_BOUNDINGBOX_FILL, Constants.COLOR_NET_LIGHT_BOUNDINGBOX_FILL, 1f, DrawMode.FILL);
		else
			style = new ShapeStyle(Constants.COLOR_NET_BOUNDINGBOX_FILL, Constants.COLOR_NET_BOUNDINGBOX_CONTOUR, 4f, DrawMode.FILL_WITH_CONTOUR);
		
		boundingBox.setStyle(style);
		
		return boundingBox;
	}
	
	public static CircleShape getEvacCircle(int shapeRendererId, Point2D c0, Point2D c1)
	{
		//create circle shape, set id
		CircleShape evacCircle = new CircleShape(shapeRendererId, c0, c1);
		evacCircle.setId(Constants.ID_EVACAREAPOLY);
		
		//set style
		ShapeStyle style = Constants.SHAPESTYLE_EVACAREA;
		evacCircle.setStyle(style);
		
		return evacCircle;
	}
	
	public static Shape getEvacPoly(int id, List<Point2D> points) {
		
		if (points.size()==0)
			return null;
		
//		if (points.size()==1)
//		{
//			Point2D p1 = new Point2D.Double(points.get(0).getX()+0.00002,points.get(0).getY()+0.00002);
//			Point2D p2 = new Point2D.Double(points.get(0).getX()+0.00002,points.get(0).getY()-0.00002);
//			points.add(p1);
//			points.add(p2);
//			points.add(p2);
//		}
		
		
		PolygonShape polygonShape = new PolygonShape(points, id);
		polygonShape.setId(Constants.ID_EVACAREAPOLY);
		polygonShape.setStyle(Constants.SHAPESTYLE_EVACAREA);
		
		return polygonShape;
	}
	
	
	
	public static CircleShape getPopShape(String popAreaID, int shapeRendererId, Point2D c0, Point2D c1)
	{
		
		//create circle shape, set id
		CircleShape popCircle = new CircleShape(popAreaID, shapeRendererId, c0, c1);
		
		//set style
		setPopAreaStyle(popCircle);
		return popCircle;
	}

	public static void setPopAreaStyle(Shape shape) {
		ShapeStyle style = Constants.SHAPESTYLE_POPAREA;
		style.setHoverColor(Constants.COLOR_POPAREA_HOVER);
		style.setSelectColor(Constants.COLOR_POPAREA_SELECTED);
		shape.setStyle(style);
	}
	
	public static LineShape getRoadClosureShape(int shapeRendererId, String linkID, Point2D c0, Point2D c1)
	{
		//create circle shape, set id
		LineShape roadClosureLine = new LineShape(shapeRendererId, c0, c1);
		roadClosureLine.setId(Constants.ID_ROADCLOSURE_PREFIX + linkID);
		
		//set style
		ShapeStyle style = Constants.SHAPESTYLE_ROADCLOSURE;
		style.setSelectColor(Constants.COLOR_POPAREA_SELECTED);
		roadClosureLine.setStyle(style);
		
		roadClosureLine.setArrow(false);
		return roadClosureLine;
	}
	
	public static LineShape getHoverLineShape(int shapeRendererId, Point2D c0, Point2D c1)
	{
		//create circle shape, set id
		LineShape hoverLine = new LineShape(shapeRendererId, c0, c1);
		hoverLine.setId(Constants.ID_HOVERELEMENT);
		
		//set style
		ShapeStyle style = Constants.SHAPESTYLE_HOVER_LINE;
		hoverLine.setStyle(style);
		
		
		return hoverLine;
	}
	
	public static LineShape getPrimarySelectedLineShape(int shapeRendererId, Point2D c0, Point2D c1)
	{
		//create circle shape, set id
		LineShape hoverLine = new LineShape(shapeRendererId, c0, c1);
		hoverLine.setId(Constants.ID_LINK_PRIMARY);
		
		//set style
		ShapeStyle style = new ShapeStyle(Color.RED, Color.RED, 4f, DrawMode.FILL);
		hoverLine.setStyle(style);
		
		hoverLine.setOffsetX(10);
		hoverLine.setOffsetY(10);
		
		hoverLine.setArrow(true);
		
		
		return hoverLine;
	}
	
	public static LineShape getSecondarySelectedLineShape(int shapeRendererId, Point2D c0, Point2D c1)
	{
		//create circle shape, set id
		LineShape hoverLine = new LineShape(shapeRendererId, c0, c1);
		hoverLine.setId(Constants.ID_LINK_SECONDARY);
		
		//set style
		ShapeStyle style = new ShapeStyle(Color.GREEN, Color.GREEN, 4f, DrawMode.FILL);
		hoverLine.setStyle(style);
		
		hoverLine.setOffsetX(-10);
		hoverLine.setOffsetY(-10);
		
		hoverLine.setArrow(true);
		
		return hoverLine;
	}

}
