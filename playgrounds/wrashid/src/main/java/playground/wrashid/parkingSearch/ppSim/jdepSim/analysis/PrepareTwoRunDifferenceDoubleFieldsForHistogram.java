package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;

public class PrepareTwoRunDifferenceDoubleFieldsForHistogram {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolderRunA="H:/data/experiments/parkingSearchOct2013/runs/run163/output/";
		String outputFolderRunB="H:/data/experiments/parkingSearchOct2013/runs/run164/output/";
		int startIteration=400;
		int endIteration=499;
		int iterationStep = 10;
		boolean ignoreCasesWithBothPPUse=true;
		String outputFile="C:/data/Dropbox/ETH/Projekte/STRC2014/experiments/compare different seed/hisogramData.txt";
		
		Matrix table=new Matrix();
		
		table.putString(0, 0, "pctScoreDifference");
		table.putString(0, 1, "pctWalkDistanceDifference");
		table.putString(0, 2, "pctSearchTimeDuration");
		table.putString(0, 3, "absScoreDifference");
		table.putString(0, 4, "absWalkDistanceDifference");
		table.putString(0, 5, "absSearchTimeDuration");
		
		for (int i=startIteration;i<=endIteration; i += iterationStep){
			Matrix eventsMatrixA = GeneralLib.readStringMatrix(CompareSelectedParkingPropertyOneRun.getEventsFileName(outputFolderRunA,i));	
			Matrix eventsMatrixB = GeneralLib.readStringMatrix(CompareSelectedParkingPropertyOneRun.getEventsFileName(outputFolderRunB,i));	
			
			String fieldName="score";
			boolean isAbsolute=true;
			int targetColumn=3;
			addData(eventsMatrixA,eventsMatrixB, fieldName,isAbsolute,targetColumn, ignoreCasesWithBothPPUse);
			
		}
	}

	private static void addData(Matrix eventsMatrixA,
			Matrix eventsMatrixB, String fieldName, boolean isAbsolute,
			int targetColumn, boolean ignoreCasesWithBothPPUse) {
		// TODO Auto-generated method stub
		
	}

}
