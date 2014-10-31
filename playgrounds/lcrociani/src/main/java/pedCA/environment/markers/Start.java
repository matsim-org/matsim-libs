package pedCA.environment.markers;

import java.util.ArrayList;

import pedCA.environment.grid.GridPoint;
import pedCA.utility.Lottery;

public class Start extends Marker{

	private static final long serialVersionUID = 1L;
	private Double frequency;
	private int totalPedestrians;
	private int generatedPedestrian = 0;
	
	public Start(ArrayList<GridPoint> cells){
		this(0,cells);
	}
	
	public Start(int totalPedestrians, ArrayList<GridPoint> cells){
		super(cells);
		this.totalPedestrians = totalPedestrians;
	}
	
	public Start(double frequency, int totalPedestrians, ArrayList<GridPoint> cells){
		super(cells);
		this.frequency=frequency;
		this.totalPedestrians = totalPedestrians;
	}
	
	public boolean canGenerate(){
		return generatedPedestrian<totalPedestrians;
	}
	
	public int toBeGenerated(){
		if(frequency==null)
			return totalPedestrians - generatedPedestrian;
		int result = frequency.intValue();
		double probability = frequency.doubleValue() - frequency.intValue();
		if (Lottery.simpleExtraction(probability))
			result++;
		return result;
	}
	
	public void notifyGeneration(){
		generatedPedestrian++;
	}
	
	public void setTotalPedestrians(int totalPedestrians){
		this.totalPedestrians = totalPedestrians;
	}
	
	public void setFrequency(double frequency){
		this.frequency = frequency;
	}
}
