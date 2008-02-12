package playground.david.vis.handler;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.mobsim.snapshots.PositionInfo;

import playground.david.vis.data.OTFDataSimpleAgentArray;
import playground.david.vis.data.OTFDataWriter;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.data.OTFData.Receiver;
import playground.david.vis.interfaces.OTFDataReader;

public class OTFAgentsListArrayHandler extends OTFDataReader {
	private OTFDataSimpleAgentArray.Receiver receiver = null;
	
	
	static public class Writer extends  OTFDataWriter implements Serializable {

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

		if(receiver != null)receiver.addAgent(idBuffer, x, y, state, color);

 	}
	

	@Override
	public void readDynData(ByteBuffer in) throws IOException {
		int count = in.getInt();
		
		if (this.receiver != null)
			try {
				this.receiver = this.receiver.getClass().newInstance();
				receiver.setMaxSize(count);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		for(int i= 0; i< count; i++) readAgent(in);
	}



	@Override
	public void readConstData(ByteBuffer in) throws IOException {
	}




	@Override
	public void connect(Receiver receiver) {
		//connect agent receivers
		if (receiver  instanceof OTFDataSimpleAgentArray.Receiver) {
			this.receiver = (OTFDataSimpleAgentArray.Receiver)receiver;
		}

	}

	
	@Override
	public void invalidate() {
		if(receiver != null)receiver.invalidate();
	}


}
