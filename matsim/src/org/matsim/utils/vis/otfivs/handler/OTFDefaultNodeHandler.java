package org.matsim.utils.vis.otfivs.handler;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.matsim.mobsim.QueueNode;
import org.matsim.utils.vis.otfivs.caching.SceneGraph;
import org.matsim.utils.vis.otfivs.data.OTFData;
import org.matsim.utils.vis.otfivs.data.OTFDataWriter;
import org.matsim.utils.vis.otfivs.data.OTFDataXYCoord;
import org.matsim.utils.vis.otfivs.data.OTFServerQuad;
import org.matsim.utils.vis.otfivs.data.OTFWriterFactory;
import org.matsim.utils.vis.otfivs.interfaces.OTFDataReader;


public class OTFDefaultNodeHandler extends OTFDataReader implements  OTFDataXYCoord.Provider  {
	private OTFDataXYCoord.Receiver xyReceiver = null;

	static public class Writer extends  OTFDataWriter<QueueNode> implements Serializable,OTFWriterFactory<QueueNode> {

		/**
		 * 
		 */
		private static final long serialVersionUID = 8011757932341886429L;

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
