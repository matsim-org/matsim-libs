package playground.gregor.otf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import org.geotools.data.FeatureSource;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.opengl.drawer.SimpleBackgroundFeatureDrawer;

public class TileDrawerDataWriter extends OTFDataWriter {


	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 864783194545566461L;
	private String data;



	public TileDrawerDataWriter() {
		this.data = "localhost:8080";
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
