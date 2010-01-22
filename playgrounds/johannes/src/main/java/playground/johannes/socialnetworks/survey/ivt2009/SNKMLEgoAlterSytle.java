/* *********************************************************************** *
 * project: org.matsim.*
 * SNKMLEgoAlterSytle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnetworks.survey.ivt2009;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.StyleType;

import org.matsim.api.core.v01.population.Person;

import playground.johannes.socialnetworks.graph.social.Ego;
import playground.johannes.socialnetworks.graph.social.SocialNetwork;
import playground.johannes.socialnetworks.graph.spatial.io.KMLObjectStyle;

/**
 * @author illenberger
 *
 */
public class SNKMLEgoAlterSytle<P extends Person> implements KMLObjectStyle<SocialNetwork, Ego> {

	private Set<Ego> sampledVertices;
		
	private ObjectFactory objectFactory = new ObjectFactory();
	
	private LinkType vertexIconLink;
	
	public SNKMLEgoAlterSytle(Set<Ego> sampledVertices, LinkType vertexIconLink) {
		this.sampledVertices = sampledVertices;
		this.vertexIconLink = vertexIconLink;
	}
	
	public List<StyleType> getObjectStyle(SocialNetwork socialnet) {
		
		
		List<StyleType> styleTypes = new ArrayList<StyleType>(2);
//		styleIdMappings = new TIntObjectHashMap<String>();
		
		
		
			StyleType styleType = objectFactory.createStyleType();
			styleType.setId("sampled");
			
			IconStyleType iconStyle = objectFactory.createIconStyleType();
			iconStyle.setIcon(vertexIconLink);
			iconStyle.setScale(0.5);
		
			Color c = Color.RED;
			iconStyle.setColor(new byte[]{(byte)c.getAlpha(), (byte)c.getBlue(), (byte)c.getGreen(), (byte)c.getRed()});
			
			styleType.setIconStyle(iconStyle);
			
			styleTypes.add(styleType);
//			styleIdMappings.put("sampled", styleType.getId());
//		}
		
			styleType = objectFactory.createStyleType();
			styleType.setId("unsampled");
			
			iconStyle = objectFactory.createIconStyleType();
			iconStyle.setIcon(vertexIconLink);
			iconStyle.setScale(0.5);
		
			c = Color.WHITE;
			iconStyle.setColor(new byte[]{(byte)c.getAlpha(), (byte)c.getBlue(), (byte)c.getGreen(), (byte)c.getRed()});
			
			styleType.setIconStyle(iconStyle);
			
			styleTypes.add(styleType);
		return styleTypes;
	}


	public String getObjectSytleId(Ego object) {
		if(sampledVertices.contains(object))
			return "sampled";
		else
			return "unsampled";
	}

}
