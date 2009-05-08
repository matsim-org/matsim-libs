package playground.gregor.otf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;

import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFData.Receiver;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;

public class InundationDataReader extends OTFDataReader {
	
	private final OTFInundationDrawer drawer;
	private Receiver receiver;

	public InundationDataReader() {
		this.drawer = new OTFInundationDrawer();
		Dummy.myDrawer = this.drawer;
	}
	
	@Override
	public void connect(Receiver receiver) {
		this.receiver = receiver;
	}

	@Override
	public void invalidate(SceneGraph graph) {
//		this.drawer.invalidate(graph);
//		graph.addItem(new Dummy(graph.getTime()));
		graph.addItem(new Dummy());
//		this.receiver.invalidate(graph);
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		
		int offset = in.arrayOffset();
		int length = in.array().length;
		System.out.println("o:" + offset + " l:" + length);
		int i = 0;
		i++;
		byte [] dat = new byte[length-offset];
		in.get(dat, offset, length);
		ByteArrayInputStream r = new ByteArrayInputStream(dat);
		
		ObjectInputStream ino = new ObjectInputStream(r);
		InundationData data = null;
		try {
			data = (InundationData)ino.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		this.drawer.setData(data);
		
		
		
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
