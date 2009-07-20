package playground.gregor.otf.readerwriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;

import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleBackgroundLayer;

import playground.gregor.otf.drawer.OTFBackgroundTexturesDrawer;

public class TextutreDataReader extends OTFDataReader{

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
		int size = in.getInt();
		
		 byte[] byts = new byte[size];
		 
		 
		 
		    in.get(byts);
		    
		    ObjectInputStream istream = null;
		 
		    try {
		        istream = new ObjectInputStream(new ByteArrayInputStream(byts));
		        Object obj = istream.readObject();
		 
		        if(obj instanceof OTFBackgroundTexturesDrawer){
		        	OGLSimpleBackgroundLayer.addPersistentItem((OTFBackgroundTexturesDrawer)obj);
		        }
		    }
		    catch(IOException e){
		        e.printStackTrace();
		    }
		    catch(ClassNotFoundException e){
		        e.printStackTrace();
		    }
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
