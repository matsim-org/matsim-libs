package playground.gregor.sim2d.otfdebug;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.evacuation.otfvis.drawer.AgentDrawer;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;

public class ForceArrowReader  extends OTFDataReader {

	@Override
	public void connect(OTFDataReceiver receiver) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invalidate(SceneGraph graph) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		// read additional agent data
		ForceArrowDrawer forces = new ForceArrowDrawer();
		graph.addItem(forces);
		int count = in.getInt();
		for(int i= 0; i< count; i++) {
			float x = in.getFloat();
			float y = in.getFloat();
			float dx= in.getFloat();
			float dy= in.getFloat();
			float color = in.getFloat();
			forces.addForce(x,y,dx,dy,color);
		}
		
	}

}
