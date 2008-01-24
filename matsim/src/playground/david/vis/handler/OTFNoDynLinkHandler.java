package playground.david.vis.handler;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.matsim.mobsim.QueueLink;

import playground.david.vis.data.OTFDataQuad;
import playground.david.vis.data.OTFDataWriter;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.data.OTFWriterFactory;
import playground.david.vis.data.OTFData.Receiver;
import playground.david.vis.interfaces.OTFDataReader;

public class OTFNoDynLinkHandler implements OTFDataQuad.Provider, OTFDataReader{
	private OTFDataQuad.Receiver quadReceiver = null;

	static public class Writer extends  OTFDataWriter<QueueLink> implements Serializable, OTFWriterFactory<QueueLink> {

		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
			out.putFloat((float)(src.getFromNode().getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.putFloat((float)(src.getFromNode().getCoord().getY() - OTFServerQuad.offsetNorth));
			out.putFloat((float)(src.getToNode().getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.putFloat((float)(src.getToNode().getCoord().getY() - OTFServerQuad.offsetNorth));
		}

		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
		}

		public OTFDataWriter<QueueLink> getWriter() {
			return new Writer();
		}
	}
	
	public void readDynData(ByteBuffer in) throws IOException {
	}


	public void readConstData(ByteBuffer in) throws IOException {
		quadReceiver.setQuad(in.getFloat(), in.getFloat(),in.getFloat(), in.getFloat());
	}




	public void connect(Receiver receiver) {
		if (receiver  instanceof OTFDataQuad.Receiver) {
			this.quadReceiver = (OTFDataQuad.Receiver)receiver;
		}

	}

	public void invalidate() {
		this.quadReceiver.invalidate();
	}



}
