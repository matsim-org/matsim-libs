package playground.wrashid.lib.obj;

import java.util.ArrayList;
import java.util.LinkedList;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;

public class StringMatrix {

	ArrayList<ArrayList<String>> matrix=new ArrayList<ArrayList<String>>();
	
	public int getColumnIndex(String columnName){
		return matrix.get(0).indexOf(columnName);
	}
	
	public int getNumberOfRows(){
		return matrix.size();
	}
	
	public int getNumberOfColumnsInRow(int rowNumber){
		return matrix.get(rowNumber).size();
	}
	
	public String getString(int row, int column){
		return matrix.get(row).get(column);
	}
	
	public double getDouble(int row, int column){
		return new Double(getString(row,column));
	}
	
	public int getInteger(int row, int column){
		return new Integer((int) Math.round(new Double(getString(row,column))));
	}
	
	public int convertDoubleToInteger(int row, int column){
		return (int) Math.round(getDouble(row, column));
	}
	
	public void addRow(ArrayList<String> row){
		matrix.add(row);
	}
	
	public void replaceString(int row,int column, String value){
		try{
			matrix.get(row).remove(column);
			matrix.get(row).add(column, value);
		} catch (Exception e) {
			DebugLib.stopSystemAndReportInconsistency("(tried to add value outside of boundries - row:" + row + ",col:" + column + ",val:" + value);
		}
	}
	
	public void putString(int row,int column, String value){
		while (row>=getNumberOfRows()){
			matrix.add(new ArrayList<String>());
		}
		
		while (column>=getNumberOfColumnsInRow(row)){
			matrix.get(row).add("");
		}
		
		replaceString(row,column,value);
	}
	
	public void writeMatrix(String fileName){
		ArrayList<String> outputArrayList=new ArrayList<String>();
		StringBuffer sb=null;
		for (int i=0;i<getNumberOfRows();i++){
			sb=new StringBuffer();
			for (int j=0;j<getNumberOfColumnsInRow(i);j++){
				sb.append(getString(i,j));
				if (j<getNumberOfColumnsInRow(i)-1){
					sb.append("\t");
				}
				
			}
			outputArrayList.add(sb.toString());
		}
		
		GeneralLib.writeList(outputArrayList, fileName);
	}
}
