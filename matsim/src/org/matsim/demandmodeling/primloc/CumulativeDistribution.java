package org.matsim.demandmodeling.primloc;

public class CumulativeDistribution {

	double[] xs;
	double[] ys;
	int numObservations;
	
	public CumulativeDistribution( double lowerBound, double upperBound, int numBins ){
		xs = new double[ numBins+1 ];
		ys = new double[ numBins+1 ];
		for( int i=0; i<numBins+1;i++)
			xs[i]=lowerBound+((upperBound-lowerBound)*i)/numBins;
		ys[numBins]=1.0;
	}
	
	public CumulativeDistribution( double[] xs, double ys[] ){
		assert( xs.length == ys.length );
		this.xs = xs;
		this.ys = ys;
		numObservations = 1;
	}
	
	public void addObservation( double x ){
		addObservations( x, 1.0 );
	}
	
	public void addObservations( double x, double y ){
		int idx=0;
		while( x>=xs[idx] && idx<xs.length-1)
			idx++;
		for( int i=0; i<xs.length;i++){
			if( i< idx )
				ys[i]=(ys[i]*numObservations)/(numObservations+y);
			else
				ys[i]=(ys[i]*numObservations+y)/(numObservations+y);
		}
		numObservations += y;
	}
	
	public void print(){
		for( int i=0; i<xs.length;i++)
			System.out.println( xs[i]+"\t"+ys[i]);
	}
}
