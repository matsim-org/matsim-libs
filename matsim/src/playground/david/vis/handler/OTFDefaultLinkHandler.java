package playground.david.vis.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import org.matsim.mobsim.QueueLink;

import playground.david.vis.data.OTFDataQuad;
import playground.david.vis.data.OTFDataWriter;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.data.OTFWriterFactory;
import playground.david.vis.data.OTFData.Receiver;
import playground.david.vis.interfaces.OTFDataReader;

public class OTFDefaultLinkHandler implements OTFDataQuad.Provider, OTFDataReader{
	private OTFDataQuad.Receiver quadReceiver = null;

	static public class Writer extends  OTFDataWriter<QueueLink> implements Serializable, OTFWriterFactory<QueueLink> {

		@Override
		public void writeConstData(DataOutputStream out) throws IOException {
			out.writeFloat((float)(src.getFromNode().getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.writeFloat((float)(src.getFromNode().getCoord().getY() - OTFServerQuad.offsetNorth));
			out.writeFloat((float)(src.getToNode().getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.writeFloat((float)(src.getToNode().getCoord().getY() - OTFServerQuad.offsetNorth));
		}

		@Override
		public void writeDynData(DataOutputStream out) throws IOException {
			out.writeFloat((float)src.getDisplayableTimeCapValue());
			
		}

		public OTFDataWriter<QueueLink> getWriter() {
			return new Writer();
		}
	}
	
	public void readDynData(DataInputStream in) throws IOException {
		quadReceiver.setColor(in.readFloat());

	}


	public void readConstData(DataInputStream in) throws IOException {
		quadReceiver.setQuad(in.readFloat(), in.readFloat(),in.readFloat(), in.readFloat());
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
