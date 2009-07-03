package playground.gregor.otf.readerwriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import org.geotools.data.FeatureSource;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.opengl.drawer.SimpleBackgroundFeatureDrawer;

public class PolygonDataWriter extends OTFDataWriter {


	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7454644070258134874L;
	private SimpleBackgroundFeatureDrawer data;



	public PolygonDataWriter(FeatureSource fs, float [] color ) {
		try {
			this.data = new SimpleBackgroundFeatureDrawer(fs,color);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
		
	ByteArrayOutputStream a = new ByteArrayOutputStream();
		
		ObjectOutputStream o = new ObjectOutputStream(a);
		o.writeObject(this.data);
		
		out.putInt(a.toByteArray().length);
		out.put(a.toByteArray());
	}

	
	
	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		// TODO Auto-generated method stub

	}

}
