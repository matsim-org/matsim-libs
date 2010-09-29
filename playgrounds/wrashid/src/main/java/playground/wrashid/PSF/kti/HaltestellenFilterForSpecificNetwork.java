package playground.wrashid.PSF.kti;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.algorithms.CalcBoundingBox;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

import playground.wrashid.lib.GeneralLib;

public class HaltestellenFilterForSpecificNetwork {
	
	public static void main(String[] args) {
		Network network=GeneralLib.readNetwork("H:/data/cvs/ivt/studies/switzerland/networks/teleatlas-ivtcheu-zrhCutC/network.xml.gz");
		
		CalcBoundingBox bbox = new CalcBoundingBox();
		bbox.run(network);
		
		try {
			readHalteStellenFileAndPrintHalteStellenWithinBoundryOfNetwork("H:/data/cvs/ivt/studies/switzerland/externals/ptNationalModel/Haltestellen.txt",bbox);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static boolean isWithinBoundy(CalcBoundingBox bbox, CoordImpl coord){
		if (bbox.getMinX()<coord.getX() && bbox.getMaxX()>coord.getX()){
			if (bbox.getMinY()<coord.getY() && bbox.getMaxY()>coord.getY()){
				return true;
			}
		}
		return false;
	}
	
	public static void readHalteStellenFileAndPrintHalteStellenWithinBoundryOfNetwork(final String filename, CalcBoundingBox bbox) throws FileNotFoundException, IOException {
		final BufferedReader reader = IOUtils.getBufferedReader(filename);
		String line = reader.readLine(); // header
		while ((line = reader.readLine()) != null) {
			String[] parts = StringUtils.explode(line, '\t');
			if (parts.length == 7) {
				CoordImpl coord = new CoordImpl(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
				
				if (isWithinBoundy(bbox,coord)){
					System.out.println(line);
				}
				
			} else {
				System.out.println("Could not parse line: " + line);
			}
		}
	}
}
