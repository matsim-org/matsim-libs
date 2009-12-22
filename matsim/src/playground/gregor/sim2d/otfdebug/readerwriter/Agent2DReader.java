package playground.gregor.sim2d.otfdebug.readerwriter;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;

import playground.gregor.sim2d.otfdebug.drawer.Agent2DDrawer;

public class Agent2DReader extends OTFDataReader{

	private Agent2DDrawer drawer;

	@Override
	public void connect(OTFDataReceiver receiver) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invalidate(SceneGraph graph) {
		this.drawer.invalidate(graph);
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	public void readAgent(ByteBuffer in) {
		String id = ByteBufferUtils.getString(in);
		float x = in.getFloat();
		float y = in.getFloat();
		float azimuth = in.getFloat();
		this.drawer.addAgent(x, y,azimuth,id);
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {

		 this.drawer = new Agent2DDrawer();
		int count = in.getInt();
		for(int i= 0; i< count; i++) readAgent(in);
		
	}

}
