package others.sergioo.util.fitting;

import java.io.File;

import others.sergioo.util.algebra.MatrixND;
import others.sergioo.util.algebra.MatrixNDImpl;

public class FittingData {

	//Attributes
	private int[] dimensions;
	private FittingControl1D[] fittingControls;
	
	//Constructor
	public FittingData(int[] dimensions, FittingControl1D[] fittingControls) {
		this.dimensions = dimensions;
		this.fittingControls = fittingControls;
	}
	public FittingData(File file) {
		//TODO
	}
	
	//Methods
	public MatrixND<Double> run(int times) {
		MatrixND<Double> data = new MatrixNDImpl<Double>(dimensions, 1.0);
		for(int i=0; i<times; i++)
			for(int j=0; j<fittingControls.length; j++)
				fittingControls[j].iterate(data, j);
		return data;
	}
	public MatrixND<Double> run(double error) {
		MatrixND<Double> data = new MatrixNDImpl<Double>(dimensions, 1.0);
		while(!finish(error))
			for(int j=0; j<fittingControls.length; j++)
				fittingControls[j].iterate(data, j);
		return data;
	}
	private boolean finish(double error) {
		//TODO
		return false;
	}
	
	//Test main
	public static void main(String[] args) {
		int[] dimensions = new int[] {3,2};
		FittingControl1D[] fittingControls = new FittingControl1D[dimensions.length];
		MatrixND<Double> controlConstants1=new MatrixNDImpl<Double>(new int[]{2});
		controlConstants1.setElement(new int[]{0}, 100.0);
		controlConstants1.setElement(new int[]{1}, 200.0);
		fittingControls[0]=new TotalFittingControl1D(controlConstants1);
		MatrixND<Double> controlConstants2=new MatrixNDImpl<Double>(new int[]{3,2});
		controlConstants2.setElement(new int[]{0,0}, 0.7);
		controlConstants2.setElement(new int[]{0,1}, 0.3);
		controlConstants2.setElement(new int[]{1,0}, 0.6);
		controlConstants2.setElement(new int[]{1,1}, 0.4);
		controlConstants2.setElement(new int[]{2,0}, 0.2);
		controlConstants2.setElement(new int[]{2,1}, 0.8);
		fittingControls[1]=new ProportionFittingControl1D(controlConstants2);
		FittingData fittingData = new FittingData(dimensions, fittingControls);
		MatrixND<Double> result=fittingData.run(50);
		for(int i=0; i<3; i++) { 
			for(int j=0; j<2; j++)
				System.out.print(result.getElement(new int[]{i,j})+" ");
			System.out.println();
		}
	}
	
}
