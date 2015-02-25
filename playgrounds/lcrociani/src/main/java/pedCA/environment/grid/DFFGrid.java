package pedCA.environment.grid;

import java.io.File;
import java.io.IOException;

import pedCA.environment.grid.neighbourhood.Neighbourhood;

public class DFFGrid extends Grid <Double>{

	final int radius;
	
	
	public DFFGrid(int rows, int cols, int radius) {
		super(rows, cols);
		this.radius = radius;
	}
	
	protected void diffusion(GridPoint gp){
		
		//TODO 
		/*
		Neighbourhood neighbourhood = getNeighbourhood(gp);
		for (int i = 0; i<neighbourhood.size();i++){
			GridPoint neighbour = neighbourhood.get(i);
			//double distance = 
		}
		*/
	}

	protected void decay(GridPoint gp){
		
	}
	
	public Neighbourhood getNeighbourhood(GridPoint gp){
		Neighbourhood neighbourhood = new Neighbourhood();
		int row_gp = gp.getY();
		int col_gp = gp.getX();
		for(int row=row_gp-radius;row<=row_gp+radius;row++)
			for (int col=col_gp-radius;col<=col_gp+radius;col++)
				if (neighbourCondition(row,col))
					neighbourhood.add(new GridPoint(col,row));
		return neighbourhood;
	} 
	
	@Override
	protected void loadFromCSV(File file) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveCSV(String path) throws IOException {
		// TODO Auto-generated method stub

	}


	
}
