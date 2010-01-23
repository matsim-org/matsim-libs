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

import org.matsim.core.config.Module;
import org.matsim.vis.otfvis.data.fileio.OTFFileWriter;
import org.matsim.vis.otfvis.opengl.gl.Point3f;


/**
 * A config module holding all preferences for the OTFVis.
 * 
 * @author dstrippgen
 *
 */
public class OTFVisConfig extends Module {
	private static final long serialVersionUID = 2L;

	public static final String GROUP_NAME = "otfvis";

  public static final String AGENT_SIZE = "agentSize";
  public static final String MIDDLE_MOUSE_FUNC = "middleMouseFunc";
  public static final String LEFT_MOUSE_FUNC = "leftMouseFunc";
  public static final String RIGHT_MOUSE_FUNC = "rightMouseFunc";

  public static final String FILE_VERSION = "fileVersion";
  public static final String FILE_MINOR_VERSION = "fileMinorVersion";

  public static final String BIG_TIME_STEP = "bigTimeStep";
  public static final String SHOW_TELEPORTATION = "showTeleportation";
  
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

  private List<ZoomEntry> zooms = new ArrayList<ZoomEntry>();
	
  public OTFVisConfig() {
    super(GROUP_NAME);
  }
	
	public List<ZoomEntry> getZooms() {
		return this.zooms;
	}
	public void addZoom(ZoomEntry entry) {
		setModified();
		this.zooms.add(entry);
	}
	public void deleteZoom(ZoomEntry entry) {
		setModified();
		this.zooms.remove(entry);
	}
	public Point3f getZoomValue(String zoomName) {
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
	public void setDelay_ms(int delay_ms) {
		this.delay_ms = delay_ms;
	}
	public boolean isCachingAllowed() {
		return this.cachingAllowed;
	}
	public void setCachingAllowed(boolean cachingAllowed) {
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
		if (AGENT_SIZE.equals(key)) {
			return Float.toString(getAgentSize());
		} else if (MIDDLE_MOUSE_FUNC.equals(key)) {
			return this.middleMouseFunc;
		} else if (LEFT_MOUSE_FUNC.equals(key)) {
			return this.leftMouseFunc;
		}  else if (RIGHT_MOUSE_FUNC.equals(key)) {
			return this.rightMouseFunc;
		} 
		else if (SHOW_TELEPORTATION.equalsIgnoreCase(key)){
			return Boolean.toString(this.showTeleportedAgents);
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (AGENT_SIZE.equals(key)) {
			this.agentSize = Float.parseFloat(value);
		} else if (MIDDLE_MOUSE_FUNC.equals(key)) {
			this.middleMouseFunc = value;
		} else if (LEFT_MOUSE_FUNC.equals(key)) {
			this.leftMouseFunc = value;
		}  else if (RIGHT_MOUSE_FUNC.equals(key)) {
			this.rightMouseFunc = value;
		} 
		else if (SHOW_TELEPORTATION.equalsIgnoreCase(key)){
			this.showTeleportedAgents = Boolean.parseBoolean(value);
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(AGENT_SIZE, getValue(AGENT_SIZE));
		map.put(LEFT_MOUSE_FUNC, getValue(LEFT_MOUSE_FUNC));
		map.put(MIDDLE_MOUSE_FUNC, getValue(MIDDLE_MOUSE_FUNC));
		map.put(RIGHT_MOUSE_FUNC, getValue(RIGHT_MOUSE_FUNC));
		map.put(SHOW_TELEPORTATION, getValue(SHOW_TELEPORTATION));
		return map;
	}
	
	@Override
	protected final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(AGENT_SIZE, "The (initial) size of the agents.  Only a range of numbers is allowed, otherwise otfvis aborts"
				+ " rather ungracefully, or displays no agents at all." ) ; 
		return map ;
	}

	/* direct access */

	public float getAgentSize() {
		return this.agentSize;
	}

	public void setAgentSize(float agentSize) {
		if(this.agentSize != agentSize) setModified();
		this.agentSize = agentSize;
	}

	public String getMiddleMouseFunc() {
		return this.middleMouseFunc;
	}

	public void setMiddleMouseFunc(String middleMouseFunc) {
		if(this.middleMouseFunc.equals(middleMouseFunc)) setModified();
		this.middleMouseFunc = middleMouseFunc;
	}

	public String getLeftMouseFunc() {
		return this.leftMouseFunc;
	}

	public void setLeftMouseFunc(String leftMouseFunc) {
		if(this.leftMouseFunc.equals(leftMouseFunc)) setModified();
		this.leftMouseFunc = leftMouseFunc;
	}

	public String getRightMouseFunc() {
		return this.rightMouseFunc;
	}

	public void setRightMouseFunc(String rightMouseFunc) {
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
	public void setFileVersion(int fileVersion) {
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
	public void setFileMinorVersion(int fileMinorVersion) {
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
	public void setBigTimeStep(int bigTimeStep) {
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
	public void setQueryType(String queryType) {
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
	public void setMultipleSelect(boolean multipleSelect) {
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
	public void setShowParking(boolean showParking) {
		setModified();
		this.showParking = showParking;
	}

	/**
	 * @return the drawLinkIds
	 */
	public boolean drawLinkIds() {
		return this.drawLinkIds;
	}

	/**
	 * @param drawLinkIds the drawLinkIds to set
	 */
	public void setDrawLinkIds(boolean drawLinkIds) {
		setModified();
		this.drawLinkIds = drawLinkIds;
	}

	public void setDrawOverlays(boolean drawOverlays) {
		setModified();
		this.drawOverlays = drawOverlays;
	}
	
	public boolean drawOverlays() {
		return this.drawOverlays;
	}
	public void setDrawTime(boolean draw) {
		setModified();
		this.drawTime = draw;
	}
	
	public boolean drawTime() {
		return this.drawTime;
	}

	public boolean setRenderImages(boolean render) {
		setModified();
		return this.renderImages = render;
	}
	
	public boolean renderImages() {
		return this.renderImages;
	}
	
	public void setDrawScaleBar(boolean drawScaleBar) {
		setModified();
		this.drawScaleBar = drawScaleBar;
	}
	
	public boolean drawScaleBar() {
		return this.drawScaleBar ;
	}
	public boolean isShowTeleportedAgents() {
		return this.showTeleportedAgents ;
	}
	
	public void setShowTeleportedAgents(boolean showTeleportation){
		this.showTeleportedAgents = showTeleportation;
	}

	public void setDrawTransitFacilities(boolean drawTransitFacilities) {
		this.drawTransitFacilities = drawTransitFacilities;
	}

	public boolean drawTransitFacilities() {
		return drawTransitFacilities;
	}

}
