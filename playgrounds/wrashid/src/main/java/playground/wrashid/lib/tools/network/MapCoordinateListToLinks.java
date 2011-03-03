package playground.wrashid.lib.tools.network;

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;

public class MapCoordinateListToLinks {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputNetworkPath="H:/data/experiments/ARTEMIS/output/run10/output_network.xml.gz";
		
		LinkedList<Coord> linkCoordinates=new LinkedList<Coord>();
		
		linkCoordinates.add(new CoordImpl(682703.870,	248719.332));
		linkCoordinates.add(new CoordImpl(682693.522, 248703.491));
		linkCoordinates.add(new CoordImpl(682661.441, 248667.857));
		linkCoordinates.add(new CoordImpl(682749.022, 248673.041));
		linkCoordinates.add(new CoordImpl(682774.155, 248617.997));
		linkCoordinates.add(new CoordImpl(682731.214, 248689.764));
		linkCoordinates.add(new CoordImpl(682631.760, 248651.038));
		linkCoordinates.add(new CoordImpl(682619.049, 248653.140));
		linkCoordinates.add(new CoordImpl(682610.793, 248673.261));
		linkCoordinates.add(new CoordImpl(682755.168, 248760.162));
		linkCoordinates.add(new CoordImpl(682713.204, 248799.523));
		linkCoordinates.add(new CoordImpl(682691.848, 248817.188));
		linkCoordinates.add(new CoordImpl(682873.382, 248537.362));
		linkCoordinates.add(new CoordImpl(682801.240, 248640.035));
		linkCoordinates.add(new CoordImpl(682860.600, 248613.556));

		NetworkImpl network= GeneralLib.readNetwork(inputNetworkPath);
		
		LinkedList<LinkImpl> selectedLinks=new LinkedList<LinkImpl>();
		
		for (Coord coordinate:linkCoordinates){
				LinkImpl nearestLink = network.getNearestLink(coordinate);
				System.out.println(nearestLink.getId());
				selectedLinks.add(nearestLink);
		}

		createKMLAtTempLocation(selectedLinks);
	}

	private static void createKMLAtTempLocation(LinkedList<LinkImpl> selectedLinks) {
		BasicPointVisualizer basicPointVisualizer=new BasicPointVisualizer();
		
		for (Link link:selectedLinks){
			basicPointVisualizer.addPointCoordinate(link.getCoord(), link.getId().toString(),Color.GREEN);
		}
		
		basicPointVisualizer.write(GeneralLib.eclipseLocalTempPath + "/selectedLinks.kml");
		
	}

}
