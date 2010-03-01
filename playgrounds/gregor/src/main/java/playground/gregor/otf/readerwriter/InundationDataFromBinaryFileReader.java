package playground.gregor.otf.readerwriter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.matsim.evacuation.otfvis.readerwriter.InundationData;

import playground.gregor.snapshots.OTFSnapshotGenerator;


public class InundationDataFromBinaryFileReader {
	String file = "test.dat";
	
	public InundationData readData(){
		ObjectInputStream o;
		try {
//			o = new ObjectInputStream(new FileInputStream("../../inputs/flooding/flooding_old.dat"));
//			o = new ObjectInputStream(new FileInputStream(this.file));
			o = new ObjectInputStream(new FileInputStream(OTFSnapshotGenerator.SHARED_SVN + "/studies/countries/id/padang/inundation/20100201_sz_pc_2b_tide_subsidence/flooding.dat"));
			InundationData data = (InundationData) o.readObject();
			o.close();
			return data;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new RuntimeException("Error");
	}
	
	
	
	public static void main(String [] args) throws FileNotFoundException, IOException, ClassNotFoundException {
		String file = "test.dat";
		ObjectInputStream o = new ObjectInputStream(new FileInputStream("test.dat"));
		InundationData data = (InundationData) o.readObject();
		o.close();
		
	}
}
