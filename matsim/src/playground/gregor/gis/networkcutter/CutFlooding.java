package playground.gregor.gis.networkcutter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.Index1D;
import ucar.ma2.Index2D;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;
import ucar.nc2.iosp.IOServiceProvider;

public class CutFlooding {


	private static final Logger log = Logger.getLogger(CutFlooding.class);

	private final static double MAX_X = 652088.;
	private final static double MAX_Y = 9894785.;
	private final static double MIN_X = 650473.;
	private final static double MIN_Y = 9892816.;
	private NetcdfFile in;
	private NetcdfFileWriteable out;

	private Array aX;

	private Array aY;

	private Array aZ;

	private Array aStage;

	private Index2D idxStage;

	private Index1D idx;


	public CutFlooding(String netcdf, String out) {
		try {
			this.in = NetcdfFile.open(netcdf);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			this.out = NetcdfFileWriteable.createNew(out, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void run()  {

		try {
			init();
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		} catch (InvalidRangeException e1) {
			throw new RuntimeException(e1);
		}

		List<Integer> indexes = getIndexesToInclude();
		createNetcdf(indexes);

	}


	private void init() throws IOException, InvalidRangeException {
		log.info("initializing netcdf");

		IOServiceProvider ios =  this.in.getIosp();
		Section sX = new Section();
		Variable varX = this.in.findVariable("x");
		sX.appendRange(varX.getRanges().get(0));
		this.aX = ios.readData(varX, sX);

		Section sY = new Section();
		Variable varY = this.in.findVariable("y");
		sY.appendRange(varY.getRanges().get(0));
		this.aY = ios.readData(varY, sY);

		Section sZ = new Section();
		Variable varZ = this.in.findVariable("elevation");
		sZ.appendRange(varZ.getRanges().get(0));
		this.aZ = ios.readData(varZ, sY);

		Section sStage = new Section();
		Variable varStage = this.in.findVariable("stage");
		sStage.appendRange(varStage.getRanges().get(0));
		sStage.appendRange(varStage.getRanges().get(1));
		this.aStage = ios.readData(varStage, sStage);

		this.idxStage = new Index2D(this.aStage.getShape());
		this.idx = new Index1D(this.aX.getShape());

		log.info("finished init.");
	}

	private void createNetcdf(List<Integer> indexes) {
		Dimension number_of_points = this.out.addDimension("number_of_points", indexes.size());
		Dimension number_of_timesteps = this.out.addDimension("number_of_timesteps", this.idxStage.getShape()[0]);
		Dimension [] count = {number_of_points}; 
		Dimension [] stages = {number_of_timesteps,number_of_points};
		
		
		int pos = 0;
		
		ArrayDouble.D1 aX = new ArrayDouble.D1(indexes.size());
		ArrayDouble.D1 aY = new ArrayDouble.D1(indexes.size());
		ArrayDouble.D1 aZ = new ArrayDouble.D1(indexes.size());
		
		ArrayDouble.D2 aStage = new ArrayDouble.D2(this.idxStage.getShape()[0],indexes.size());
		
		for (int i : indexes) {
			this.idx.set(i);
			this.idxStage.set(0,i);
			for (int j = 0 ; j < this.idxStage.getShape()[0]; j++) {
				this.idxStage.set0(j);
				double s = this.aStage.getFloat(this.idxStage);
				aStage.set(j,pos, s);
			}
			
			double x = this.aX.getDouble(this.idx);
			double y = this.aY.getDouble(this.idx);
			double z = this.aZ.getDouble(this.idx);
			aX.setDouble(pos, x);
			aY.setDouble(pos, y);
			aZ.setDouble(pos++, z);


		}
		
		this.out.addVariable("x", DataType.DOUBLE,count);
		this.out.addVariable("y", DataType.DOUBLE,count);
		this.out.addVariable("elevation", DataType.DOUBLE,count);
		this.out.addVariable("stage", DataType.DOUBLE,stages);
		

		// create the file
		try {
			this.out.create();
			this.out.write("x",new int[] {0},aX);
			this.out.write("y",new int[] {0},aY);
			this.out.write("elevation",new int[] {0},aZ);
			this.out.write("stage",new int[] {0,0},aStage);
			this.out.close();
		} catch (IOException e) {
			System.err.println("ERROR creating file "+this.out.getLocation()+"\n"+e);
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		}

	}


	private List<Integer> getIndexesToInclude() {


		log.info("found " + this.idx.getSize() + " coordinates");

		int next = 0;
		List<Integer> indexes = new ArrayList<Integer>();

		for (int i = 0; i < this.idx.getSize(); i++) {
			if (i  >= next){
				log.info(i + " coordinates to processed.");
				next = i*2;
			}
			this.idx.set(i);
			this.idxStage.set(0,i);
			double x = this.aX.getDouble(this.idx);
			double y = this.aY.getDouble(this.idx);

			if (x > MAX_X || x < MIN_X || y > MAX_Y || y < MIN_Y) {
				continue;
			} 

			indexes.add(i);
		}

		log.info(indexes.size() + " coordinates to include");

		return indexes;
	}

	public static void main(String [] args) {
		String in = "../../inputs/padang/Model_result_Houses_kst20.sww";
		String out = "tmp2/flooding.sww";

		new CutFlooding(in,out).run();



	}



}
