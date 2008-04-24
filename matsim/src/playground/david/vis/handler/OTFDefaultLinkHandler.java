package playground.david.vis.handler;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.matsim.mobsim.QueueLink;

import playground.david.vis.caching.SceneGraph;
import playground.david.vis.data.OTFDataQuad;
import playground.david.vis.data.OTFDataWriter;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.data.OTFWriterFactory;
import playground.david.vis.data.OTFData.Receiver;
import playground.david.vis.interfaces.OTFDataReader;

public class OTFDefaultLinkHandler extends OTFDataReader implements OTFDataQuad.Provider {
	static boolean prevV1_1 = OTFDataReader.setPreviousVersion(OTFDefaultLinkHandler.class.getCanonicalName() + "V1.1", ReaderV1_1.class);

	private OTFDataQuad.Receiver quadReceiver = null;

	static public class Writer extends  OTFDataWriter<QueueLink> implements Serializable, OTFWriterFactory<QueueLink> {

		/**
		 *
		 */
		private static final long serialVersionUID = 2827811927720044709L;

		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
			out.putFloat((float)(this.src.getLink().getFromNode().getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.putFloat((float)(this.src.getLink().getFromNode().getCoord().getY() - OTFServerQuad.offsetNorth));
			out.putFloat((float)(this.src.getLink().getToNode().getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.putFloat((float)(this.src.getLink().getToNode().getCoord().getY() - OTFServerQuad.offsetNorth));
		}

		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			out.putFloat((float)this.src.getDisplayableTimeCapValue());

		}

		public OTFDataWriter<QueueLink> getWriter() {
			return new Writer();
		}
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		this.quadReceiver.setColor(in.getFloat());

	}


	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		this.quadReceiver.setQuad(in.getFloat(), in.getFloat(),in.getFloat(), in.getFloat());
	}




	@Override
	public void connect(Receiver receiver) {
		if (receiver  instanceof OTFDataQuad.Receiver) {
			this.quadReceiver = (OTFDataQuad.Receiver)receiver;
		}

	}

	@Override
	public void invalidate(SceneGraph graph) {
		this.quadReceiver.invalidate(graph);
	}


	// Prevoius version of the reader


	public static final class ReaderV1_1 extends OTFDefaultLinkHandler {
		@Override
		public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		}


	}
}

