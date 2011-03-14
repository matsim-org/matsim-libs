package playground.wrashid.lib.tools.network.obj;

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;

public class ComplexRectangularSelectionArea {

	private final LinkedList<RectangularArea> includeInSection;
	private final LinkedList<RectangularArea> excludeFromSelection;

	public ComplexRectangularSelectionArea(LinkedList<RectangularArea> includeInSection, LinkedList<RectangularArea> excludeFromSelection) {
		this.includeInSection = includeInSection;
		this.excludeFromSelection = excludeFromSelection;
	}
	
	public boolean isInArea(Coord coordinateToCheck){
		if (isInInclusionAreas(coordinateToCheck) && !isInExclusionAreas(coordinateToCheck)){
			return true;
		}
		return false;
	}

	private boolean isInExclusionAreas(Coord coordinateToCheck) {
		return isInAreaList(coordinateToCheck, excludeFromSelection);
	}

	private boolean isInInclusionAreas(Coord coordinateToCheck) {
		return isInAreaList(coordinateToCheck, includeInSection);
	}

	private boolean isInAreaList(Coord coordinateToCheck, LinkedList<RectangularArea> list) {
		for (RectangularArea rectangularArea : list){
			if (rectangularArea.isInArea(coordinateToCheck)){
				return true;
			}
		}
		return false;
	}
	
	
}
