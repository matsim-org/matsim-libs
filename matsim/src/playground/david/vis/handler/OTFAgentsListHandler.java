package playground.david.vis.handler;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;

import playground.david.vis.data.OTFDataSimpleAgent;
import playground.david.vis.data.OTFDataWriter;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.data.SceneGraph;
import playground.david.vis.data.OTFData.Receiver;
import playground.david.vis.interfaces.OTFDataReader;

public class OTFAgentsListHandler extends OTFDataReader {
	static boolean prevV1_1 = OTFDataReader.setPreviousVersion(OTFAgentsListHandler.class.getCanonicalName() + "V1.1", ReaderV1_2.class);
	static boolean prevV1_2 = OTFDataReader.setPreviousVersion(OTFAgentsListHandler.class.getCanonicalName() + "V1.2", ReaderV1_2.class);
	
	protected Class agentReceiverClass = null;
	
	protected List<OTFDataSimpleAgent.Receiver> agents = new LinkedList<OTFDataSimpleAgent.Receiver>();
	public static class ExtendedPositionInfo extends PositionInfo {

		int type = 0;
		int user = 0;
		
		public ExtendedPositionInfo(IdI driverId, double easting, double northing, double elevation, double azimuth, double speed, VehicleState vehicleState, int type, int userdata) {
			super(driverId, easting, northing, elevation, azimuth, speed, vehicleState, "");
			this.type = type;
			this.user = userdata;
		}
		
	}
	
	@SuppressWarnings("unchecked")
	static public class Writer extends  OTFDataWriter implements Serializable {


		/**
		 * 
		 */
		private static final long serialVersionUID = -6368752578878835954L;
		
		public transient Collection<ExtendedPositionInfo> positions = new ArrayList<ExtendedPositionInfo>();

		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
		}


		public void writeAgent(ExtendedPositionInfo pos, ByteBuffer out) throws IOException {
			String id = pos.getAgentId().toString();
			out.putInt(id.length());
			for (int i=0; i<id.length(); i++) out.putChar(id.charAt(i));
			out.asCharBuffer().put(pos.getAgentId().toString());
			out.putFloat((float)(pos.getEasting() - OTFServerQuad.offsetEast));
			out.putFloat((float)(pos.getNorthing()- OTFServerQuad.offsetNorth));
			out.putInt(pos.type);
			out.putInt(pos.user);
			out.putFloat((float)pos.getSpeed());
		}
		
		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			// Write additional agent data
	        /*
	         * (4) write agents
	         */
			out.putInt(positions.size());

			for (ExtendedPositionInfo pos : positions) {
				writeAgent(pos, out);
			}
	        positions.clear();
		}

	}
	
	static char[] idBuffer = new char[100];
	
	public void readAgent(ByteBuffer in, SceneGraph graph) throws IOException {
		int length = in.getInt();
		idBuffer = new char[length];
		for(int i=0;i<length;i++) idBuffer[i] = in.getChar();
		float x = in.getFloat();
		float y = in.getFloat();
		int type = in.getInt();
		int user = in.getInt();
		// Convert to km/h 
		float speed = in.getFloat()*3.6f;

			OTFDataSimpleAgent.Receiver drawer = null;
			try {
				drawer = (playground.david.vis.data.OTFDataSimpleAgent.Receiver) graph.newInstance(agentReceiverClass);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //factoryAgent.getOne();
			drawer.setAgent(idBuffer, x, y, type, user, speed);
			agents.add(drawer);

 	}
	

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		// read additional agent data
		agents.clear();
		
		int count = in.getInt();
		for(int i= 0; i< count; i++) readAgent(in, graph);
	}



	@Override
	public void readConstData(ByteBuffer in) throws IOException {
	}




	@Override
	public void connect(Receiver receiver) {
		//connect agent receivers
		if (receiver  instanceof OTFDataSimpleAgent.Receiver) {
			this.agentReceiverClass = receiver.getClass();
		}

	}

	
	@Override
	public void invalidate(SceneGraph graph) {
		// invalidate agent receivers
		for(OTFDataSimpleAgent.Receiver agent : agents) agent.invalidate(graph);
	}


	/***
	 * PREVIOUS VERSION of the reader
	 * @author dstrippgen
	 *
	 */
	public static final class ReaderV1_2 extends OTFAgentsListHandler {
		
		@Override
		public void readAgent(ByteBuffer in, SceneGraph graph) throws IOException {
			int length = in.getInt();
			idBuffer = new char[length];
			for(int i=0;i<length;i++) idBuffer[i] = in.getChar();
			float x = in.getFloat();
			float y = in.getFloat();
			int state = in.getInt();
			// Convert to km/h 
			float color = in.getFloat()*3.6f;

				OTFDataSimpleAgent.Receiver drawer = null;
				try {
					drawer = (playground.david.vis.data.OTFDataSimpleAgent.Receiver) graph.newInstance(agentReceiverClass);
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} //factoryAgent.getOne();
				// at this version, only userdata was defined... aka state
				drawer.setAgent(idBuffer, x, y, 0, state, color);
				agents.add(drawer);

	 	}
		

	}
}
