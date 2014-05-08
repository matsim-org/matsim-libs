package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.contrib.parking.lib.obj.list.Lists;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;

public class PrepareTwoRunDifferenceFloatFieldsForHistogram {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolderRunA = "H:/data/experiments/parkingSearchOct2013/runs/run164/output/";
		String outputFolderRunB = "H:/data/experiments/parkingSearchOct2013/runs/run135/output/";
		int startIteration = 400;
		int endIteration = 405;
		int iterationStep = 10;
		boolean ignoreCasesWithBothPPUse = true;
		String outputFolder = "c:/tmp/comparisonRun164And135/";

		ArrayList<Float> pctScoreDifference = new ArrayList<Float>();
		ArrayList<Float> pctWalkDistanceDifference = new ArrayList<Float>();
		ArrayList<Float> pctSearchTimeDuration = new ArrayList<Float>();
		ArrayList<Float> absScoreDifference = new ArrayList<Float>();
		ArrayList<Float> absWalkDistanceDifference = new ArrayList<Float>();
		ArrayList<Float> absSearchTimeDuration = new ArrayList<Float>();

		for (int i = startIteration; i <= endIteration; i += iterationStep) {
			Matrix eventsMatrixA = GeneralLib
					.readStringMatrix(CompareSelectedParkingPropertyOneRun
							.getEventsFileName(outputFolderRunA, i));
			Matrix eventsMatrixB = GeneralLib
					.readStringMatrix(CompareSelectedParkingPropertyOneRun
							.getEventsFileName(outputFolderRunB, i));

			boolean isAbsolute = false;
			collectData(eventsMatrixA, eventsMatrixB, "score", isAbsolute,ignoreCasesWithBothPPUse, pctScoreDifference);
			collectData(eventsMatrixA, eventsMatrixB, "walkDistance", isAbsolute,ignoreCasesWithBothPPUse, pctWalkDistanceDifference);
			collectData(eventsMatrixA, eventsMatrixB, "parkingSearchDuration", isAbsolute,ignoreCasesWithBothPPUse, pctSearchTimeDuration);
			
			isAbsolute = true;
			collectData(eventsMatrixA, eventsMatrixB, "score", isAbsolute,ignoreCasesWithBothPPUse, absScoreDifference);
			collectData(eventsMatrixA, eventsMatrixB, "walkDistance", isAbsolute,ignoreCasesWithBothPPUse, absWalkDistanceDifference);
			collectData(eventsMatrixA, eventsMatrixB, "parkingSearchDuration", isAbsolute,ignoreCasesWithBothPPUse, absSearchTimeDuration);
			
			System.out.println("iteration " + i + " processed.");
		}
		
		
		String tempOutputPath = writeTempDataAndGetPath(pctScoreDifference,outputFolder,"pctScoreDifference.txt");
		new RIntegration().generateHistogram(tempOutputPath, outputFolder + "pctScoreDifference.png", "Score Difference pct", "score difference pct", "frequency", outputFolder + "pctScoreDifference.batch");
		
		tempOutputPath = writeTempDataAndGetPath(pctWalkDistanceDifference,outputFolder,"pctWalkDistanceDifference.txt");
		new RIntegration().generateHistogram(tempOutputPath, outputFolder + "pctWalkDistanceDifference.png", "walk distance difference pct", "walk distance difference pct", "frequency", outputFolder + "pctWalkDistanceDifference.batch");
		
		tempOutputPath = writeTempDataAndGetPath(pctSearchTimeDuration,outputFolder,"pctSearchTimeDuration.txt");
		new RIntegration().generateHistogram(tempOutputPath, outputFolder + "pctSearchTimeDuration.png", "Search Time Difference pct", "search time difference pct", "frequency", outputFolder + "pctSearchTimeDuration.batch");
		
		tempOutputPath = writeTempDataAndGetPath(absScoreDifference,outputFolder,"absScoreDifference.txt");
		new RIntegration().generateHistogram(tempOutputPath, outputFolder + "absScoreDifference.png", "Score Difference Abs", "score difference abs", "frequency", outputFolder + "absScoreDifference.batch");
		
		tempOutputPath = writeTempDataAndGetPath(absWalkDistanceDifference,outputFolder,"absWalkDistanceDifference.txt");
		new RIntegration().generateHistogram(tempOutputPath, outputFolder + "absWalkDistanceDifference.png", "Walk Distance Difference Abs", "walk distance difference abs [m]", "frequency", outputFolder + "absWalkDistanceDifference.batch");
		
		tempOutputPath = writeTempDataAndGetPath(absSearchTimeDuration,outputFolder,"absSearchTimeDuration.txt");
		new RIntegration().generateHistogram(tempOutputPath, outputFolder + "absSearchTimeDuration.png", "Search Time Difference Abs", "search time difference abs [s]", "frequency", outputFolder + "absSearchTimeDuration.batch");
			
	}

	private static String writeTempDataAndGetPath(
			ArrayList<Float> pctScoreDifference, String outputFolder, String fileName) {
		String outputPath=outputFolder+fileName;
		GeneralLib.writeList(Lists.converFloatToStringArrayList(pctScoreDifference), outputPath);
		System.out.println(outputPath);
		return outputPath;
	}

	private static void collectData(Matrix matrixA, Matrix matrixB,
			String fieldName, boolean isAbsolute,
			boolean ignoreCasesWithBothPPUse, ArrayList<Float> outputArray) {

		// persondId, legIndex, rowId
		TwoHashMapsConcatenated<Id, Integer, Integer> indexMatrixB = CompareSelectedParkingPropertyOneRun
				.getIndex(matrixB);

		int indexPersonId = matrixA.getColumnIndex("personId");
		int indexLeg = matrixA.getColumnIndex("legIndex");

		int indexSelectedField = matrixA.getColumnIndex(fieldName);

		for (int i = 1; i < matrixA.getNumberOfRows(); i++) {
			Id personId = new IdImpl(matrixA.getString(i, indexPersonId));
			int legIndex = matrixA.getInteger(i, indexLeg);

			Float selectedFieldA = matrixA.getFloat(i, indexSelectedField);
			Float selectedFieldB = matrixB.getFloat(
					indexMatrixB.get(personId, legIndex), indexSelectedField);

			if (ignoreCasesWithBothPPUse) {
				int indexFacilityId = matrixA.getColumnIndex("FacilityId");
				String facilityIdA = matrixA.getString(i, indexFacilityId);
				String facilityIdB = matrixB.getString(
						indexMatrixB.get(personId, legIndex), indexFacilityId);

				if (!(facilityIdA.toString().contains("private") && facilityIdB
						.toString().contains("private"))) {
					if (isAbsolute){
						outputArray.add(Math.abs(selectedFieldA) - Math.abs(selectedFieldB));
					} else {
						if (selectedFieldA!=0.0 && selectedFieldA!=0.0){
							outputArray.add((Math.abs(selectedFieldA) - Math.abs(selectedFieldB))/(Math.abs(selectedFieldA)+Math.abs(selectedFieldB)));
						} else {
							outputArray.add(0.0f);
						}
					}
				}
			} else {
				if (isAbsolute){
					outputArray.add(Math.abs(selectedFieldA) - Math.abs(selectedFieldB));
				} else {
					if (selectedFieldA!=0.0 && selectedFieldA!=0.0){
						outputArray.add((Math.abs(selectedFieldA) - Math.abs(selectedFieldB))/(Math.abs(selectedFieldA)+Math.abs(selectedFieldB)));
					} else {
						outputArray.add(0.0f);
					}
				}
			}
		}
	}

}
