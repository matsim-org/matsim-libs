package playground.david.vis;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.Plan;
import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;
import org.matsim.world.World;

import playground.david.vis.data.OTFDefaultNetWriterFactoryImpl;
import playground.david.vis.data.OTFNetWriterFactory;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.handler.OTFAgentsListHandler;
import playground.david.vis.interfaces.OTFNetHandler;
import playground.david.vis.interfaces.OTFServerRemote;

public class OTFTVehServer implements OTFServerRemote{
	private final   String netFileName = "";
	private  String vehFileName = "";
	private final  String outFileName = "";
	private static final int BUFFERSIZE = 100000000;
	BufferedReader reader = null;
	private double nextTime = -1;
	private boolean bufferedReading = false;
	TreeMap<Integer, byte[]> timesteps = new TreeMap<Integer, byte[]>();
	private byte[] actBuffer = null;



	private final OTFAgentsListHandler.Writer writer = new OTFAgentsListHandler.Writer();
	private final ByteArrayOutputStream out;
	private QueueNetworkLayer net;
	public OTFServerQuad quad;
	
	public OTFTVehServer(String netFileName, String vehFileName) {
		this.vehFileName = vehFileName;
		
		if (Gbl.getConfig() == null) Gbl.createConfig(null);

		Gbl.startMeasurement();
		World world = Gbl.createWorld();

		QueueNetworkLayer net = new QueueNetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);

		out = new ByteArrayOutputStream(20000000);
		quad = new OTFServerQuad(net);
		quad.fillQuadTree(new OTFDefaultNetWriterFactoryImpl());
		quad.addAdditionalElement(writer);
		open();
		if (bufferedReading) {
			bufferedReading = false; // now temporarily do the real reading
			double time = nextTime;
			step();
			while (time != nextTime) {
				//DS TODO unsafe, entry could get bigger than int 
				byte [] buffer = new byte[buf.position()+1];
				System.arraycopy(actBuffer, 0, buffer, 0, buffer.length);
				timesteps.put((int)nextTime, buffer); 
				System.out.println("read timestep: " + nextTime);
				time = nextTime;
				step();
			}
			finish();
			nextTime = -1.;
			bufferedReading = true;
		}
	}

	ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);
	private final int cntPositions=0;
	private final double lastTime=-1;
	private final int cntTimesteps=0;
	private PositionInfo readVehicle = null;
	private double time;
	
	public boolean readOneLine(){
		String line = null;
		boolean lineFound = false;
		
		try {
			line = reader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		while ( !lineFound && line != null) {
			String[] result = line.split("\t");
			if (result.length == 16) {
				double easting = Double.parseDouble(result[11]);
				double northing = Double.parseDouble(result[12]);
				if (easting >= quad.getMinEasting() && easting <= quad.getMaxEasting() && northing >= quad.getMinNorthing() && northing <= quad.getMaxNorthing()) {
					String agent = result[0];
					String time = result[1];
					String speed = result[6];
					String elevation = result[13];
					String azimuth = result[14];

					lineFound = true;
					this.time = Double.parseDouble(time);
					this.readVehicle = new PositionInfo(new Id(agent), easting, northing,
							Double.parseDouble(elevation), Double.parseDouble(azimuth), Double.parseDouble(speed), PositionInfo.VehicleState.Driving, result[15]);
					return true;
				}
			}
			try {
				line = reader.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} 
		return false;
	}
	
	private boolean finishedReading = false;
	
	public void step() {
		if (bufferedReading) {
			actBuffer = nextStateBuffer();
			return;
		}
		if ( finishedReading) return;
		
		double actTime = time;
		
		if (readVehicle == null){
			readOneLine();
			writer.positions.add(readVehicle);
			actTime = time;
		} else {
			writer.positions.clear();
			writer.positions.add(readVehicle);
		}

		// collect all vehicles
		while (readOneLine() && time == actTime) writer.positions.add(readVehicle);

		// check if file is read to end
		if(time == actTime)finishedReading = true;

		// now write this into stream
		nextTime = actTime;
		buf.position(0);
		quad.writeDynData(null, buf);
		actBuffer = buf.array();
		
	}

	public void open() {
		
		try {
			reader = IOUtils.getBufferedReader(this.vehFileName);
			reader.readLine(); // Read the commentary line
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	private void finish() {
	try {
		reader.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
	}
	
	
	public Plan getAgentPlan(String id) throws RemoteException {
		return null;
	}

	public int getLocalTime() throws RemoteException {
		return (int)nextTime;
	}

	public OTFVisNet getNet(OTFNetHandler handler) throws RemoteException {
		throw new RemoteException("getNet not implemented for OTFTVehServer");
	}


	public OTFServerQuad getQuad(String id, OTFNetWriterFactory writers)
			throws RemoteException {
		return quad;
	}

	public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
		buf.position(0);
		quad.writeConstData(buf);
		byte [] result;
		synchronized (buf) {
			result = buf.array();
		}
		return result;
	}	

	public byte[] getQuadDynStateBuffer(String id, Rect bounds)
			throws RemoteException {
		if (nextTime == -1) {
			step();
		}
		return buf.array();
	}

	public byte[] nextStateBuffer() {
		if (nextTime == -1. && bufferedReading)nextTime = timesteps.firstKey();
		
		byte [] buffer = timesteps.get((int)nextTime);
		int time = 0;
		Iterator<Integer> it =  timesteps.keySet().iterator();
		while(it.hasNext() && time <= nextTime) time = it.next();
		if (time == nextTime) {
			time = timesteps.firstKey();
		}
		nextTime = time;
		return buffer;
	}
	public byte[] getStateBuffer() throws RemoteException {
		throw new RemoteException("getStateBuffer not implemented for OTFTVehServer");
	}

	public boolean isLive() throws RemoteException {
		return false;
	}

	public void pause() throws RemoteException {
	}

	public void play() throws RemoteException {
	}

	public void setStatus(int status) throws RemoteException {
	}


	public static void main(String[] args) {

		String netFileName = "../studies/schweiz/2network/ch.xml"; 
		String vehFileName = "../runs/run168/run168.it210.T.veh"; 
//		String netFileName = "../../tmp/studies/ivtch/network.xml"; 
//		String vehFileName = "../../tmp/studies/ivtch/T.veh"; 
		
		OTFTVehServer test  = new OTFTVehServer(netFileName, vehFileName);
		try {
			double time = test.getLocalTime();
			test.step();
			while (time != test.getLocalTime()) {
				time = test.getLocalTime();
				test.step();
			}
			test.finish();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

}
