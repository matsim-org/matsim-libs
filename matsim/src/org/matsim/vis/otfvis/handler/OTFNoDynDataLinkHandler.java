package org.matsim.vis.otfvis.handler;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFWriterFactory;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler.Writer;

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
