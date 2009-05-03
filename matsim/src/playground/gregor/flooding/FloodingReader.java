package playground.gregor.flooding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.Index1D;
import ucar.ma2.Index2D;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.IOServiceProvider;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class FloodingReader {
	
	private static final Logger log = Logger.getLogger(FloodingReader.class);
	
	private static final double FLOODING_TRESHOLD = 0.05;
	
	private NetcdfFile ncfile;

	private Collection<FloodingInfo> fis;

	private Envelope envelope;
	
	private boolean initialized = false;
	
	public FloodingReader(String netcdf) {
		try {
			this.ncfile = NetcdfFile.open(netcdf);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	private void readFile() throws IOException, InvalidRangeException {
		this.fis = new ArrayList<FloodingInfo>();
		this.envelope = new Envelope();
		
		
		log.info("initializing netcdf");
		
		IOServiceProvider ios =  this.ncfile.getIosp();
		Section sX = new Section();
		Variable varX = this.ncfile.findVariable("x");
		sX.appendRange(varX.getRanges().get(0));
		Array aX = ios.readData(varX, sX);
		
		Section sY = new Section();
		Variable varY = this.ncfile.findVariable("y");
		sY.appendRange(varY.getRanges().get(0));
		Array aY = ios.readData(varY, sY);

		Section sZ = new Section();
		Variable varZ = this.ncfile.findVariable("elevation");
		sZ.appendRange(varZ.getRanges().get(0));
		Array aZ = ios.readData(varZ, sY);
		
		Section sStage = new Section();
		Variable varStage = this.ncfile.findVariable("stage");
		sStage.appendRange(varStage.getRanges().get(0));
		sStage.appendRange(varStage.getRanges().get(1));
		Array aStage = ios.readData(varStage, sStage);
		
		Index idxStage = new Index2D(aStage.getShape());
		Index idx = new Index1D(aX.getShape());
		
		log.info("finished init.");
		
		log.info("found " + idx.getSize() + " coordinates");
		
		int next = 0;
		for (int i = 0; i < idx.getSize(); i++) {
			if (i  >= next){
				log.info(i + " coordinates processed.");
				next = i*2;
			}
			idx.set(i);
			idxStage.set(0,i);
			double x = aX.getDouble(idx);
			double y = aY.getDouble(idx);
			double z = aZ.getDouble(idx);
			Coordinate c = new Coordinate(x,y,z);
			this.envelope.expandToInclude(c);
			
			//offshore coord
			if (z < 0) {
				continue;
			}
			FloodingInfo flooding = processCoord(idxStage,aStage,c);
			if (flooding != null) {
				this.fis.add(flooding);
			}
		}
		this.initialized = true;
	}
	
	public Collection<FloodingInfo> getFloodingInfos() {
		if(!this.initialized) {
			try {
				readFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (InvalidRangeException e) {
				throw new RuntimeException(e);
			}
		}
		return this.fis;
	}
	
	public Envelope getEnvelope() {
		if(!this.initialized) {
			try {
				readFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (InvalidRangeException e) {
				throw new RuntimeException(e);
			}
		}
		return this.envelope;
	}
	
	private FloodingInfo processCoord(Index idxStage, Array stage, Coordinate c) {
		FloodingInfo flooding = null;
		List<Double> stages = new ArrayList<Double>();
		
		double time = -1;
		for (int i = 0 ; i < idxStage.getShape()[0]; i++) {
			idxStage.set0(i);
			double tmp = stage.getFloat(idxStage);
//			stages.add((tmp - c.z));
			if ((tmp - c.z) > FLOODING_TRESHOLD) {
				if (time == -1) {
					time = i;
				}
				stages.add(tmp - c.z);	
			} else {
				stages.add(0.);
			}
		}
		if (time != -1) {
			flooding = new FloodingInfo(c,stages,time);
		}
		return flooding;
	}


	
	

}
