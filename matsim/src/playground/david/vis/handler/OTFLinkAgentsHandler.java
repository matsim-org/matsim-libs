package playground.david.vis.handler;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.mobsim.QueueLink;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;
import org.matsim.utils.vis.snapshots.writers.PositionInfo.VehicleState;

import playground.david.vis.data.OTFDataSimpleAgent;
import playground.david.vis.data.OTFDataWriter;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.data.OTFWriterFactory;
import playground.david.vis.data.OTFData.Receiver;
import playground.david.vis.gui.PoolFactory;
import playground.david.vis.interfaces.OTFDataReader;

public class OTFLinkAgentsHandler extends OTFDefaultLinkHandler {
	
	static boolean prevV1_1 = OTFDataReader.setPreviousVersion(OTFLinkAgentsHandler.class.getCanonicalName() + "V1.1", ReaderV1_1.class);
	
	private final Logger log = Logger.getLogger(OTFLinkAgentsHandler.class);

	private Class agentReceiverClass = null;
	static PoolFactory<OTFDataSimpleAgent.Receiver> factoryAgent;
	
	protected List<OTFDataSimpleAgent.Receiver> agents = new LinkedList<OTFDataSimpleAgent.Receiver>();
	
	static public class Writer extends  OTFDefaultLinkHandler.Writer implements Serializable, OTFWriterFactory<QueueLink>{

		/**
		 * 
		 */
		private static final long serialVersionUID = -7916541567386865404L;
		protected static transient Collection<PositionInfo> positions = new ArrayList<PositionInfo>();

		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
			super.writeConstData(out);
		}


		public void writeAgent(PositionInfo pos, ByteBuffer out) throws IOException {
			String id = pos.getAgentId().toString();
			out.putInt(id.length());
			for (int i=0; i<id.length(); i++) out.putChar(id.charAt(i));
			out.asCharBuffer().put(pos.getAgentId().toString());
			out.putFloat((float)(pos.getEasting() - OTFServerQuad.offsetEast));
			out.putFloat((float)(pos.getNorthing()- OTFServerQuad.offsetNorth));
			out.putInt(pos.getVehicleState()== VehicleState.Parking ? 1:0);
			out.putFloat((float)pos.getSpeed());
		}
		
		protected void writeAllAgents(ByteBuffer out) throws IOException {
			// Write additional agent data
	        /*
	         * (4) write agents
	         */
	        positions.clear();
			src.getVehiclePositions(positions);
			out.putInt(positions.size());

			for (PositionInfo pos : positions) {
				writeAgent(pos, out);
			}
		}
		
		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			super.writeDynData(out);
			
			writeAllAgents(out);
		}

		@Override
		public OTFDataWriter<QueueLink> getWriter() {
			return new Writer();
		}

	}
	
	static char[] idBuffer = new char[100];
	
	public void readAgent(ByteBuffer in) throws IOException {
		int length = in.getInt();
		idBuffer = new char[length];
		for(int i=0;i<length;i++) idBuffer[i] = in.getChar();
		float x = in.getFloat();
		float y = in.getFloat();
		int state = in.getInt();
		// Convert to km/h 
		float color = in.getFloat()*3.6f;
		
		// No agent receiver given, then we are finished
		if (agentReceiverClass == null) return;

			OTFDataSimpleAgent.Receiver drawer = null;
			try {
				drawer = (playground.david.vis.data.OTFDataSimpleAgent.Receiver) agentReceiverClass.newInstance();
				drawer.setAgent(idBuffer, x, y, state, color);
				agents.add(drawer);
			} catch (InstantiationException e) {
				log.warn("Agent drawer could not be instanciated");
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //factoryAgent.getOne();

 	}
	
	@Override
	public void readDynData(ByteBuffer in) throws IOException {
		super.readDynData(in);
		// read additional agent data
		agents.clear();
		
		int count = in.getInt();
		for(int i= 0; i< count; i++) readAgent(in);
	}


	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		super.readConstData(in);
	}



	@Override
	public void connect(Receiver receiver) {
		super.connect(receiver);
		//connect agent receivers
		if (receiver  instanceof OTFDataSimpleAgent.Receiver) {
			this.agentReceiverClass = receiver.getClass();
			this.factoryAgent = PoolFactory.get(this.agentReceiverClass);
		}

	}

	@Override
	public void invalidate() {
		super.invalidate();
		// invalidate agent receivers
		for(OTFDataSimpleAgent.Receiver agent : agents) agent.invalidate();
	}

	
	public static final class ReaderV1_1 extends OTFLinkAgentsHandler {
		@Override
		public void readDynData(ByteBuffer in) throws IOException {
			agents.clear();
			
			int count = in.getInt();
			for(int i= 0; i< count; i++) readAgent(in);
		}
	}
}
