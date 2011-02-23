package playground.gregor.gis.dxf;


import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFEntity;
import org.kabeja.dxf.DXFLayer;
import org.kabeja.dxf.DXFLine;
import org.kabeja.dxf.helpers.Point;



public class DXFTest {
	
	public static void main(String [] args) {
		DXFDocument doc = new DXFDocument();
		DXFLayer layer = new DXFLayer();
		doc.addDXFLayer(layer);
		
		DXFLine e = new DXFLine();
		layer.addDXFEntity(e);
		
		Point p1 = new Point(0,0,0);
		e.setStartPoint(p1);
		
		
		Point p2 = new Point(10,0,0);
		e.setEndPoint(p2);
		
	
	}

}
