package playground.david.vis.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import org.matsim.mobsim.QueueNode;

import playground.david.vis.data.OTFData;
import playground.david.vis.data.OTFDataWriter;
import playground.david.vis.data.OTFDataXYCoord;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.data.OTFWriterFactory;
import playground.david.vis.interfaces.OTFDataReader;

public class OTFDefaultNodeHandler implements OTFDataReader,  OTFDataXYCoord.Provider  {
	private OTFDataXYCoord.Receiver xyReceiver = null;

	static public class Writer extends  OTFDataWriter<QueueNode> implements Serializable,OTFWriterFactory<QueueNode> {

		@Override
		public void writeConstData(DataOutputStream out) throws IOException {
			out.writeFloat((float)(src.getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.writeFloat((float)(src.getCoord().getY() - OTFServerQuad.offsetNorth));
		}

		@Override
		public void writeDynData(DataOutputStream out) throws IOException {
		}
		
		public OTFDataWriter<QueueNode> getWriter() {
			return new Writer();
		}

}

	
	public void readConstData(DataInputStream in) throws IOException {
		if (xyReceiver != null) xyReceiver.setXYCoord(in.readFloat(), in.readFloat());
		else {in.readFloat();in.readFloat();};
	}

	public void readDynData(DataInputStream in) throws IOException {
	}


	public void connect(OTFData.Receiver receiver) {
		if (receiver instanceof OTFDataXYCoord.Receiver) {
			this.xyReceiver = (OTFDataXYCoord.Receiver) receiver;
			
		}
	}

	public void invalidate() {
		if (xyReceiver != null) this.xyReceiver.invalidate();
	}


}
