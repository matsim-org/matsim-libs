package playground.gregor.flooding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

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

import com.vividsolutions.jts.geom.Coordinate;

public class CutFlooding {


	protected static final Logger log = Logger.getLogger(CutFlooding.class);

	private final static double MAX_X = 658541.;
	private final static double MAX_Y = 9902564.;
	private final static double MIN_X = 648520.;
	private final static double MIN_Y = 988294.;
	private static final double DISTANCE = 5.;
	private static final int TIME_RES_DOWNSCALE = 2;
	private static final int MAX_TIME = 120; 
	private NetcdfFile in;
	protected NetcdfFileWriteable out;

	protected Array aX;

	protected Array aY;

	protected Array aZ;

	protected Array aStage;

	protected Index2D idxStage;

	protected Index1D idx;


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

	protected void run()  {

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

	protected void createNetcdf(List<Integer> indexes) {
		Dimension number_of_points = this.out.addDimension("number_of_points", indexes.size());
		Dimension number_of_timesteps = this.out.addDimension("number_of_timesteps", MAX_TIME);
		Dimension [] count = {number_of_points}; 
		Dimension [] stages = {number_of_timesteps,number_of_points};
		
		
		int pos = 0;
		
		ArrayDouble.D1 aX = new ArrayDouble.D1(indexes.size());
		ArrayDouble.D1 aY = new ArrayDouble.D1(indexes.size());
		ArrayDouble.D1 aZ = new ArrayDouble.D1(indexes.size());
		
		ArrayDouble.D2 aStage = new ArrayDouble.D2(MAX_TIME,indexes.size());
		
		for (int i : indexes) {
			this.idx.set(i);
			this.idxStage.set(0,i);
			for (int j = 0 ; j < MAX_TIME * TIME_RES_DOWNSCALE; j += TIME_RES_DOWNSCALE) {
				this.idxStage.set0(j);
				double s = this.aStage.getFloat(this.idxStage);
				if (s != 0) {
					System.out.println("s:" + s);
				}
				aStage.set((j/TIME_RES_DOWNSCALE),pos, s);
			}
			double x = this.aX.getDouble(this.idx);
			double y = this.aY.getDouble(this.idx);
			double z = this.aZ.getDouble(this.idx);
			aX.setDouble(pos, x);
			aY.setDouble(pos, y);
			if (z != 0 ) {
				System.out.println("z:" + z);
			}
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


	protected List<Integer> getIndexesToInclude() {
		QuadTree<Coordinate> coords = new QuadTree<Coordinate>(MIN_X,MIN_Y,MAX_X,MAX_Y);

		log.info("found " + this.idx.getSize() + " coordinates and " + this.idxStage.getShape()[0] + " time steps");

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
			double z = this.aZ.getDouble(this.idx);

			if (x > MAX_X || x < MIN_X || y > MAX_Y || y < MIN_Y) {
				continue;
			}
			Collection<Coordinate> coll = coords.get(x, y, DISTANCE);
			if (coll.size() > 0) {
				continue;
			}
			Coordinate c = new Coordinate(x,y,z);
			coords.put(x, y,c);
			
			indexes.add(i);
		}

		log.info(indexes.size() + " coordinates to include");

		return indexes;
	}

	public static void main(String [] args) {
		String in = "../../inputs/flooding/SZ_r018M_m003_095_11_mw9.00_03h__P0_8.sww";
		String out = "../../inputs/flooding/flooding01.sww";

		new CutFlooding(in,out).run();



	}



}
