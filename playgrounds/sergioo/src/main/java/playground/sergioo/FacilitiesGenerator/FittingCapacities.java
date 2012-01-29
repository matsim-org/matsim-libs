package playground.sergioo.FacilitiesGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import util.algebra.Matrix1DImpl;
import util.algebra.Matrix2DImpl;
import util.algebra.Matrix3DImpl;
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
		MatrixND<Double> quantitiesFS = new Matrix2DImpl(new int[]{dimensions[0],dimensions[2]});
		for(int s=0; s<dimensions[2]; s++) {
			double totalStopQuantity = 0;
			for(int o=0; o<dimensions[1]; o++)
				totalStopQuantity += quantitiesOS.getElement(new int[]{o, s});
			for(int f=0; f<dimensions[0]; f++) {
				double quantity = weightsFS.getElement(new int[]{f, s})*totalStopQuantity;
				if(quantity == 0)
					quantity = Double.MIN_VALUE;
				quantitiesFS.setElement(new int[]{f, s}, quantity);
			}
		}
		fs = new TotalFittingControl1D(quantitiesFS);
		fo = new ProportionFittingControlND(proportionsFO, new int[]{2});
		f = new MaxFittingControlND(maxCapacityF);
		System.out.println("Heap memory: "+Runtime.getRuntime().freeMemory()+" of "+Runtime.getRuntime().maxMemory());
	}
	
	//Methods
	public MatrixND<Double> run(int times) {
		System.out.println("Start fitting!");
		System.out.println("Free heap memory: "+Runtime.getRuntime().freeMemory()+" of "+Runtime.getRuntime().maxMemory());
		Matrix3DImpl data = new Matrix3DImpl(dimensions, 1.0);
		System.out.println("Free heap memory: "+Runtime.getRuntime().freeMemory()+" of "+Runtime.getRuntime().maxMemory());
		for(int i=0; i<times; i++) {
			long time = System.currentTimeMillis();
			fixMatrix(data);
			totalIterateOS(data);
			System.out.println("Iteration "+i+": "+(System.currentTimeMillis()-time)/1000);
			fixMatrix(data);
			totalIterateFS(data);
			System.out.println("Iteration "+i+": "+(System.currentTimeMillis()-time)/1000);
			fixMatrix(data);
			proportionIterateFO(data);
			System.out.println("Iteration "+i+": "+(System.currentTimeMillis()-time)/1000);
			fixMatrix(data);
			maxIterateF(data);
			System.out.println("Iteration "+i+": "+(System.currentTimeMillis()-time)/1000);
			System.out.println("Free heap memory: "+Runtime.getRuntime().freeMemory()+" of "+Runtime.getRuntime().maxMemory());
		}
		System.out.println("Finish fitting!");
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
	private void fixMatrix(Matrix3DImpl data) {
		for(int f=0; f<dimensions[0]; f++)
			for(int o=0; o<dimensions[1]; o++)
				for(int s=0; s<dimensions[2]; s++)
					if(data.getElement(f, o, s)==0)
						data.setElement(f, o, s, Double.MIN_VALUE);
	}
	private void totalIterateOS(Matrix3DImpl data) {
		double[][][] matrix = data.getData();
		Matrix2DImpl totalsOS = (Matrix2DImpl) os.getControlConstants();
		for(int o=0; o<dimensions[1]; o++)
			for(int s=0; s<dimensions[2]; s++) {
				double totalOS = totalsOS.getElement(o, s);
				double totalF = 0;
				for(int f=0; f<dimensions[0]; f++)
					totalF += matrix[f][o][s];
				if(!(totalF==0 && totalOS==0))
					if(totalF==0)
						for(int f=0; f<dimensions[0]; f++)
							matrix[f][o][s] = totalOS/dimensions[0];
					else
						for(int f=0; f<dimensions[0]; f++) {
							double prev = matrix[f][o][s];
							matrix[f][o][s] = (matrix[f][o][s]/totalF)*totalOS;
							if(data.getElement(f, o, s).isNaN()) {
								System.out.println("Paila"+f+","+o+","+s+";"+prev+","+totalF+","+totalOS);
								try {
									new BufferedReader(new InputStreamReader(System.in)).readLine();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
			}
	}
	private void totalIterateFS(Matrix3DImpl data) {
		double[][][] matrix = data.getData();
		Matrix2DImpl totalsFS = (Matrix2DImpl) fs.getControlConstants();
		for(int f=0; f<dimensions[0]; f++)
			for(int s=0; s<dimensions[2]; s++) {
				double totalFS = totalsFS.getElement(f, s);
				double totalO = 0;
				for(int o=0; o<dimensions[1]; o++)
					totalO += matrix[f][o][s];
				if(!(totalO==0 && totalFS==0))
					if(totalO==0)
						for(int o=0; o<dimensions[1]; o++) {
							matrix[f][o][s] = totalFS/dimensions[1];
							if(totalFS==0)
								System.out.println("FS0");
							if(dimensions[1]==0)
								System.out.println("FS1");
						}
					else
						for(int o=0; o<dimensions[1]; o++)
							matrix[f][o][s] = (matrix[f][o][s]/totalO)*totalFS;
			}
	}
	private void proportionIterateFO(Matrix3DImpl data) {
		double[][][] matrix = data.getData();
		Matrix2DImpl proportionsFO = (Matrix2DImpl) fo.getControlConstants();
		for(int f=0; f<dimensions[0]; f++)
			for(int o=0; o<dimensions[1]; o++) {
				double proportionFO = proportionsFO.getElement(f, o);
				double totalOS = 0;
				for(int o2=0; o2<dimensions[1]; o2++)
					for(int s=0; s<dimensions[2]; s++)
						totalOS += matrix[f][o2][s];
				double totalS = 0;
				for(int s=0; s<dimensions[2]; s++)
					totalS+=matrix[f][o][s];
				if(!(totalS==0 && totalOS*proportionFO==0))
					if(totalS==0)
						for(int s=0; s<dimensions[2]; s++) {
							matrix[f][o][s] = totalOS*proportionFO/dimensions[2];
							if(totalOS==0)
								System.out.println("FO0");
							if(proportionFO==0)
								System.out.println("FO1");
							if(dimensions[2]==0)
								System.out.println("FO2");
						}
					else
						for(int s=0; s<dimensions[2]; s++)
							matrix[f][o][s] = (matrix[f][o][s]/totalS)*totalOS*proportionFO;
			}
	}
	private void maxIterateF(Matrix3DImpl data) {
		double[][][] matrix = data.getData();
		Matrix1DImpl maxsF = (Matrix1DImpl) f.getControlConstants();
		for(int f=0; f<dimensions[0]; f++) {
			double maxF = maxsF.getElement(f);
			double totalOS = 0;
			for(int o=0; o<dimensions[1]; o++)
				for(int s=0; s<dimensions[2]; s++)
					totalOS += matrix[f][o][s];
			if(totalOS>maxF && !(totalOS==0 && maxF==0))
				for(int o=0; o<dimensions[1]; o++)
					for(int s=0; s<dimensions[2]; s++)
						matrix[f][o][s] = (matrix[f][o][s]/totalOS)*maxF;
		}
	}
	private boolean finish(double error) {
		//TODO
		return false;
	}
	
}
