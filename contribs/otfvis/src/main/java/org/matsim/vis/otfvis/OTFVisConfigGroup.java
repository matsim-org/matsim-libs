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

package org.matsim.vis.otfvis;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.ZoomEntry;


/**
 * A config module holding all preferences for the OTFVis.
 *
 * @author dstrippgen
 *
 */
public class OTFVisConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "otfvis";

	public static final String AGENT_SIZE = "agentSize";
	public static final String MIDDLE_MOUSE_FUNC = "middleMouseFunc";
	public static final String LEFT_MOUSE_FUNC = "leftMouseFunc";
	public static final String RIGHT_MOUSE_FUNC = "rightMouseFunc";

	public static final String SHOW_TELEPORTATION = "showTeleportation";
	public static final String LINK_WIDTH = "linkWidth";
	public static final String DRAW_TRANSIT_FACILITIES = "drawTransitFacilities";
	public static final String DRAW_TRANSIT_FACILITY_IDS = "drawTransitFacilityIds";
	public static final String DRAW_NON_MOVING_ITEMS = "drawNonMovingItems";


	private  float agentSize = 120.f;
	private  String middleMouseFunc = "Pan";
	private  String leftMouseFunc = "Zoom";
	private  String rightMouseFunc = "Select";

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
	private boolean drawTransitFacilityIds = true;
	private boolean renderImages = false;
	private boolean modified = false;
	private int delay_ms = 30;
	private int maximumZoom = 17;
	private boolean mapOverlayMode = false;

	private boolean drawScaleBar = false;
	private boolean showTeleportedAgents = false;

	/**
	 * this is here so that the entry can be communicated from the network (which seems to be known only by the
	 * otfvis server) to the otfvis client.  I am not sure if this is really a hack; might also just make this
	 * configurable in the sense that it tries to find a useful value either from network or from config or from
	 * the saved config.  this is, however, not implemented.  kai, jan'11
	 */
	private Double effectiveLaneWidth = 3.75 ;

	private String mapBaseURL = "";

	private String mapLayer = "";

	private final List<ZoomEntry> zooms = new ArrayList<>();

	private boolean scaleQuadTreeRect;

	// ---

	private static final String COLORING="coloringScheme" ;

	public static enum ColoringScheme { standard, bvg, bvg2, byId, gtfs, taxicab, infection }

    private ColoringScheme coloring = ColoringScheme.standard ;

	// ---

	private static final String LINK_WIDTH_IS_PROPORTIONAL_TO="linkwidthIsProportionalTo" ;

	public static final String NUMBER_OF_LANES = "numberOfLanes" ;
	public static final String CAPACITY = "capacity" ;

	private static final String MAP_OVERLAY_MODE = "mapOverlayMode";
	private static final String MAP_BASE_URL = "mapBaseURL";
	private static final String MAP_LAYER = "mapLayer";

	private String linkWidthIsProportionalTo = NUMBER_OF_LANES ;

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
	public Rectangle2D getZoomValue(final String zoomName) {
		Rectangle2D result = null;
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
	public OTFVisConfigGroup setDelay_ms(final int delay_ms) {
		this.setModified();
		this.delay_ms = delay_ms;
		return this;
	}

	/**
	 * @return the modified
	 */
	public boolean isModified() {
		// yy called from nowhere (as far as I can tell).  kai, jul'16
		return this.modified;
	}
	private void setModified() {
		this.modified = true;
	}

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
		else {
			throw new IllegalArgumentException(key + ".  There may exist a direct getter.");
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		// this is needed since config file parsing uses it.
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;

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
			this.setColoringScheme( ColoringScheme.valueOf(value) ) ;
		}
		else if ( LINK_WIDTH_IS_PROPORTIONAL_TO.equalsIgnoreCase(key) ) {
			this.setLinkWidthIsProportionalTo( value ) ;
		}
		else if (RIGHT_MOUSE_FUNC.equals(key)) {
			this.rightMouseFunc = value;
		}
		else if (SHOW_TELEPORTATION.equalsIgnoreCase(key)) {
			this.showTeleportedAgents = Boolean.parseBoolean(value);
		}
		else if (LINK_WIDTH.equalsIgnoreCase(key)) {
			this.linkWidth = Float.parseFloat(value);
		}
		else if (MAP_OVERLAY_MODE.equalsIgnoreCase(key)) {
			this.mapOverlayMode = Boolean.parseBoolean(value);
		}
		else if (MAP_BASE_URL.equalsIgnoreCase(key)) {
			this.mapBaseURL = value;
		}
		else if (MAP_LAYER.equalsIgnoreCase(key)) {
			this.mapLayer = value;
		}
		else if (DRAW_TRANSIT_FACILITIES.equalsIgnoreCase(key)) {
			this.drawTransitFacilities = Boolean.parseBoolean(value);
		}
		else if (DRAW_TRANSIT_FACILITY_IDS.equalsIgnoreCase(key)) {
			this.drawTransitFacilityIds = Boolean.parseBoolean(value);
		}
		else if (DRAW_NON_MOVING_ITEMS.equalsIgnoreCase(key)) {
			this.showParking = Boolean.parseBoolean(value);
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		// this is needed for everything since the config dump is based on this.

		TreeMap<String, String> map = new TreeMap<>();
		map.put(AGENT_SIZE, Float.toString(this.getAgentSize()));
		map.put(LEFT_MOUSE_FUNC, this.leftMouseFunc);
		map.put(MIDDLE_MOUSE_FUNC, this.middleMouseFunc);
		map.put(RIGHT_MOUSE_FUNC, this.rightMouseFunc);
		map.put(SHOW_TELEPORTATION, Boolean.toString( this.isShowTeleportedAgents()));
		map.put(LINK_WIDTH_IS_PROPORTIONAL_TO, this.getLinkWidthIsProportionalTo());
		map.put(LINK_WIDTH, Double.toString(this.getLinkWidth()));
		map.put(COLORING, this.getColoringScheme().toString() );
		map.put(MAP_OVERLAY_MODE, Boolean.toString(this.isMapOverlayMode()));
		map.put(MAP_BASE_URL, this.mapBaseURL);
		map.put(MAP_LAYER, this.mapLayer);
		map.put(DRAW_NON_MOVING_ITEMS, Boolean.toString(this.showParking));
		map.put(DRAW_TRANSIT_FACILITIES, Boolean.toString(this.drawTransitFacilities));
		map.put(DRAW_TRANSIT_FACILITY_IDS, Boolean.toString(this.drawTransitFacilityIds));
		return map;
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(AGENT_SIZE, "The (initial) size of the agents.  Only a range of numbers is allowed, otherwise otfvis aborts"
				+ " rather ungracefully, or displays no agents at all.");
		map.put(LINK_WIDTH, "The (initial) width of the links of the network. Use positive floating point values.");
		map.put(LINK_WIDTH_IS_PROPORTIONAL_TO, "Link width is proportional to `"+NUMBER_OF_LANES+"' or to `"+CAPACITY+"'.");

		StringBuilder allowedColorings = new StringBuilder();
		for (ColoringScheme scheme : ColoringScheme.values()) {
			allowedColorings.append(' ');
			allowedColorings.append(scheme.toString());
		}
		map.put(COLORING, "coloring scheme for otfvis.  Currently (2012) allowed values:" + allowedColorings);

		map.put(MAP_OVERLAY_MODE, "Render everything on top of map tiles. Default: From tiles.openstreetmap.org");
		map.put(MAP_BASE_URL, "URL to get WMS tiles from. For a local GeoServer instance, use http://localhost:8080/geoserver/wms?service=WMS&");
		map.put(MAP_LAYER, "The WMS layer to display. For GeoServer and a layer called clipped in workspace mz, use mz:clipped");


		map.put(DRAW_NON_MOVING_ITEMS, "If non-moving items (e.g. agents at activities, at bus stops, etc.) should be showed.  " +
				"May affect all non-moving items.") ;
		return map ;
	}

	/* direct access */

	public float getAgentSize() {
		return this.agentSize;
	}

	public OTFVisConfigGroup setAgentSize(final float agentSize) {
		if(this.agentSize != agentSize) setModified();
		this.agentSize = agentSize;
		return this;
	}

	public String getMiddleMouseFunc() {
		return this.middleMouseFunc;
	}

	public OTFVisConfigGroup setMiddleMouseFunc(final String middleMouseFunc) {
		if(this.middleMouseFunc.equals(middleMouseFunc)) setModified();
		this.middleMouseFunc = middleMouseFunc;
		return this;
	}

	public String getLeftMouseFunc() {
		return this.leftMouseFunc;
	}

	public OTFVisConfigGroup setLeftMouseFunc(final String leftMouseFunc) {
		if(this.leftMouseFunc.equals(leftMouseFunc)) setModified();
		this.leftMouseFunc = leftMouseFunc;
		return this;
	}

	public String getRightMouseFunc() {
		return this.rightMouseFunc;
	}

	public OTFVisConfigGroup setRightMouseFunc(final String rightMouseFunc) {
		if(this.rightMouseFunc.equals(rightMouseFunc)) setModified();
		this.rightMouseFunc = rightMouseFunc;
		return this;
	}

	public int getBigTimeStep() {
		return this.bigTimeStep;
	}

	public OTFVisConfigGroup setBigTimeStep(final int bigTimeStep) {
		if(this.bigTimeStep != bigTimeStep) setModified();
		this.bigTimeStep = bigTimeStep;
		return this;
	}

	public String getQueryType() {
		return this.queryType;
	}

	public OTFVisConfigGroup setQueryType( final String queryType ) {
		setModified() ;
		this.queryType = queryType;
		return this;
	}

	public boolean isMultipleSelect() {
		return this.multipleSelect;
	}


	public OTFVisConfigGroup setMultipleSelect(final boolean multipleSelect) {
		setModified() ;
		this.multipleSelect = multipleSelect;
		return this;
	}

	public Color getNetworkColor() {
		return this.networkColor;
	}

	public OTFVisConfigGroup setNetworkColor(final Color networkColor) {
		setModified();
		this.networkColor = new Color(networkColor.getRed(), networkColor.getGreen(), networkColor.getBlue(), 128);
		return this;
	}

	public Color getBackgroundColor() {
		return this.backgroundColor;
	}

	public OTFVisConfigGroup setBackgroundColor(final Color bgColor) {
		setModified();
		this.backgroundColor = bgColor;
		return this;
	}

	public float getLinkWidth() {
		return this.linkWidth;
	}

	public OTFVisConfigGroup setLinkWidth(final float linkWidth) {
		setModified();
		this.linkWidth = linkWidth;
		return this;
	}

	public boolean isDrawNonMovingItems() {
		return this.showParking;
	}

	public OTFVisConfigGroup setDrawNonMovingItems(final boolean showParking) {
		setModified();
		this.showParking = showParking;
		return this;
	}

	public boolean isDrawingLinkIds() {
		return this.drawLinkIds;
	}

	public OTFVisConfigGroup setDrawLinkIds(final boolean drawLinkIds) {
		setModified();
		this.drawLinkIds = drawLinkIds;
		return this;
	}

	public OTFVisConfigGroup setDrawOverlays(final boolean drawOverlays) {
		setModified();
		this.drawOverlays = drawOverlays;
		return this;
	}

	public boolean drawOverlays() {
		return this.drawOverlays;
	}
	public OTFVisConfigGroup setDrawTime(final boolean draw) {
		setModified();
		this.drawTime = draw;
		return this;
	}

	public boolean drawTime() {
		return this.drawTime;
	}

	public OTFVisConfigGroup setRenderImages( final boolean render ) {
		setModified();
		this.renderImages = render;
		return this;
	}

	public boolean getRenderImages() {
		return this.renderImages;
	}

	public OTFVisConfigGroup setDrawScaleBar(final boolean drawScaleBar) {
		setModified();
		this.drawScaleBar = drawScaleBar;
		return this;
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

	public OTFVisConfigGroup setDrawTransitFacilities(final boolean drawTransitFacilities) {
		setModified() ;
		this.drawTransitFacilities = drawTransitFacilities;
		return this;
	}

	public boolean isDrawTransitFacilities() {
		return this.drawTransitFacilities;
	}

	public OTFVisConfigGroup setDrawTransitFacilityIds(final boolean drawTransitFacilityIds) {
		setModified() ;
		this.drawTransitFacilityIds = drawTransitFacilityIds;
		return this;
	}

	public boolean isDrawTransitFacilityIds() {
		return this.drawTransitFacilityIds;
	}

	public boolean isScaleQuadTreeRect() {
		return this.scaleQuadTreeRect;
	}

	public OTFVisConfigGroup setScaleQuadTreeRect(final boolean doScale){
		setModified() ;
		this.scaleQuadTreeRect = doScale;
		return this;
	}

	public ColoringScheme getColoringScheme() {
		return this.coloring ;
	}
	public OTFVisConfigGroup setColoringScheme( ColoringScheme value ) {
		this.setModified() ;
		this.coloring = value ;
		return this;
	}

	public OTFVisConfigGroup setEffectiveLaneWidth(Double effectiveLaneWidth) {
		this.effectiveLaneWidth = effectiveLaneWidth ;
		return this;
	}
	public Double getEffectiveLaneWidth() {
		return this.effectiveLaneWidth ;
	}

	public String getLinkWidthIsProportionalTo() {
		return linkWidthIsProportionalTo;
	}

	public OTFVisConfigGroup setLinkWidthIsProportionalTo(String linkWidthIsProportionalTo) {
		this.linkWidthIsProportionalTo = linkWidthIsProportionalTo;
		return this;
	}

	public int getMaximumZoom() {
		return maximumZoom;
	}

	public OTFVisConfigGroup setMaximumZoom(int maximumZoom) {
		this.maximumZoom = maximumZoom;
		return this;
	}

	public boolean isMapOverlayMode() {
		return mapOverlayMode;
	}

	public OTFVisConfigGroup setMapOverlayMode(boolean mapOverlayMode) {
		this.mapOverlayMode = mapOverlayMode;
		return this;
	}

	public OTFVisConfigGroup setMapBaseUrl(String mapBaseURL) {
		this.mapBaseURL = mapBaseURL;
		return this;
	}

	public OTFVisConfigGroup setMapLayer(String mapLayer) {
		this.mapLayer = mapLayer;
		return this;
	}

	public String getMapBaseURL() {
		return mapBaseURL;
	}

	public String getMapLayer() {
		return mapLayer;
	}


}
