package org.matsim.utils.vis.otfvis.handler;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.utils.vis.otfvis.caching.SceneGraph;
import org.matsim.utils.vis.otfvis.data.OTFDataWriter;
import org.matsim.utils.vis.otfvis.data.OTFWriterFactory;
import org.matsim.utils.vis.otfvis.handler.OTFDefaultLinkHandler.Writer;

public class OTFNoDynDataLinkHandler extends OTFDefaultLinkHandler {
	static public class Writer extends  OTFDefaultLinkHandler.Writer implements OTFWriterFactory<QueueLink> {
		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			// do nothing
		}
		public OTFDataWriter<QueueLink> getWriter() {
			return new Writer();
		}
	}
	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		// do nothing
	}

}
