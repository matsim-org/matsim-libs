package playground.david.vis;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.snapshots.PositionInfo;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.Plan;
import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;

import playground.david.vis.OnTheFlyServer.QuadStorage;
import playground.david.vis.data.OTFNetWriterFactory;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.handler.OTFAgentsListHandler;
import playground.david.vis.interfaces.OTFNetHandler;
import playground.david.vis.interfaces.OTFServerRemote;

public class OTFTVehServer implements OTFServerRemote{
	private final   String netFileName = "";
	private  String vehFileName = "";
	private  String outFileName = "";
	private static final int BUFFERSIZE = 100000000;
	BufferedReader reader = null;
	private double nextTime = -1;


	private final OTFAgentsListHandler.Writer writer = new OTFAgentsListHandler.Writer();
	private ByteArrayOutputStream out;
	private QueueNetworkLayer net;
	public OTFServerQuad quad;
	
	public OTFTVehServer(String netFileName, String vehFileName) {
		this.vehFileName = vehFileName;
		
		Gbl.createConfig(null);
		Gbl.startMeasurement();
		World world = Gbl.createWorld();

		QueueNetworkLayer net = new QueueNetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);

		out = new ByteArrayOutputStream(20000000);
		quad = new OTFServerQuad(net);
		quad.addAdditionalElement(writer);
		open();
	}

	ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);
	private int cntPositions=0;
	private double lastTime=-1;
	private int cntTimesteps=0;
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
		
		if ( !lineFound && line != null) {
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
							Double.parseDouble(elevation), Double.parseDouble(azimuth), Double.parseDouble(speed), PositionInfo.VehicleState.Driving);
				}
			}
			return true;
		} else return false;
	}
	
	private boolean finishedReading = false;
	
	public void step() {
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
		
	}

	public void open() {
		
		try {
			reader = IOUtils.getBufferedReader(this.vehFileName);
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
		return buf.array();
	}

	public byte[] getStateBuffer() throws RemoteException {
		throw new RemoteException("getNet not implemented for OTFTVehServer");
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

//		String netFileName = "../studies/schweiz/2network/ch.xml"; 
//		String vehFileName = "../runs/run168/run168.it210.T.veh"; 
		String netFileName = "../../tmp/studies/ivtch/network.xml"; 
		String vehFileName = "../../tmp/studies/ivtch/T.veh"; 
		
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

}
