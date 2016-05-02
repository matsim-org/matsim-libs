package pedCA.environment.grid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import pedCA.utility.Constants;

public class EnvironmentGrid extends Grid<Integer>{

	public EnvironmentGrid(int rows, int cols) {
		super(rows, cols);
	}
		
	public EnvironmentGrid(File environmentFile) throws IOException{
		super(environmentFile);
	}
	
	public EnvironmentGrid(String path) throws IOException{
		super(path+"/input/environment/environmentGrid.csv");
	}
	
	public void setCellValue(int row, int col, int value){
		try{
			get(row,col).set(0,value);
		}catch(IndexOutOfBoundsException e){
			get(row,col).add(value);
		}
	}
	
	public int getCellValue(GridPoint p){
		return getCellValue(p.getY(),p.getX());
	}
	
	public int getCellValue(int row, int col){
		Integer result = get(row, col).get(0);
		if (result==null)
			return 0;
		return result;
	}
	
	public boolean isWalkable(int row, int col) {
		return getCellValue(row, col)!=Constants.ENV_OBSTACLE;
	}
	
	private boolean isWalkable(GridPoint cell) {
		return isWalkable(cell.getY(),cell.getX());
	}
	
	@Override
	protected void loadFromCSV(File environmentFile) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(environmentFile));
		String line = br.readLine();
		for (int row = 0;line!=null;row++){
			addRow();
			StringTokenizer st = new StringTokenizer(line,",");
			if (st.countTokens()==1)
				st = new StringTokenizer(line, ";");
			if (st.countTokens()==1)
				st = new StringTokenizer(line, "\t");
			String value_s;
			do{
				value_s = st.nextToken();
				int field_value = Integer.parseInt(value_s);
				addElementAt(row, field_value);
			}while(st.countTokens()>0);
			line = br.readLine();
		}
		br.close();
	}
	
	
	
	@Override
	public void saveCSV(String path) throws IOException {
		path = path+"/input/environment/";
		new File(path).mkdirs();
		File file = new File(path+"environmentGrid.csv");
		if (!file.exists()) {
			file.createNewFile();
		} 
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		for(int i=0;i<getRows();i++){
			String line="";
			for(int j=0;j<getColumns();j++)
				line+=getCellValue(i,j)+",";
			line+="\n";
			bw.write(line);
		}		
		bw.close();
	}
	
	public boolean belongsToExit(GridPoint cell){
		return (cell.getY()==0 || cell.getX()==0 || cell.getY() == getRows()-1 || cell.getX() == getColumns()-1) && isWalkable(cell);
	}
	
	public boolean belongsToTacticalDestination(GridPoint cell){
		return getCellValue(cell) == Constants.ENV_TACTICAL_DESTINATION || getCellValue(cell) == Constants.ENV_STAIRS_BORDER;
	}

	public boolean isStairsBorder(GridPoint cell) {
		return getCellValue(cell) == Constants.ENV_STAIRS_BORDER;
	}

}
