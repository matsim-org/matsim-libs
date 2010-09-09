/* *********************************************************************** *
 * project: org.matsim.*
 * DgNetworkKmlStyleFactory
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.visualization;

import java.io.IOException;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.LineStyleType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.StyleType;

import org.matsim.core.gbl.MatsimResource;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKmlStyleFactory;
import org.matsim.vis.kml.NetworkKmlStyleFactory;


/**
 * @author dgrether
 *
 */
public class DgNetworkKmlStyleFactory implements NetworkKmlStyleFactory {

	private StyleType defaultnetworknodestyle;

	private StyleType defaultnetworklinkstyle;

	private KMZWriter writer;

	private DocumentType document;
	
	private ObjectFactory kmlObjectFactory = new ObjectFactory();

	private static final byte[] WHITE = new byte[]{(byte) 230, (byte) 230, (byte) 230, (byte) 230};
	private static final byte[] GREY = new byte[]{(byte) 210, (byte) 70, (byte) 50, (byte) 50};

	public DgNetworkKmlStyleFactory(KMZWriter writer, DocumentType document) {
		this.writer = writer;
		this.document = document;
	}

	
	@Override
	public StyleType createDefaultNetworkLinkStyle() throws IOException {
		if (this.defaultnetworklinkstyle == null) {
			this.defaultnetworklinkstyle = kmlObjectFactory.createStyleType();
			this.defaultnetworklinkstyle.setId("defaultnetworklinkstyle");

			LinkType iconLink = kmlObjectFactory.createLinkType();
			iconLink.setHref(MatsimKmlStyleFactory.DEFAULTLINKICON);
			this.writer.addNonKMLFile(MatsimResource.getAsInputStream(MatsimKmlStyleFactory.DEFAULTNODEICONRESOURCE), MatsimKmlStyleFactory.DEFAULTLINKICON);
			IconStyleType iStyle = kmlObjectFactory.createIconStyleType();
			iStyle.setIcon(iconLink);
			iStyle.setColor(WHITE);
			iStyle.setScale(0.2);
			this.defaultnetworklinkstyle.setIconStyle(iStyle);
			LineStyleType lineStyle = kmlObjectFactory.createLineStyleType();
			lineStyle.setColor(GREY);
			lineStyle.setWidth(Double.valueOf(4.0));
			this.defaultnetworklinkstyle.setLineStyle(lineStyle);
			this.document.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(this.defaultnetworklinkstyle));
		}
		return this.defaultnetworklinkstyle;
	}
	
	@Override
	public StyleType createDefaultNetworkNodeStyle() throws IOException {
		if (this.defaultnetworknodestyle == null) {
			this.defaultnetworknodestyle = kmlObjectFactory.createStyleType();
			this.defaultnetworknodestyle.setId("defaultnetworknodestyle");

			LinkType iconLink = kmlObjectFactory.createLinkType();
			iconLink.setHref(MatsimKmlStyleFactory.DEFAULTNODEICON);
			this.writer.addNonKMLFile(MatsimResource.getAsInputStream(MatsimKmlStyleFactory.DEFAULTNODEICONRESOURCE), MatsimKmlStyleFactory.DEFAULTNODEICON);
			IconStyleType iStyle = kmlObjectFactory.createIconStyleType();
			iStyle.setIcon(iconLink);
			iStyle.setColor(MatsimKmlStyleFactory.MATSIMRED);
			iStyle.setScale(0.3);
			this.defaultnetworknodestyle.setIconStyle(iStyle);
			this.document.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(this.defaultnetworknodestyle));
		}
		return this.defaultnetworknodestyle;	
		}
}
