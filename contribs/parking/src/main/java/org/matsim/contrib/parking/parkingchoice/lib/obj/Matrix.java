package org.matsim.contrib.parking.parkingchoice.lib.obj;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.contrib.parking.parkingchoice.lib.DebugLib;
import org.matsim.contrib.parking.parkingchoice.lib.GeneralLib;


public class Matrix<T> {

	ArrayList<ArrayList<T>> matrix=new ArrayList<ArrayList<T>>();
	
	public ArrayList<T> getRow(int rowNumber){
		return matrix.get(rowNumber);
	}
	
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
		T t = matrix.get(row).get(column);
		if (t==null){
			return "";
		} else {
			return matrix.get(row).get(column).toString();
		}
	}
	
	public double getDouble(int row, int column){
		return new Double(getString(row,column));
	}
	
	public boolean getBoolean(int row, int column){
		return new Boolean(getString(row,column));
	}
	
	public int getInteger(int row, int column){
		return new Integer((int) Math.round(new Double(getString(row,column))));
	}
	
	public int convertDoubleToInteger(int row, int column){
		return (int) Math.round(getDouble(row, column));
	}
	
	public void addRow(ArrayList<T> row){
		matrix.add(row);
	}
	
	public void deleteRow(int row){
		matrix.remove(row);
	}
	
	public void replaceString(int row,int column, T value){
		try{
			matrix.get(row).remove(column);
			matrix.get(row).add(column, value);
		} catch (Exception e) {
			DebugLib.stopSystemAndReportInconsistency("(tried to add value outside of boundries - row:" + row + ",col:" + column + ",val:" + value);
		}
	}
	
	public void putString(int row,int column, T value){
		while (row>=getNumberOfRows()){
			matrix.add(new ArrayList<T>());
		}
		
		while (column>=getNumberOfColumnsInRow(row)){
			matrix.get(row).add(null);
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
	
	public void writeColumn(int columnIndex, String fileName){
		ArrayList<String> outputArrayList=new ArrayList<String>();
		for (int i=0;i<getNumberOfRows();i++){
			outputArrayList.add(getString(i, columnIndex));
		}
		GeneralLib.writeList(outputArrayList, fileName);
	}

	public Float getFloat(int row, int column){
		return new Float(getString(row,column));
	}
}
