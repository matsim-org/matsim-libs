package playground.david.vis.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.snapshots.PositionInfo;
import org.matsim.mobsim.snapshots.PositionInfo.VehicleState;

import playground.david.vis.data.OTFDataSimpleAgent;
import playground.david.vis.data.OTFDataWriter;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.data.OTFWriterFactory;
import playground.david.vis.data.OTFData.Receiver;
import playground.david.vis.gui.PoolFactory;

public class OTFLinkAgentsHandler extends OTFDefaultLinkHandler {
	static Class agentReceiverClass = null;
	static PoolFactory<OTFDataSimpleAgent.Receiver> factoryAgent;
	
	protected List<OTFDataSimpleAgent.Receiver> agents = new LinkedList<OTFDataSimpleAgent.Receiver>();
	
	static public class Writer extends  OTFDefaultLinkHandler.Writer implements Serializable, OTFWriterFactory<QueueLink>{

		static transient Collection<PositionInfo> positions = new ArrayList<PositionInfo>();

		@Override
		public void writeConstData(DataOutputStream out) throws IOException {
			super.writeConstData(out);
		}


		public void writeAgent(PositionInfo pos, DataOutputStream out) throws IOException {
			out.writeUTF(pos.getAgentId().toString());
			out.writeFloat((float)(pos.getEasting() - OTFServerQuad.offsetEast));
			out.writeFloat((float)(pos.getNorthing()- OTFServerQuad.offsetNorth));
			out.writeInt(pos.getVehicleState()== VehicleState.Parking ? 1:0);
			out.writeFloat((float)pos.getSpeed());
		}
		
		@Override
		public void writeDynData(DataOutputStream out) throws IOException {
			super.writeDynData(out);
			// Write additional agent data
	        /*
	         * (4) write agents
	         */
	        positions.clear();
			src.getVehiclePositions(positions);
			out.writeInt(positions.size());

			for (PositionInfo pos : positions) {
				writeAgent(pos, out);
			}
		}

		@Override
		public OTFDataWriter<QueueLink> getWriter() {
			return new Writer();
		}

	}
	
	public void readAgent(DataInputStream in) throws IOException {
		String id = in.readUTF();
		float x = in.readFloat();
		float y = in.readFloat();
		int state = in.readInt();
		// Convert to km/h 
		float color = in.readFloat()*3.6f;

			OTFDataSimpleAgent.Receiver drawer =  factoryAgent.getOne();
			drawer.setAgent(id, x, y, state, color);
			agents.add(drawer);

 	}
	
	@Override
	public void readDynData(DataInputStream in) throws IOException {
		super.readDynData(in);
		// read additional agent data
		agents.clear();
		
		int count = in.readInt();
		for(int i= 0; i< count; i++) readAgent(in);
	}


	@Override
	public void readConstData(DataInputStream in) throws IOException {
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


}
