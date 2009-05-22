package playground.gregor.gis.shapefiletransformation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.basic.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class CRSTransformer {

	
	private final String inFile;
	private final String outFile;
	private CoordinateReferenceSystem scrs;
	private CoordinateReferenceSystem tcrs;
	private CoordinateTransformation transform;
	
	public CRSTransformer(String inFile, String outFile) {
		this.inFile = inFile;
		this.outFile = outFile;
		
		
	}
	
	public void run() throws IOException {
		FeatureSource fts = ShapeFileReader.readDataFile(this.inFile);
		Iterator it = fts.getFeatures().iterator();
		List<Feature> outFts = new ArrayList<Feature>(); 
		this.scrs = fts.getSchema().getDefaultGeometry().getCoordinateSystem();
		this.tcrs = MGC.getCRS(TransformationFactory.WGS84);
		this.transform  = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_UTM47S, TransformationFactory.WGS84);
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Geometry geo = ft.getDefaultGeometry();
			Coordinate [] coords = geo.getCoordinates();
			for (int i = 0; i < coords.length; i++) {
				Coordinate c = coords[i];
				transformCoord(c);
				
			}
			outFts.add(ft);
		}
		
		ShapeFileWriter.writeGeometries(outFts, this.outFile);
		
	}
	
	
	private void transformCoord(Coordinate c) {
		Coord cc = this.transform.transform(MGC.coordinate2Coord(c));
		c.x = cc.getX();
		c.y = cc.getY();
	}

	public static void main(String [] args) {
		String inFile = "../../../workspace/vsp-cvs/studies/padang/gis/network_v20080618/nodes.shp";
		String outFile = "./tmp/nodes.shp";
		try {
			new CRSTransformer(inFile,outFile).run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	
}
