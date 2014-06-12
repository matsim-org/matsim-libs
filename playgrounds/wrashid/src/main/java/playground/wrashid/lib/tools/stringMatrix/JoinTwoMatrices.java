package playground.wrashid.lib.tools.stringMatrix;

import java.util.ArrayList;

import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.contrib.parking.lib.obj.Matrix;


public class JoinTwoMatrices {

	/**

	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Matrix leftMatrix = GeneralLib.readStringMatrix("C:/data/parkingSearch/psim/zurich/output/run20/output/ITERS/0.parkingEvents.txt.gz");
		Matrix rightMatrix = GeneralLib.readStringMatrix("C:/data/parkingSearch/psim/zurich/output/run20/output/parkingProperties.txt");
		String outputPathJoinedMatrix="C:/data/parkingSearch/psim/zurich/tmp/joinedMatrix.txt";

		String joinFieldNameLeftMatrix="FacilityId";
		String joinFieldNameRightMatrix="parkingFacilityId";
		
		int joinFieldIndexLeftMatrix=leftMatrix.getColumnIndex(joinFieldNameLeftMatrix);
		int joinFieldIndexRightMatrix=rightMatrix.getColumnIndex(joinFieldNameRightMatrix);
		
		IntegerValueHashMap<String> keyToIndexMapping = initHashMap(
				rightMatrix, joinFieldIndexRightMatrix);
		
		updateHeader(leftMatrix, rightMatrix);
		
		joinMatrices(leftMatrix, rightMatrix, joinFieldIndexLeftMatrix,
				keyToIndexMapping);
		
		leftMatrix.writeMatrix(outputPathJoinedMatrix);
	}

	private static IntegerValueHashMap<String> initHashMap(
			Matrix rightMatrix, int joinFieldIndexRightMatrix) {
		IntegerValueHashMap<String> keyToIndexMapping=new IntegerValueHashMap<String>();
		
		for (int i=1;i<rightMatrix.getNumberOfRows();i++){
			if (keyToIndexMapping.containsKey(rightMatrix.getString(i, joinFieldIndexRightMatrix))){
				DebugLib.stopSystemAndReportInconsistency("field needs to be unique in the right hand matrix for this to work");
			}
			keyToIndexMapping.set(rightMatrix.getString(i, joinFieldIndexRightMatrix), i);
		}
		return keyToIndexMapping;
	}

	private static void joinMatrices(Matrix leftMatrix,
			Matrix rightMatrix, int joinFieldIndexLeftMatrix,
			IntegerValueHashMap<String> keyToIndexMapping) {
		for (int i=1;i<leftMatrix.getNumberOfRows();i++){
			ArrayList<String> currrentRowLeftMatrix = leftMatrix.getRow(i);
			String joinFieldValueLeftMatrix=currrentRowLeftMatrix.get(joinFieldIndexLeftMatrix);
			ArrayList<String> matchingRowRightMatrix = rightMatrix.getRow(keyToIndexMapping.get(joinFieldValueLeftMatrix));
			
			for (int j=0;j<rightMatrix.getNumberOfColumnsInRow(0);j++){
				currrentRowLeftMatrix.add(matchingRowRightMatrix.get(j));
			}
		}
	}

	private static void updateHeader(Matrix leftMatrix,
			Matrix rightMatrix) {
		ArrayList<String> titleRowLeftMatrix = leftMatrix.getRow(0);
		for (int i=0;i<rightMatrix.getNumberOfColumnsInRow(0);i++){
			titleRowLeftMatrix.add("RHT_" + rightMatrix.getString(0, i));
		}
	}

}
