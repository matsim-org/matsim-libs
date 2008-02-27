package playground.david.vis.executables;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.utils.StringUtils;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;
import org.matsim.world.World;

import playground.david.vis.OTFQuadFileHandler;
import playground.david.vis.OTFQuadFileHandler.Writer;
import playground.david.vis.handler.OTFAgentsListHandler;
import playground.david.vis.handler.OTFAgentsListHandler.ExtendedPositionInfo;

public class OTFTVeh2MVI extends OTFQuadFileHandler.Writer{
	private final   String netFileName = "";
	private  String vehFileName = "";
	private  String outFileName = "";
	private static final int BUFFERSIZE = 100000000;


	private final OTFAgentsListHandler.Writer writer = new OTFAgentsListHandler.Writer();
	
	public OTFTVeh2MVI(QueueNetworkLayer net, String vehFileName, String outFileName, double startTime, double intervall_s) {
		super(intervall_s, net, outFileName);
		this.vehFileName = vehFileName;
		this.outFileName = outFileName;
		this.intervall_s = intervall_s;
	}

	@Override
	protected void onAdditionalQuadData() {
		quad.addAdditionalElement(writer);
	}
	
//	public static double myParseDouble(String rep) {
//		double result = 0;
//		int exp = 1;
//		double factor = 1;
//		double before= 0;
//		double after = 0;
//		String [] parts = StringUtils.explode(rep, 'E');
//		if(parts.length == 2) {
//			exp = Integer.parseInt(parts[1]);
//			while (exp-- >0) factor *=10.;
//		} else {
//			
//		}
//		parts = StringUtils.explode(parts[0], '.');
//		before = Long.parseLong(parts[0]);
//		if(parts.length == 2) {
//			after = Long.parseLong(parts[1]);
//			double divider = 1;
//			int stellen = parts[1].length();
//			while (stellen-- >0) divider *=10.;
//			after /= divider;
//		} else {
//			
//		}
//		result = (before + after)* factor;
//		return result;
//	}

	ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);
	private int cntPositions=0;
	private double lastTime=-1;
	private int cntTimesteps=0;
	
	private void convert() {

		open();
		// read and convert data from veh-file
		
		BufferedReader reader = null;
		try {
			reader = IOUtils.getBufferedReader(this.vehFileName);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		try {
			reader.readLine(); // header, we do not use it
			String line = null;
			while ( (line = reader.readLine()) != null) {
	
				
				String[] result = StringUtils.explode(line, '\t', 16);//line.split("\t");
				if (result.length == 16) {
					double easting = Double.parseDouble(result[11]);
					double northing = Double.parseDouble(result[12]);
					
					if (easting >= quad.getMinEasting() && easting <= quad.getMaxEasting() && northing >= quad.getMinNorthing() && northing <= quad.getMaxNorthing()) {
						String agent = result[0];
						String time = result[1];
//					String dist = result[5];
						String speed = result[6];
						String elevation = result[13];
						String azimuth = result[14];
						String type = result[7];
						ExtendedPositionInfo position = new ExtendedPositionInfo(new Id(agent), easting, northing,
								Double.parseDouble(elevation), Double.parseDouble(azimuth), Double.parseDouble(speed), PositionInfo.VehicleState.Driving, Integer.parseInt(result[7]), Integer.parseInt(result[15]));
						addVehicle(Double.parseDouble(time), position);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		finish();
	}

	private void addVehicle(double time, ExtendedPositionInfo position) {
		this.cntPositions++;

		// Init lastTime with first occurence of time!
		if (lastTime == -1) lastTime = time;
		
		if (time != this.lastTime) {
			this.cntTimesteps++;

			if (time % 600 == 0 ) {
				System.out.println("Parsing T = " + time + " secs");
				Gbl.printElapsedTime();
				Gbl.startMeasurement();
			}
			// the time changes
				// this is a dumpable timestep
				try {
					dump((int)this.lastTime);
					writer.positions.clear();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			this.lastTime = time;
		}			
// I do not realyy know which second will be written, as it might be any second AFTER nextTime, when NOTHING has happened on "nextTime", as the if-clause will be executed only then
// still I can collect all vehicles, as to every time change it will get erased...
//		if (time == nextTime) {
			writer.positions.add(position);
//		}
	}
	
	

	private void finish() {
	try {
		close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}
	
	
	public static void main(String[] args) {

//		String netFileName = "../studies/schweiz/2network/ch.xml"; 
//		String vehFileName = "../runs/run168/run168.it210.T.veh"; 
//		String netFileName = "../../tmp/studies/ivtch/network.xml"; 
//		String vehFileName = "../../tmp/studies/ivtch/T.veh"; 
//		String outFileName = "output/testSWI2.mvi.gz";

		String netFileName = "../../tmp/studies/padang/padang_net.xml"; 
//		String vehFileName = "../../tmp/studies/padang/run301.it100.colorized.T.veh.gz"; 
		String vehFileName = "../runs/run301/output/100.T.veh.gz"; 
		String outFileName = "../OnTheFlyVis/test/testPadabang1.3.mvi";
		int intervall_s = 60;
		
		Gbl.createConfig(null);
		Gbl.startMeasurement();
		World world = Gbl.createWorld();

		QueueNetworkLayer net = new QueueNetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);

		
		OTFTVeh2MVI test  = new OTFTVeh2MVI(net, vehFileName, outFileName, 0, intervall_s);
		test.convert();
	}


}
