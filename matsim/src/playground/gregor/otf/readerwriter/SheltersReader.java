package playground.gregor.otf.readerwriter;

import org.matsim.vis.otfvis.interfaces.OTFDataReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;

import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;

import playground.gregor.otf.drawer.OTFInundationDrawer;
import playground.gregor.otf.drawer.OTFSheltersDrawer;
import playground.gregor.otf.drawer.TimeDependentTrigger;

public class SheltersReader extends OTFDataReader {

	private OTFSheltersDrawer drawer;
	private TimeDependentTrigger receiver;

	@Override
	public void connect(OTFDataReceiver receiver) {
		this.receiver = (TimeDependentTrigger) receiver;
		
		
	}

	@Override
	public void invalidate(SceneGraph graph) {
//		this.drawer.invalidate(graph);
		this.receiver = new TimeDependentTrigger();
		this.receiver.setDrawer(this.drawer);
		graph.addItem(this.receiver);
		this.receiver.setTime(graph.getTime());
		
//		this.receiver.invalidate(graph);
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
		 
		        if(obj instanceof OTFSheltersDrawer){
		        	this.drawer = (OTFSheltersDrawer) obj;
		            System.out.println("deserialization successful");
		        }
		    }
		    catch(IOException e){
		        e.printStackTrace();
		    }
		    catch(ClassNotFoundException e){
		        e.printStackTrace();
		    }
//		this.drawer.setData(data);
		
		
		
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		// TODO Auto-generated method stub
		
	}
}


