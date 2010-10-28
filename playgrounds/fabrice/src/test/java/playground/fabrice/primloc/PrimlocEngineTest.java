package playground.fabrice.primloc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.testcases.MatsimTestCase;

import Jama.Matrix;

// FIXME this test-case has no Assert-statement, so it will always succeed!

public class PrimlocEngineTest extends MatsimTestCase {

	private final static String testPropertyFile = "test/input/org/matsim/demandmodeling/primloc/PrimaryLocationChoice.xml";

	private final static Logger log = Logger.getLogger(PrimlocEngineTest.class);

	private PrimlocCore core = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.core = new PrimlocCore();
	}

	@Override
	protected void tearDown() throws Exception {
		this.core = null;
		super.tearDown();
	}

	public void testPLCM() throws InvalidPropertiesFormatException, FileNotFoundException, IOException {

		System.out.println("Starting Primary Location Choice Model (PLCM)");
		System.out.println("PLCM: Looking for PrimaryLocationChoice.xml config file of the Zurich sample dataset in " + testPropertyFile);


		if( new File( testPropertyFile ).exists() ){
			System.out.println("PLCM: ... config found");
			String outputfile = getOutputDirectory() + "/rent_patterns_testPLCM:txt";
			standAloneRun( testPropertyFile, outputfile );
		}
		else{
			System.err.println("PLCM: Could not find a suitable data set");
		}
	}

	void standAloneRun( final String propFile, final String outputFile ) throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		TreeMap<Integer,Integer> zonemap = initProperties( propFile ); // model_ID to internal_ID

		this.core.setCalibrationError( new PrimlocCalibrationErrorMatrix( gravityModelMatrix() ) );
		this.core.runCalibrationProcess();

		System.out.println("PLCM: writing rent patterns to:" + new File(outputFile).getAbsolutePath() );

		PrimlocIO.saveResults( this.core.X, zonemap, outputFile);
		System.out.println("PLCM: done");
	}

	private TreeMap<Integer,Integer> initProperties( final String propertyFile ) throws InvalidPropertiesFormatException, FileNotFoundException, IOException {

		String zoneFileName=null;
		String costFileName=null;
		String homesFileName=null;
		String jobsFileName=null;

		Properties props = new Properties();

		FileInputStream stream = null;
		try {
			stream = new FileInputStream(propertyFile);
			props.loadFromXML(stream);
		}
		finally {
			try {
				if (stream != null) { stream.close(); }
			} catch (IOException e) { log.warn("Could not close stream.", e); }
		}
		zoneFileName = props.getProperty("zoneFileName");
		costFileName = props.getProperty("costFileName");
		homesFileName = props.getProperty("homesFileName");
		jobsFileName = props.getProperty("jobsFileName");

		this.core.maxiter = Integer.parseInt(props.getProperty("maxiter"));
		this.core.mu = Double.parseDouble(props.getProperty("mu"));
		this.core.theta = Double.parseDouble( props.getProperty("theta"));
		this.core.threshold1 = Double.parseDouble(props.getProperty("threshold1"));
		this.core.threshold2 = Double.parseDouble(props.getProperty("threshold2"));
		this.core.verbose = Boolean.parseBoolean(props.getProperty("verbose"));

		//
		// 1) Read the zone indices
		//

		HashSet<Integer> zoneids = PrimlocIO.readZoneIDs(zoneFileName);
		this.core.numZ=zoneids.size();

		if( this.core.verbose ){
			System.out.println("PLCM: Data:");
			System.out.println("PLCM:\t#zones = "+this.core.numZ);
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

		this.core.cij = PrimlocIO.readMatrix(costFileName, this.core.numZ, this.core.numZ);
		for( int i=0; i<this.core.numZ; i++ ){
			double mincij = Double.POSITIVE_INFINITY;
			for( int j=0; j<this.core.numZ; j++ ){
				double v=this.core.cij.get(i, j);
				if( (v < mincij) && (v>0.0) )
					mincij = v;
			}
			if( this.core.cij.get(i, i) == 0.0 )
				this.core.cij.set(i, i, mincij );
		}

		this.core.setupCostStatistics();
		if( this.core.verbose )
			System.out.println("PLCM:\tTravel costs: mean="+this.core.df.format(this.core.avgCost)+" std.dev.= "+this.core.df.format(this.core.stdCost));

		//
		// 3) read the homes and jobs files
		// These matrices can be sparse
		//

		this.core.P = PrimlocIO.readZoneAttribute( this.core.numZ, homesFileName, zonemap );
		this.core.J = PrimlocIO.readZoneAttribute( this.core.numZ, jobsFileName, zonemap );

		double maxpj=0.0;
		double sp=0.0;
		double sj=0.0;

		for( int i=0; i<this.core.numZ; i++ ){
			sp += this.core.P[i];
			sj += this.core.J[i];
			if( this.core.P[i] > maxpj )
				maxpj = this.core.P[i];
			if( this.core.J[i] > maxpj )
				maxpj = this.core.J[i];
		}

		if( Math.abs(sp-sj) > 1.0 ){
			throw new RuntimeException("PLCM: Error: #jobs("+sj+")!= #homes("+sp+")");
		}
		this.core.N=sp;

		if( this.core.verbose )
			System.out.println("PLCM:\tTrip table: #jobs = #homes= "+this.core.N);

		return zonemap;
	}


	Matrix gravityModelMatrix(){
		double betaExponent = 0.7;
		Matrix grav = new Matrix( this.core.numZ, this.core.numZ );
		double sum1=0.0;
		for( int i=0; i<this.core.numZ; i++ ){
			for( int j=0; j<this.core.numZ; j++){
				double v = this.core.P[i]*this.core.J[j] / Math.pow( this.core.cij.get(i, j), betaExponent );
				grav.set(i, j, v);
				sum1+=v;
			}
		}

		double v=0.0;
		for( int i=0; i<this.core.numZ; i++ )
			for( int j=0; j<this.core.numZ; j++){
				grav.set(i, j, grav.get(i, j)*this.core.N/sum1);
				v+=grav.get(i, j);
			}
		return grav;
	}
}
