package org.matsim.contrib.accessibility;

public final class Labels {
	private Labels(){} // do not instantiate
	
	public static final String _ACCESSIBILITY = "_accessibility" ;
	
	public static final String ZONE_ID = "zone_id";
	public static final String X_COORDINATE = "xcoord";
	public static final String Y_COORDINATE = "ycoord";
	public static final String TIME = "time";
//	public static final String NEARESTNODE_ID = "nearest_node_id";
//	public static final String NEARESTNODE_X_COORD = "x_coord_nn";
//	public static final String NEARESTNODE_Y_COORD = "y_coord_nn";

	static final String ACCESSIBILITY_CELLSIZE = "Accessibility_cellsize_";
	public static final String FREESPEED_FILENAME = "freeSpeed" + ACCESSIBILITY_CELLSIZE;
	public static final String CAR_FILENAME = "car" + ACCESSIBILITY_CELLSIZE;
	public static final String BIKE_FILENAME = "bike" + ACCESSIBILITY_CELLSIZE;
	public static final String WALK_FILENAME = "walk" + ACCESSIBILITY_CELLSIZE;
	public static final String PT_FILENAME = "pt" + ACCESSIBILITY_CELLSIZE;
	
	public static final String POPULATION_DENSITIY = "population_density";
}
