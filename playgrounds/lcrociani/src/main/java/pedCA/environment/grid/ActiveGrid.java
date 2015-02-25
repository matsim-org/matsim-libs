package pedCA.environment.grid;

import java.io.File;
import java.io.IOException;



public abstract class ActiveGrid<T> extends Grid<T> {
	protected int step;
	
	public ActiveGrid(int rows, int cols) {
		super(rows, cols);
		step = 0;
	}
	
	public void update(){
		updateGrid();
		step++;
	}
	
	protected abstract void updateGrid();

	public ActiveGrid(String fileName) throws IOException {
		super(fileName);
		// TODO Auto-generated constructor stub
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