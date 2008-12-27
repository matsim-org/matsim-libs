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

package org.matsim.utils.vis.otfvis.gui;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.matsim.config.Module;
import org.matsim.utils.vis.otfvis.opengl.gl.Point3f;
import org.matsim.utils.vis.otfvis.server.OTFQuadFileHandler;


public class OTFVisConfig extends Module {
	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "otfvis";

	public static class ZoomEntry  implements Serializable{
		private static final long serialVersionUID = 1L;
		
		public Point3f getZoomstart() {
			return zoomstart;
		}
		public BufferedImage getSnap() {
			return snap;
		}
		public String getName() {
			return name;
		}
		
		Point3f zoomstart;
		BufferedImage snap;
		String name;
		
		public ZoomEntry() {
			
		}
		
		public ZoomEntry(BufferedImage snap, Point3f zoomstart, String name) {
			super();
			this.snap = snap;
			this.zoomstart = zoomstart;
			this.name = name;
		}

		private void writeObject( java.io.ObjectOutputStream s ) throws IOException {
			s.writeUTF(name);
			s.writeFloat(zoomstart.x);
			s.writeFloat(zoomstart.y);
			s.writeFloat(zoomstart.z);
			ImageIO.write(snap, "jpg", s);
		}


		private void readObject( java.io.ObjectInputStream s ) throws IOException {
			name = s.readUTF();
			zoomstart = new Point3f(s.readFloat(),s.readFloat(),s.readFloat());
			snap = ImageIO.read(s);
		}
	}
	
	public List<ZoomEntry> getZooms() {
		return zooms;
	}
	public void addZoom(ZoomEntry entry) {
		setModified();
		zooms.add(entry);
	}
	public void deleteZoom(ZoomEntry entry) {
		setModified();
		zooms.remove(entry);
	}
	public Point3f getZoomValue(String zoomName) {
		Point3f result = null;
		for(ZoomEntry entry : zooms) {
			if(entry.name.equals(zoomName))result = entry.zoomstart;
		}
		return result;
	}

	List<ZoomEntry> zooms = new ArrayList<ZoomEntry>();

	public OTFVisConfig() {
		super(GROUP_NAME);
	}

	public static final String AGENT_SIZE = "agentSize";
	public static final String MIDDLE_MOUSE_FUNC = "middleMouseFunc";
	public static final String LEFT_MOUSE_FUNC = "leftMouseFunc";
	public static final String RIGHT_MOUSE_FUNC = "rightMouseFunc";

	public static final String FILE_VERSION = "fileVersion";
	public static final String FILE_MINOR_VERSION = "fileMinorVersion";

	public static final String BIG_TIME_STEP = "bigTimeStep";
//	public static final String TIME_STEP = "timeStep";

	private  float agentSize = 120.f;
	private  String middleMouseFunc = "Pan";
	private  String leftMouseFunc = "Zoom";
	private  String rightMouseFunc = "Select";
	private int fileVersion = OTFQuadFileHandler.VERSION;
	private int fileMinorVersion = OTFQuadFileHandler.MINORVERSION;

	private int bigTimeStep = 600;
//	private final int timeStep = 1;
	private String queryType = "agentPlan";
	private boolean multipleSelect = true;
	private boolean showParking = false;
	private Color backgroundColor = new Color(255, 255, 255, 0);
	private Color networkColor = new Color(128, 128, 128, 200);
	private float linkWidth = 30;
	private boolean drawLinkIds = false;
	private boolean drawTime = false;
	private boolean drawOverlays = true;
	private boolean renderImages = false;
	private boolean modified = false;

	/**
	 * @return the modified
	 */
	public boolean isModified() {
		return modified;
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
			return middleMouseFunc;
		} else if (LEFT_MOUSE_FUNC.equals(key)) {
			return leftMouseFunc;
		}  else if (RIGHT_MOUSE_FUNC.equals(key)) {
			return rightMouseFunc;
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (AGENT_SIZE.equals(key)) {
			agentSize = Float.parseFloat(value);
		} else if (MIDDLE_MOUSE_FUNC.equals(key)) {
			middleMouseFunc = value;
		} else if (LEFT_MOUSE_FUNC.equals(key)) {
			leftMouseFunc = value;
		}  else if (RIGHT_MOUSE_FUNC.equals(key)) {
			rightMouseFunc = value;
		} else {
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
		return map;
	}

	/* direct access */

	public float getAgentSize() {
		return agentSize;
	}

	public void setAgentSize(float agentSize) {
		if(this.agentSize != agentSize) setModified();
		this.agentSize = agentSize;
	}

	public String getMiddleMouseFunc() {
		return middleMouseFunc;
	}

	public void setMiddleMouseFunc(String middleMouseFunc) {
		if(this.middleMouseFunc.equals(middleMouseFunc)) setModified();
		this.middleMouseFunc = middleMouseFunc;
	}

	public String getLeftMouseFunc() {
		return leftMouseFunc;
	}

	public void setLeftMouseFunc(String leftMouseFunc) {
		if(this.leftMouseFunc.equals(leftMouseFunc)) setModified();
		this.leftMouseFunc = leftMouseFunc;
	}

	public String getRightMouseFunc() {
		return rightMouseFunc;
	}

	public void setRightMouseFunc(String rightMouseFunc) {
		if(this.rightMouseFunc.equals(rightMouseFunc)) setModified();
		this.rightMouseFunc = rightMouseFunc;
	}

	/**
	 * @return the fileVersion
	 */
	public int getFileVersion() {
		return fileVersion;
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
		return fileMinorVersion;
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
		return bigTimeStep;
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
		return queryType;
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
		return multipleSelect;
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
		return showParking;
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
		return drawLinkIds;
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
		return drawOverlays;
	}
	public void setDrawTime(boolean draw) {
		setModified();
		this.drawTime = draw;
	}
	
	public boolean drawTime() {
		return drawTime;
	}

	public boolean setRenderImages(boolean render) {
		setModified();
		return renderImages = render;
	}
	
	public boolean renderImages() {
		return renderImages;
	}
	

}
