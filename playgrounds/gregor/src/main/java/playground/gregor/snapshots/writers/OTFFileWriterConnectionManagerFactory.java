package playground.gregor.snapshots.writers;

import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFConnectionManagerFactory;

public class OTFFileWriterConnectionManagerFactory implements
		OTFConnectionManagerFactory {

	@Override
	public OTFConnectionManager createConnectionManager() {
		OTFConnectionManager c = new OTFConnectionManager();
		return c;
	}

}
