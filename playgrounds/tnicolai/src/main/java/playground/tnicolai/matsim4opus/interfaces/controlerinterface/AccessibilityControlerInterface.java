package playground.tnicolai.matsim4opus.interfaces.controlerinterface;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneAccessibilityObject;

public interface AccessibilityControlerInterface {
	
	public SpatialGrid<Double> getCongestedTravelTimeAccessibilityGrid();
	
	public SpatialGrid<Double> getFreespeedTravelTimeAccessibilityGrid();
	
	public SpatialGrid<Double> getWalkTravelTimeAccessibilityGrid();
	
	public ZoneLayer<ZoneAccessibilityObject> getStartZones();

}
