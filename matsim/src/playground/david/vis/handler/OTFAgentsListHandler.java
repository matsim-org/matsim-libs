package playground.david.vis.handler;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.utils.vis.snapshots.writers.PositionInfo;

import playground.david.vis.data.OTFDataSimpleAgent;
import playground.david.vis.data.OTFDataWriter;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.data.OTFData.Receiver;
import playground.david.vis.gui.PoolFactory;
import playground.david.vis.interfaces.OTFDataReader;

public class OTFAgentsListHandler extends OTFDataReader {
	static Class agentReceiverClass = null;
	static PoolFactory<OTFDataSimpleAgent.Receiver> factoryAgent;
	
	protected List<OTFDataSimpleAgent.Receiver> agents = new LinkedList<OTFDataSimpleAgent.Receiver>();
	
	@SuppressWarnings("unchecked")
	static public class Writer extends  OTFDataWriter implements Serializable {


		/**
		 * 
		 */
		private static final long serialVersionUID = -6368752578878835954L;
		
		public transient Collection<PositionInfo> positions = new ArrayList<PositionInfo>();

		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
		}


		public void writeAgent(PositionInfo pos, ByteBuffer out) throws IOException {
			String id = pos.getAgentId().toString();
			out.putInt(id.length());
			for (int i=0; i<id.length(); i++) out.putChar(id.charAt(i));
			out.asCharBuffer().put(pos.getAgentId().toString());
			out.putFloat((float)(pos.getEasting() - OTFServerQuad.offsetEast));
			out.putFloat((float)(pos.getNorthing()- OTFServerQuad.offsetNorth));
			out.putInt(Integer.parseInt(pos.getVisualizerData()));
			out.putFloat((float)pos.getSpeed());
		}
		
		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			// Write additional agent data
	        /*
	         * (4) write agents
	         */
			out.putInt(positions.size());

			for (PositionInfo pos : positions) {
				writeAgent(pos, out);
			}
	        positions.clear();
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

			OTFDataSimpleAgent.Receiver drawer = null;
			try {
				drawer = (playground.david.vis.data.OTFDataSimpleAgent.Receiver) agentReceiverClass.newInstance();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //factoryAgent.getOne();
			drawer.setAgent(idBuffer, x, y, state, color);
			agents.add(drawer);

 	}
	

	@Override
	public void readDynData(ByteBuffer in) throws IOException {
		// read additional agent data
		agents.clear();
		
		int count = in.getInt();
		for(int i= 0; i< count; i++) readAgent(in);
	}



	@Override
	public void readConstData(ByteBuffer in) throws IOException {
	}




	@Override
	public void connect(Receiver receiver) {
		//connect agent receivers
		if (receiver  instanceof OTFDataSimpleAgent.Receiver) {
			this.agentReceiverClass = receiver.getClass();
			this.factoryAgent = PoolFactory.get(this.agentReceiverClass);
		}

	}

	
	@Override
	public void invalidate() {
		// invalidate agent receivers
		for(OTFDataSimpleAgent.Receiver agent : agents) agent.invalidate();
	}


}
