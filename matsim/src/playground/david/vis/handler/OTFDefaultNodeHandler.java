package playground.david.vis.handler;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.matsim.mobsim.QueueNode;

import playground.david.vis.data.OTFData;
import playground.david.vis.data.OTFDataWriter;
import playground.david.vis.data.OTFDataXYCoord;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.data.OTFWriterFactory;
import playground.david.vis.data.SceneGraph;
import playground.david.vis.interfaces.OTFDataReader;

public class OTFDefaultNodeHandler extends OTFDataReader implements  OTFDataXYCoord.Provider  {
	private OTFDataXYCoord.Receiver xyReceiver = null;

	static public class Writer extends  OTFDataWriter<QueueNode> implements Serializable,OTFWriterFactory<QueueNode> {

		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
			out.putFloat((float)(this.src.getNode().getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.putFloat((float)(this.src.getNode().getCoord().getY() - OTFServerQuad.offsetNorth));
		}

		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
		}

		public OTFDataWriter<QueueNode> getWriter() {
			return new Writer();
		}

}


	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		if (this.xyReceiver != null) this.xyReceiver.setXYCoord(in.getFloat(), in.getFloat());
		else {in.getFloat();in.getFloat();};
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
	}


	@Override
	public void connect(OTFData.Receiver receiver) {
		if (receiver instanceof OTFDataXYCoord.Receiver) {
			this.xyReceiver = (OTFDataXYCoord.Receiver) receiver;

		}
	}

	@Override
	public void invalidate(SceneGraph graph) {
		if (this.xyReceiver != null) this.xyReceiver.invalidate(graph);
	}


}
