package playground.wrashid.lib.obj.geoGrid;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class GeoGrid {

	private final int sideLengthInMeters;
	private boolean dataCollectionPhaseOn = true;
	private LinkedList<GridDataItem> inputData = new LinkedList<GridDataItem>();
	HashMap<String, Double> valuesInGrid = new HashMap<String, Double>();

	public GeoGrid(int sideLengthInMeters) {
		this.sideLengthInMeters = sideLengthInMeters;
	}

	public void markDataCollectionPhaseAsFishished() {
		dataCollectionPhaseOn = false;

		LinkedListValueHashMap<String, GridDataItem> itemsInGrid = new LinkedListValueHashMap<String, GridDataItem>();

		for (GridDataItem dataItem : inputData) {
			itemsInGrid.put(getGridKeyFromCoordinate(dataItem.getCoord()), dataItem);
		}

		resetInputData();

		for (String gridKey : itemsInGrid.getKeySet()) {
			double sumOfValuesTimesWeights = 0;
			double sumOfWeights = 0;
			for (GridDataItem dataItem : itemsInGrid.get(gridKey)) {
				sumOfValuesTimesWeights += dataItem.getValue() * dataItem.getWeight();
				sumOfWeights += dataItem.getWeight();
			}
			valuesInGrid.put(gridKey, sumOfValuesTimesWeights / sumOfWeights);
		}
	}

	private void resetInputData() {
		inputData = null;
	}

	private String getGridKeyFromCoordinate(Coord coord) {
		return (int) Math.round(coord.getX()) / sideLengthInMeters + "," + (int) Math.round(coord.getY()) / sideLengthInMeters;
	}

	public double getNumberOfDataPoints() {
		checkConsistencyAfterDataCollectionPhase();
		return 0;
	}

	public void addGridInformation(GridDataItem gridValue) {
		checkConsistencyInDataCollectionPhase();

		inputData.push(gridValue);
	}

	public Double getValue(Coord coord) {
		checkConsistencyAfterDataCollectionPhase();

		if (valuesInGrid.keySet().size()==0){
			return null;
		} else {
			for (int i=0;i<Integer.MAX_VALUE;i++){
				Double averageValueOfFrame = getAverageValueOfFrame(coord,i);
				if (averageValueOfFrame!=null){
					return averageValueOfFrame;
				}
			}
		}
		
		DebugLib.stopSystemAndReportInconsistency("The grid size is chosen too small (running out of Integers...)");
		
		return 0.0;
	}

	public Double getAverageValueOfFrame(Coord coord, int degree) {
		if (degree == 0) {
			if (valuesInGrid.containsKey(getGridKeyFromCoordinate(coord))) {
				return valuesInGrid.get(getGridKeyFromCoordinate(coord));
			} else {
				return null;
			}
		}

		boolean someValueFound = false;
		double sumOfValues = 0;
		int sampleSize = 0;
		// TODO: make this more efficient (don't iterate throug all fields!!!!
		for (int i = - degree ; i < degree ; i++) {
			for (int j = -degree; j < degree; j++) {
				String tmpCoordKey = getGridKeyFromCoordinate(new CoordImpl(coord.getX() + i * sideLengthInMeters, coord.getY()
						+ j * sideLengthInMeters));
				if (valuesInGrid.containsKey(tmpCoordKey)) {
					someValueFound = true;
					sumOfValues += valuesInGrid.get(tmpCoordKey);
					sampleSize++;
				}
			}
		}

		if (someValueFound) {
			return sumOfValues / sampleSize;
		} else {
			return null;
		}
	}

	private void checkConsistencyInDataCollectionPhase() {
		if (!dataCollectionPhaseOn) {
			DebugLib.stopSystemAndReportInconsistency("This method should not be invoked, after completion of data collection phase");
		}
	}

	private void checkConsistencyAfterDataCollectionPhase() {
		if (dataCollectionPhaseOn) {
			DebugLib.stopSystemAndReportInconsistency("This method should not be invoked, before completion of data collection phase");
		}
	}
}
