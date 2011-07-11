/* *********************************************************************** *
 * project: org.matsim.*
 * OTFVisConfig.java
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

package org.matsim.vis.otfvis.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.Module;
import org.matsim.vis.otfvis.data.fileio.OTFFileWriter;
import org.matsim.vis.otfvis.opengl.gl.Point3f;


/**
 * A config module holding all preferences for the OTFVis.
 *
 * @author dstrippgen
 *
 */
public class OTFVisConfigGroup extends Module {
	private static final Logger log = Logger.getLogger(OTFVisConfigGroup.class);

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "otfvis";

	public static final String AGENT_SIZE = "agentSize";
	public static final String MIDDLE_MOUSE_FUNC = "middleMouseFunc";
	public static final String LEFT_MOUSE_FUNC = "leftMouseFunc";
	public static final String RIGHT_MOUSE_FUNC = "rightMouseFunc";

	public static final String SHOW_TELEPORTATION = "showTeleportation";
	public static final String LINK_WIDTH = "linkWidth";
	
//	public static final String SHOW_PARKING = "showNonMovingItems" ;
	// can't set this outside the true preferences dialogue since there is additional mechanics involved.  kai, jan'11

	private  float agentSize = 120.f;
	private  String middleMouseFunc = "Pan";
	private  String leftMouseFunc = "Zoom";
	private  String rightMouseFunc = "Select";
	private int fileVersion = OTFFileWriter.VERSION;
	private int fileMinorVersion = OTFFileWriter.MINORVERSION;

	private int bigTimeStep = 600;
	private String queryType = "agentPlan";
	private boolean multipleSelect = false;
	private boolean showParking = false;
	private Color backgroundColor = new Color(255, 255, 255, 0);
	private Color networkColor = new Color(128, 128, 128, 200);
	private float linkWidth = 30;
	private boolean drawLinkIds = false;
	private boolean drawTime = false;
	private boolean drawOverlays = true;
	private boolean drawTransitFacilities = true;
	private boolean renderImages = false;
	private boolean modified = false;
	private boolean cachingAllowed = true;
	private int delay_ms = 30;

	private boolean drawScaleBar = false;
	private boolean showTeleportedAgents = false;

	private final List<ZoomEntry> zooms = new ArrayList<ZoomEntry>();

	private boolean scaleQuadTreeRect;

	// ---

	private static final String COLORING="coloringScheme" ;

	public static final String COLORING_STANDARD = "standard" ;
	public static final String COLORING_BVG = "bvg" ;

	private String coloring = COLORING_STANDARD ;

	// ---
	
	private static final String LINK_WIDTH_IS_PROPORTIONAL_TO="linkwidthIsProportionalTo" ;
	
	public static final String NUMBER_OF_LANES = "numberOfLanes" ;
	public static final String CAPACITY = "capacity" ;
	
	private String linkWidthIsProportionalTo = NUMBER_OF_LANES ;
	
	// ---

	public OTFVisConfigGroup() {
		super(GROUP_NAME);
	}

	public List<ZoomEntry> getZooms() {
		return this.zooms;
	}
	public void addZoom(final ZoomEntry entry) {
		setModified();
		this.zooms.add(entry);
	}
	public void deleteZoom(final ZoomEntry entry) {
		setModified();
		this.zooms.remove(entry);
	}
	public Point3f getZoomValue(final String zoomName) {
		Point3f result = null;
		for(ZoomEntry entry : this.zooms) {
			if(entry.getName().equals(zoomName)) {
				result = entry.getZoomstart();
			}
		}
		return result;
	}

	/**
	 * @return the delay_ms
	 */
	public int getDelay_ms() {
		return this.delay_ms;
	}
	/**
	 * @param delay_ms the delay_ms to set
	 */
	public void setDelay_ms(final int delay_ms) {
		this.setModified();
		this.delay_ms = delay_ms;
	}
	public boolean isCachingAllowed() {
		return this.cachingAllowed;
	}
	public void setCachingAllowed(final boolean cachingAllowed) {
		setModified();
		this.cachingAllowed = cachingAllowed;
	}
	/**
	 * @return the modified
	 */
	public boolean isModified() {
		return this.modified;
	}
	/**
	 * @param modified the modified to set
	 */
	private void setModified() {
		this.modified = true;
	}

	/**
	 * @param modified the modified to unset
	 */
	public void clearModified() {
		this.modified = false;
	}

	@Override
	public String getValue(final String key) {
		// for variables that have getters, this is not needed (and should probably be avoided).  kai, jan'11
		if (MIDDLE_MOUSE_FUNC.equals(key)) {
			return this.middleMouseFunc;
		} else if (LEFT_MOUSE_FUNC.equals(key)) {
			return this.leftMouseFunc;
		}  else if (RIGHT_MOUSE_FUNC.equals(key)) {
			return this.rightMouseFunc;
		}
//		else if (AGENT_SIZE.equals(key)) {
//			return Float.toString(getAgentSize());
//		} else if (SHOW_TELEPORTATION.equalsIgnoreCase(key)){
//			return Boolean.toString(this.showTeleportedAgents);
//		}
//		else if (LINK_WIDTH.equalsIgnoreCase(key)){
//			return Float.toString(this.getLinkWidth());
//		}
//		else if ( COLORING.equalsIgnoreCase(key) )
//			return this.coloring ;
		else {
			throw new IllegalArgumentException(key + ".  There may exist a direct getter.");
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		// this is needed since config file parsing uses it.
		
		if (AGENT_SIZE.equals(key)) {
			this.agentSize = Float.parseFloat(value);
		} 
		else if (MIDDLE_MOUSE_FUNC.equals(key)) {
			this.middleMouseFunc = value;
		} 
		else if (LEFT_MOUSE_FUNC.equals(key)) {
			this.leftMouseFunc = value;
		} 
		else if ( COLORING.equalsIgnoreCase(key) ) {
			this.setColoringScheme( value ) ;
		}  
		else if ( LINK_WIDTH_IS_PROPORTIONAL_TO.equalsIgnoreCase(key) ) {
			this.setLinkWidthIsProportionalTo( value ) ;
		}  
		else if (RIGHT_MOUSE_FUNC.equals(key)) {
			this.rightMouseFunc = value;
		}
		else if (SHOW_TELEPORTATION.equalsIgnoreCase(key)){
			this.showTeleportedAgents = Boolean.parseBoolean(value);
		}
		else if (LINK_WIDTH.equalsIgnoreCase(key)){
			this.linkWidth = Float.parseFloat(value);
		}
//		else if ( SHOW_PARKING.equalsIgnoreCase(key) ) {
//			this.setShowParking( Boolean.parseBoolean(value) ) ;
//			// can't set this outside the true preferences dialogue since there is additional mechanics involved.  kai, jan'11
//		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		// this is needed for everything since the config dump is based on this.
		
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(AGENT_SIZE, Float.toString(this.getAgentSize()) );
		map.put(LEFT_MOUSE_FUNC, getValue(LEFT_MOUSE_FUNC));
		map.put(MIDDLE_MOUSE_FUNC, getValue(MIDDLE_MOUSE_FUNC));
		map.put(RIGHT_MOUSE_FUNC, getValue(RIGHT_MOUSE_FUNC));
		map.put(SHOW_TELEPORTATION, Boolean.toString( this.isShowTeleportedAgents() ) ) ;
		map.put(LINK_WIDTH_IS_PROPORTIONAL_TO, this.getLinkWidthIsProportionalTo() );
		map.put(LINK_WIDTH, Double.toString(this.getLinkWidth()) );
		map.put(COLORING, this.getColoringScheme() ) ;
//		map.put(SHOW_PARKING, Boolean.toString( this.isShowParking() ) ) ;
		// can't set this outside the true preferences dialogue since there is additional mechanics involved.  kai, jan'11

		return map;
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(AGENT_SIZE, "The (initial) size of the agents.  Only a range of numbers is allowed, otherwise otfvis aborts"
				+ " rather ungracefully, or displays no agents at all." ) ;
		map.put(LINK_WIDTH, "The (initial) width of the links of the network. Use positive floating point values.");
		map.put(LINK_WIDTH_IS_PROPORTIONAL_TO, "Link width is proportional to `"+NUMBER_OF_LANES+"' or to `"+CAPACITY+"'." ) ;
		map.put(COLORING, "coloring scheme for otfvis.  Currently (2010) allowed values: ``standard'', ``bvg''") ;
//		map.put(SHOW_PARKING, "If non-moving items (e.g. agents at activities, at bus stops, etc.) should be showed.  " +
//				"May affect all non-moving items.") ;
		// can't set this outside the true preferences dialogue since there is additional mechanics involved.  kai, jan'11
		return map ;
	}

	/* direct access */

	public float getAgentSize() {
		return this.agentSize;
	}

	public void setAgentSize(final float agentSize) {
		if(this.agentSize != agentSize) setModified();
		this.agentSize = agentSize;
	}

	public String getMiddleMouseFunc() {
		return this.middleMouseFunc;
	}

	public void setMiddleMouseFunc(final String middleMouseFunc) {
		if(this.middleMouseFunc.equals(middleMouseFunc)) setModified();
		this.middleMouseFunc = middleMouseFunc;
	}

	public String getLeftMouseFunc() {
		return this.leftMouseFunc;
	}

	public void setLeftMouseFunc(final String leftMouseFunc) {
		if(this.leftMouseFunc.equals(leftMouseFunc)) setModified();
		this.leftMouseFunc = leftMouseFunc;
	}

	public String getRightMouseFunc() {
		return this.rightMouseFunc;
	}

	public void setRightMouseFunc(final String rightMouseFunc) {
		if(this.rightMouseFunc.equals(rightMouseFunc)) setModified();
		this.rightMouseFunc = rightMouseFunc;
	}

	/**
	 * @return the fileVersion
	 */
	public int getFileVersion() {
		return this.fileVersion;
	}

	/**
	 * @param fileVersion the fileVersion to set
	 */
	public void setFileVersion(final int fileVersion) {
		this.setModified();
		log.info("File (major) version setting to: " + fileVersion ) ;
		this.fileVersion = fileVersion;
	}

	/**
	 * @return the fileMinorVersion
	 */
	public int getFileMinorVersion() {
		return this.fileMinorVersion;
	}

	/**
	 * @param fileMinorVersion the fileMinorVersion to set
	 */
	public void setFileMinorVersion(final int fileMinorVersion) {
		this.setModified() ;
		log.info("File minor version set to: " + fileMinorVersion ) ;
		this.fileMinorVersion = fileMinorVersion;
	}

	/**
	 * @return the bigTimeStep
	 */
	public int getBigTimeStep() {
		return this.bigTimeStep;
	}

	/**
	 * @param bigTimeStep the bigTimeStep to set
	 */
	public void setBigTimeStep(final int bigTimeStep) {
		if(this.bigTimeStep != bigTimeStep) setModified();
		this.bigTimeStep = bigTimeStep;
	}

	/**
	 * @return the queryType
	 */
	public String getQueryType() {
		return this.queryType;
	}

	/**
	 * @param queryType the queryType to set
	 */
	public void setQueryType(final String queryType) {
		setModified() ;
		this.queryType = queryType;
	}

	/**
	 * @return the multipleSelect
	 */
	public boolean isMultipleSelect() {
		return this.multipleSelect;
	}

	/**
	 * @param multipleSelect the multipleSelect to set
	 */
	public void setMultipleSelect(final boolean multipleSelect) {
		setModified() ;
		this.multipleSelect = multipleSelect;
	}

	public Color getNetworkColor() {
		return this.networkColor;
	}

	public void setNetworkColor(final Color networkColor) {
		setModified();
		this.networkColor = new Color(networkColor.getRed(), networkColor.getGreen(), networkColor.getBlue(), 128);
	}

	public Color getBackgroundColor() {
		return this.backgroundColor;
	}

	public void setBackgroundColor(final Color bgColor) {
		setModified();
		this.backgroundColor = bgColor;
	}

	public float getLinkWidth() {
		return this.linkWidth;
	}

	public void setLinkWidth(final float linkWidth) {
		setModified();
		this.linkWidth = linkWidth;
	}

	/**
	 * @return the showParking
	 */
	public boolean isShowParking() {
		return this.showParking;
	}

	/**
	 * @param showParking the showParking to set
	 */
	public void setShowParking(final boolean showParking) {
		setModified();
		this.showParking = showParking;
	}

	/**
	 * @return the drawLinkIds
	 */
	public boolean isDrawingLinkIds() {
		return this.drawLinkIds;
	}

	/**
	 * @param drawLinkIds the drawLinkIds to set
	 */
	public void setDrawLinkIds(final boolean drawLinkIds) {
		setModified();
		this.drawLinkIds = drawLinkIds;
	}

	public void setDrawOverlays(final boolean drawOverlays) {
		setModified();
		this.drawOverlays = drawOverlays;
	}

	public boolean drawOverlays() {
		return this.drawOverlays;
	}
	public void setDrawTime(final boolean draw) {
		setModified();
		this.drawTime = draw;
	}

	public boolean drawTime() {
		return this.drawTime;
	}

	public boolean setRenderImages(final boolean render) {
		setModified();
		return this.renderImages = render;
	}

	public boolean renderImages() {
		return this.renderImages;
	}

	public void setDrawScaleBar(final boolean drawScaleBar) {
		setModified();
		this.drawScaleBar = drawScaleBar;
	}

	public boolean drawScaleBar() {
		return this.drawScaleBar ;
	}
	public boolean isShowTeleportedAgents() {
		return this.showTeleportedAgents ;
	}

	public void setShowTeleportedAgents(final boolean showTeleportation){
		setModified() ;
		this.showTeleportedAgents = showTeleportation;
	}

	public void setDrawTransitFacilities(final boolean drawTransitFacilities) {
		setModified() ;
		this.drawTransitFacilities = drawTransitFacilities;
	}

	public boolean isDrawTransitFacilities() {
		return this.drawTransitFacilities;
	}

	public boolean isScaleQuadTreeRect() {
		return this.scaleQuadTreeRect;
	}

	public void setScaleQuadTreeRect(final boolean doScale){
		setModified() ;
		this.scaleQuadTreeRect = doScale;
	}

	public String getColoringScheme() {
		return this.coloring ;
	}
	public void setColoringScheme( String value ) {
		this.setModified() ;
		this.coloring = value ;
	}

	/**
	 * this is here so that the entry can be communicated from the network (which seems to be known only by the
	 * otfvis server) to the otfvis client.  I am not sure if this is really a hack; might also just make this 
	 * configurable in the sense that it tries to find a useful value either from network or from config or from
	 * the saved config.  this is, however, not implemented.  kai, jan'11
	 */
	private Double effectiveLaneWidth = null ;
	public void setEffectiveLaneWidth(Double effectiveLaneWidth) {
		this.effectiveLaneWidth = effectiveLaneWidth ;
	}
	public Double getEffectiveLaneWidth() {
		return this.effectiveLaneWidth ;
	}

	public String getLinkWidthIsProportionalTo() {
		return linkWidthIsProportionalTo;
	}

	public void setLinkWidthIsProportionalTo(String linkWidthIsProportionalTo) {
		this.linkWidthIsProportionalTo = linkWidthIsProportionalTo;
	}




}
