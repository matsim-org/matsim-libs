package playground.sergioo.FacilitiesGenerator;

import util.algebra.MatrixND;
import util.algebra.MatrixNDImpl;
import util.fitting.FittingControl1D;
import util.fitting.FittingControlND;
import util.fitting.MaxFittingControlND;
import util.fitting.ProportionFittingControlND;
import util.fitting.TotalFittingControl1D;

public class FittingCapacities {

	//Attributes
	private int[] dimensions;
	private FittingControl1D os;
	private FittingControl1D fs;
	private FittingControlND fo;
	private FittingControlND f;
	
	//Constructor
	public FittingCapacities(int[] dimensions, MatrixND<Double> weightsFS, MatrixND<Double> quantitiesOS, MatrixND<Double> proportionsFO, MatrixND<Double> maxCapacityF) {
		this.dimensions = dimensions;
		os = new TotalFittingControl1D(quantitiesOS);
		MatrixND<Double> quantitiesSF = new MatrixNDImpl<Double>(new int[]{dimensions[0],dimensions[2]});
		for(int s=0; s<dimensions[2]; s++) {
			double totalStopQuantity = 0;
			for(int o=0; o<dimensions[1]; o++)
				totalStopQuantity += quantitiesOS.getElement(new int[]{o, s});
			for(int f=0; f<dimensions[0]; f++)
				quantitiesSF.setElement(new int[]{f, s}, weightsFS.getElement(new int[]{f, s})*totalStopQuantity);
		}
		fs = new TotalFittingControl1D(quantitiesSF);
		fo = new ProportionFittingControlND(proportionsFO, new int[]{2});
		f = new MaxFittingControlND(maxCapacityF);
	}
	
	//Methods
	public MatrixND<Double> run(int times) {
		MatrixND<Double> data = new MatrixNDImpl<Double>(dimensions, 1.0);
		for(int i=0; i<times; i++) {
			os.iterate(data, 0);
			fs.iterate(data, 1);
			fo.iterate(data, new int[]{1, 2});
			f.iterate(data, new int[]{1, 2});
		}
		return data;
	}
	public MatrixND<Double> run(double error) {
		MatrixND<Double> data = new MatrixNDImpl<Double>(dimensions, 1.0);
		while(!finish(error)) {
			os.iterate(data, 0);
			fs.iterate(data, 1);
			fo.iterate(data, new int[]{2});
			f.iterate(data, new int[]{1, 2});
		}
		return data;
	}
	private boolean finish(double error) {
		//TODO
		return false;
	}
	
}
