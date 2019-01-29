package org.matsim.contrib.analysis.vsp.qgis;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class QGisMapnikFileCreator {
	
	public static void writeMapnikFile(String osmMapnikFile) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter( osmMapnikFile ) ;

			writer.write("<GDAL_WMS>\n");
			writer.write("\t<Service name=\"TMS\">\n");
			writer.write("\t\t<ServerUrl>http://tile.openstreetmap.org/${z}/${x}/${y}.png</ServerUrl>\n");
			writer.write("\t</Service>\n");
			writer.write("\t<DataWindow>\n");
			//TODO the following four lines seem to have to be made adjustable to transfer the file created here to another region (dz, dez'14)
			writer.write("\t\t<UpperLeftX>-20037508.34</UpperLeftX>\n");
			writer.write("\t\t<UpperLeftY>20037508.34</UpperLeftY>\n");
			writer.write("\t\t<LowerRightX>20037508.34</LowerRightX>\n");
			writer.write("\t\t<LowerRightY>-20037508.34</LowerRightY>\n");
			writer.write("\t\t<TileLevel>18</TileLevel>\n");
			writer.write("\t\t<TileCountX>1</TileCountX>\n");
			writer.write("\t\t<TileCountY>1</TileCountY>\n");
			writer.write("\t\t<YOrigin>top</YOrigin>\n");
			writer.write("\t</DataWindow>\n");
			writer.write("\t<Projection>EPSG:3857</Projection>\n");
			writer.write("\t<BlockSizeX>256</BlockSizeX>\n");
			writer.write("\t<BlockSizeY>256</BlockSizeY>\n");
			writer.write("\t<BandsCount>3</BandsCount>\n");
			writer.write("\t<Cache>\n");
			writer.write("\t\t<Path>/tmp/cache_osm_mapnik</Path>\n");
			writer.write("\t</Cache>\t\n");
			writer.write("</GDAL_WMS>\n");

			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException( "writing Mapnik file did not work") ;
		}
	}

}
