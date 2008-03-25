package playground.david.vis.handler;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.mobsim.QueueLink;
import org.matsim.trafficmonitoring.LinkTravelTimeCounter;
import org.matsim.utils.misc.Time;

import playground.david.vis.data.OTFDataWriter;

public class OTFLinkTravelTimesHandler extends OTFDefaultLinkHandler {

	private static transient LinkTravelTimeCounter count =null;
	static public class Writer extends  OTFDefaultLinkHandler.Writer {
		public Writer() {
		}
		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			Double erg = count.getLastLinkTravelTime(src.getId().toString());
			if (erg != null) out.putFloat((float)(src.getLength()/erg.doubleValue()));
			else out.putFloat((float)src.getFreespeed(Time.UNDEFINED_TIME));
			
		}

		@Override
		public OTFDataWriter<QueueLink> getWriter() {
			if (count == null) {
				LinkTravelTimeCounter.init(server.events, 1000000);
				count = LinkTravelTimeCounter.getInstance();
			}
			return new Writer();
		}
	}


}
