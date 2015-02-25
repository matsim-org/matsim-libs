package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import java.io.File;
import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.contrib.parking.lib.obj.list.Lists;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;

public class PrepareCumulativeGraphForTwoRuns_DifferenceFloatFields {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolderRunA = "h:/data/experiments/parkingSearchOct2013/runs/run403/output/";
		String outputFolderRunB = "h:/data/experiments/parkingSearchOct2013/runs/run404/output/";
		int startIteration = 480;
		int endIteration = startIteration+5;
		int iterationStep = 10;
		boolean ignoreCasesWithBothPPUse = true;
		String outputFolder = "C:/data/Dropbox/ETH/Projekte/STRC2014/experiments/comparison different strategy groups/comparisonRun403And404-It480/";
		double cutPctAccumulationFreq=0.95;
		
		int runNumber=199;
		int offSet=488;
		boolean compareIterationsOfSameRun=false; // in this case only 'outputFolderRunA' needs to be provided ('outputFolderRunB' one is ignored)

		
		if (compareIterationsOfSameRun){
			//outputFolder = "C:/data/Dropbox/ETH/Projekte/STRC2014/experiments/compare different seeds/run199Selfs/";
			//outputFolder += "comparisonRun" + runNumber + "ToItSelf_It" + startIteration + "AndOffSet" +   offSet + "/";
			
			endIteration = startIteration+1;
			iterationStep = 10;
					
			new File(outputFolder).mkdir();
		}
	
		
		ArrayList<Float> pctScoreDifference = new ArrayList<Float>();
		ArrayList<Float> pctWalkDistanceDifference = new ArrayList<Float>();
		ArrayList<Float> pctSearchTimeDuration = new ArrayList<Float>();
		ArrayList<Float> pctParkingCost = new ArrayList<Float>();
		ArrayList<Float> absScoreDifference = new ArrayList<Float>();
		ArrayList<Float> absWalkDistanceDifference = new ArrayList<Float>();
		ArrayList<Float> absSearchTimeDuration = new ArrayList<Float>();
		ArrayList<Float> absParkingCost = new ArrayList<Float>();

		for (int i = startIteration; i <= endIteration; i += iterationStep) {
			Matrix eventsMatrixA=null;
			Matrix eventsMatrixB=null;
			
			if (compareIterationsOfSameRun){
				eventsMatrixA = GeneralLib
						.readStringMatrix(CompareSelectedParkingPropertyOneRun
								.getEventsFileName(outputFolderRunA, i));
				eventsMatrixB = GeneralLib
						.readStringMatrix(CompareSelectedParkingPropertyOneRun
								.getEventsFileName(outputFolderRunA, i+offSet));
			} else {
				eventsMatrixA = GeneralLib
						.readStringMatrix(CompareSelectedParkingPropertyOneRun
								.getEventsFileName(outputFolderRunA, i));
				eventsMatrixB = GeneralLib
						.readStringMatrix(CompareSelectedParkingPropertyOneRun
								.getEventsFileName(outputFolderRunB, i));
			}
			
			boolean isAbsolute = false;
			collectData(eventsMatrixA, eventsMatrixB, "score", isAbsolute,ignoreCasesWithBothPPUse, pctScoreDifference);
			collectData(eventsMatrixA, eventsMatrixB, "walkDistance", isAbsolute,ignoreCasesWithBothPPUse, pctWalkDistanceDifference);
			collectData(eventsMatrixA, eventsMatrixB, "parkingSearchDuration", isAbsolute,ignoreCasesWithBothPPUse, pctSearchTimeDuration);
			collectData(eventsMatrixA, eventsMatrixB, "parkingCost", isAbsolute,ignoreCasesWithBothPPUse, pctParkingCost);
			
			isAbsolute = true;
			collectData(eventsMatrixA, eventsMatrixB, "score", isAbsolute,ignoreCasesWithBothPPUse, absScoreDifference);
			collectData(eventsMatrixA, eventsMatrixB, "walkDistance", isAbsolute,ignoreCasesWithBothPPUse, absWalkDistanceDifference);
			collectData(eventsMatrixA, eventsMatrixB, "parkingSearchDuration", isAbsolute,ignoreCasesWithBothPPUse, absSearchTimeDuration);
			collectData(eventsMatrixA, eventsMatrixB, "parkingCost", isAbsolute,ignoreCasesWithBothPPUse, absParkingCost);
			
			System.out.println("iteration " + i + " processed.");
		}
		
		
		String tempOutputPath = writeTempDataAndGetPath(pctScoreDifference,outputFolder,"pctScoreDifference.txt");
		new RIntegration().generateCumulativeFrequencyGraph(tempOutputPath, outputFolder + "pctScoreDifference.png", "Score Difference pct", "score difference pct", "frequency", outputFolder + "pctScoreDifference.batch",cutPctAccumulationFreq);
		
		tempOutputPath = writeTempDataAndGetPath(pctWalkDistanceDifference,outputFolder,"pctWalkDistanceDifference.txt");
		new RIntegration().generateCumulativeFrequencyGraph(tempOutputPath, outputFolder + "pctWalkDistanceDifference.png", "walk distance difference pct", "walk distance difference pct", "frequency", outputFolder + "pctWalkDistanceDifference.batch",cutPctAccumulationFreq);
		
		tempOutputPath = writeTempDataAndGetPath(pctSearchTimeDuration,outputFolder,"pctSearchTimeDuration.txt");
		new RIntegration().generateCumulativeFrequencyGraph(tempOutputPath, outputFolder + "pctSearchTimeDuration.png", "Search Time Difference pct", "search time difference pct", "frequency", outputFolder + "pctSearchTimeDifference.batch",cutPctAccumulationFreq);
		
		tempOutputPath = writeTempDataAndGetPath(pctParkingCost,outputFolder,"pctParkingCost.txt");
		new RIntegration().generateCumulativeFrequencyGraph(tempOutputPath, outputFolder + "pctParkingCost.png", "Parking Cost Difference pct", "parking cost difference pct", "frequency", outputFolder + "pctParkingCostDifference.batch",cutPctAccumulationFreq);
		
		tempOutputPath = writeTempDataAndGetPath(absScoreDifference,outputFolder,"absScoreDifference.txt");
		new RIntegration().generateCumulativeFrequencyGraph(tempOutputPath, outputFolder + "absScoreDifference.png", "Score Difference Abs", "score difference abs", "frequency", outputFolder + "absScoreDifference.batch",cutPctAccumulationFreq);
		
		tempOutputPath = writeTempDataAndGetPath(absWalkDistanceDifference,outputFolder,"absWalkDistanceDifference.txt");
		new RIntegration().generateCumulativeFrequencyGraph(tempOutputPath, outputFolder + "absWalkDistanceDifference.png", "Walk Distance Difference Abs", "walk distance difference abs [m]", "frequency", outputFolder + "absWalkDistanceDifference.batch",cutPctAccumulationFreq);
		
		tempOutputPath = writeTempDataAndGetPath(absSearchTimeDuration,outputFolder,"absSearchTimeDuration.txt");
		new RIntegration().generateCumulativeFrequencyGraph(tempOutputPath, outputFolder + "absSearchTimeDuration.png", "Search Time Difference Abs", "search time difference abs [s]", "frequency", outputFolder + "absSearchTimeDifference.batch",cutPctAccumulationFreq);
		
		tempOutputPath = writeTempDataAndGetPath(absParkingCost,outputFolder,"absParkingCost.txt");
		new RIntegration().generateCumulativeFrequencyGraph(tempOutputPath, outputFolder + "absParkingCost.png", "Parking Cost Difference Abs", "parking cost difference abs [CHF]", "frequency", outputFolder + "absParkingCostDifference.batch",cutPctAccumulationFreq);
		
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
		TwoHashMapsConcatenated<Id<Person>, Integer, Integer> indexMatrixB = CompareSelectedParkingPropertyOneRun
				.getIndex(matrixB);

		int indexPersonId = matrixA.getColumnIndex("personId");
		int indexLeg = matrixA.getColumnIndex("legIndex");

		int indexSelectedField = matrixA.getColumnIndex(fieldName);

		for (int i = 1; i < matrixA.getNumberOfRows(); i++) {
			Id<Person> personId = Id.create(matrixA.getString(i, indexPersonId), Person.class);
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
						.toString().contains("private") || facilityIdA.toString().contains("publicPOutside") && facilityIdB
						.toString().contains("publicPOutside"))) {
					if (isAbsolute){
						outputArray.add(Math.abs(Math.abs(selectedFieldA) - Math.abs(selectedFieldB)));
					} else {
						if (selectedFieldA!=0.0 && selectedFieldA!=0.0){
							outputArray.add(Math.abs((Math.abs(selectedFieldA) - Math.abs(selectedFieldB))/(Math.abs(selectedFieldA)+Math.abs(selectedFieldB))));
						} else {
							outputArray.add(0.0f);
						}
					}
				}
			} else {
				if (isAbsolute){
					outputArray.add(Math.abs(Math.abs(selectedFieldA) - Math.abs(selectedFieldB)));
				} else {
					if (selectedFieldA!=0.0 && selectedFieldA!=0.0){
						outputArray.add(Math.abs((Math.abs(selectedFieldA) - Math.abs(selectedFieldB))/(Math.abs(selectedFieldA)+Math.abs(selectedFieldB))));
					} else {
						outputArray.add(0.0f);
					}
				}
			}
		}
	}

}
