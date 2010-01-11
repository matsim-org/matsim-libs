/* *********************************************************************** *
 * project: org.matsim.*
 * QueryLinkId.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.opengl.drawer;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Executor;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFDataQuadReceiver;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer.SimpleQuadDrawer;

/**
 * CollectDrawLinkId is a specialized class for collecting and drawing the linkIds, if that
 * option is choosen in Preferences.
 * 
 * @author dstrippgen
 *
 */
public class CollectDrawLinkId {

  private static final Logger log = Logger.getLogger(CollectDrawLinkId.class);
  
	private static final long serialVersionUID = -1389950511283282110L;
	private final double sx;
	private final double sy;
	private double width = 0;
	private double height = 0;
	
	public Map<Coord, String> linkIds = new HashMap<Coord, String>();

	public CollectDrawLinkId(Rectangle2D.Double rect) {
		this.sx = rect.x;
		this.sy = rect.y;
		this.width = rect.width;
		this.height = rect.height;
	}

	public void prepare(OTFClientQuad quad) {
		// just look in a certain region around the actual point, 
		double regionWidth = (quad.getMaxEasting()-quad.getMinEasting())*0.1;
		double regionHeight = (quad.getMaxNorthing()-quad.getMinNorthing())*0.1;
		
		// The quadtree has its own coord system from (0,0) (max-minXY)
		double qsx = sx - quad.getMinEasting();
		double qsy = sy - quad.getMinNorthing();

		QuadTree.Rect rect;
		if (width == 0) {
			rect = new QuadTree.Rect(qsx-regionWidth, qsy-regionHeight, qsx+regionWidth, qsy+regionHeight);
		} else {
			rect = new QuadTree.Rect(qsx,qsy,qsx+width, qsy+height);
		}
		quad.execute(rect, new AddIdStringExecutor(this.getLinkIds()));
	}

  public Map<Coord, String> getLinkIds() {
    return this.linkIds;
  }

  private static final class AddIdStringExecutor implements Executor<OTFDataReader> {
    
    private static int warnCount = 0;
    
    private Map<Coord, String> linkIdMap;

    public AddIdStringExecutor(Map<Coord, String> map){
      this.linkIdMap = map;
    }
    
    public void execute(double x, double y, OTFDataReader reader)  {
      if(reader instanceof OTFDefaultLinkHandler) {
        OTFDataQuadReceiver quadReceiver = ((OTFDefaultLinkHandler) reader).getQuadReceiver();
        if (quadReceiver != null && quadReceiver instanceof SimpleQuadDrawer){
          SimpleQuadDrawer drawer = (SimpleQuadDrawer) quadReceiver;
            drawer.prepareLinkId(linkIdMap);
        }
        else {
          if (warnCount < 3){
            log.warn("Not able to draw link ids. Check if SimpleQuadDrawer is used to draw the network. This message is displayed 3 times only.");
            warnCount++;
          }
        }
      }
    }
  }

  
}
