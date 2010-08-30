package playground.wrashid.lib.obj;

import java.util.ArrayList;
import java.util.LinkedList;

public class StringMatrix {

	ArrayList<LinkedList<String>> matrix=new ArrayList<LinkedList<String>>();
	
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
	
	public void addRow(LinkedList<String> row){
		matrix.add(row);
	}
	
}
