package org.matsim.demandmodeling.primloc;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.TreeMap;
import org.matsim.testcases.MatsimTestCase;

import Jama.Matrix;


public class PrimlocEngineTest  extends MatsimTestCase {

private final static String testPropertyFile = "test/input/org/matsim/demandmodeling/primloc/PrimaryLocationChoice.xml";

	PrimlocCore core = new PrimlocCore();
	
	public void testPLCM(){
		
		System.out.println("Starting Primary Location Choice Model (PLCM)");
		System.out.println("PLCM: Looking for PrimaryLocationChoice.xml config file of the Zurich sample dataset in " + testPropertyFile);
		

		if( new File( testPropertyFile ).exists() ){
			System.out.println("PLCM: ... config found");
			String outputDirectory = "test/output/" + this.getClass().getCanonicalName().replace('.', '/') + "/" + getName() + "/";
			String outputfile = outputDirectory + "/rent_patterns_testPLCM:txt";
			standAloneRun( testPropertyFile, outputfile );
		}
		else{
			System.err.println("PLCM: Could not find a suitable data set");
		}
	}
	
	void standAloneRun( String propFile, String outputFile ){
		TreeMap<Integer,Integer> zonemap = initProperties( propFile ); // model_ID to internal_ID
	
		core.setCalibrationError( new PrimlocCalibrationErrorMatrix( gravityModelMatrix() ) );	
		core.runCalibrationProcess();

		System.out.println("PLCM: writing rent patterns to:" + new File(outputFile).getAbsolutePath() );
		
		PrimlocIO.saveResults( core.X, zonemap, outputFile);
		System.out.println("PLCM: done");
	}
	
	TreeMap<Integer,Integer> initProperties( String propertyFile ){

		String zoneFileName=null;
		String costFileName=null;
		String homesFileName=null;
		String jobsFileName=null;

		Properties props = new Properties();

		
		try{
			props.loadFromXML( new FileInputStream(propertyFile) );
			zoneFileName = props.getProperty("zoneFileName");
			costFileName = props.getProperty("costFileName");
			homesFileName = props.getProperty("homesFileName");
			jobsFileName = props.getProperty("jobsFileName");
			
			core.maxiter = Integer.parseInt(props.getProperty("maxiter"));
			core.mu = Double.parseDouble(props.getProperty("mu"));
			core.theta = Double.parseDouble( props.getProperty("theta"));
			core.threshold1 = Double.parseDouble(props.getProperty("threshold1"));
			core.threshold2 = Double.parseDouble(props.getProperty("threshold2"));
			core.verbose = Boolean.parseBoolean(props.getProperty("verbose"));
		}
		catch( Exception xc ){
			xc.printStackTrace();
			System.exit(-1);
		}

		//
		// 1) Read the zone indices
		//

		HashSet<Integer> zoneids = PrimlocIO.readZoneIDs(zoneFileName);
		core.numZ=zoneids.size();

		if( core.verbose ){
			System.out.println("PLCM: Data:");
			System.out.println("PLCM:\t#zones = "+core.numZ);
		}

		int idx=0;
		TreeMap<Integer,Integer> zonemap = new TreeMap<Integer,Integer>();

		for( Integer id : zoneids )
			zonemap.put( id, idx++ );

		//
		// 2) read the ecore.cij cost file
		// This matrix is full
		//
		// We hack ecore.cij to avoid cii=0
		// cii is select as min_j core.cij
		//

		core.cij = PrimlocIO.readMatrix(costFileName, core.numZ, core.numZ);
		for( int i=0; i<core.numZ; i++ ){
			double mincij = Double.POSITIVE_INFINITY;
			for( int j=0; j<core.numZ; j++ ){
				double v=core.cij.get(i, j);
				if( (v < mincij) && (v>0.0) )
					mincij = v;
			}
			if( core.cij.get(i, i) == 0.0 )
				core.cij.set(i, i, mincij );
		}

		core.setupCostStatistics();
		if( core.verbose )
			System.out.println("PLCM:\tTravel costs: mean="+core.df.format(core.avgCost)+" std.dev.= "+core.df.format(core.stdCost));

		//
		// 3) read the homes and jobs files
		// These matrices can be sparse
		//

		core.P = PrimlocIO.readZoneAttribute( core.numZ, homesFileName, zonemap );
		core.J = PrimlocIO.readZoneAttribute( core.numZ, jobsFileName, zonemap );

		double maxpj=0.0;
		double sp=0.0;
		double sj=0.0;

		for( int i=0; i<core.numZ; i++ ){
			sp += core.P[i];
			sj += core.J[i];
			if( core.P[i] > maxpj )
				maxpj = core.P[i];
			if( core.J[i] > maxpj )
				maxpj = core.J[i];
		}

		if( Math.abs(sp-sj) > 1.0 ){
			System.err.println("PLCM: Error: #jobs("+sj+")!= #homes("+sp+")");
			System.exit(-1);
		}
		core.N=sp;

		if( core.verbose )
			System.out.println("PLCM:\tTrip table: #jobs = #homes= "+core.N);

		return zonemap;
	}


	Matrix gravityModelMatrix(){
		double betaExponent = 0.7;
		Matrix grav = new Matrix( core.numZ, core.numZ );
		double sum1=0.0;
		for( int i=0; i<core.numZ; i++ ){
			for( int j=0; j<core.numZ; j++){
				double v = core.P[i]*core.J[j] / Math.pow( core.cij.get(i, j), betaExponent );
				grav.set(i, j, v);
				sum1+=v;
			}
		}

		double v=0.0;
		for( int i=0; i<core.numZ; i++ )
			for( int j=0; j<core.numZ; j++){
				grav.set(i, j, grav.get(i, j)*core.N/sum1);
				v+=grav.get(i, j);
			}
		return grav;
	}
}
