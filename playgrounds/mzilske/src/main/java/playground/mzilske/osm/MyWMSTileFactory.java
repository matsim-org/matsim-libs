package playground.mzilske.osm;

import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.jdesktop.swingx.mapviewer.wms.WMSService;

public class MyWMSTileFactory extends DefaultTileFactory {
	public MyWMSTileFactory(final WMSService wms) {
		super(new TileFactoryInfo(1,15,17, 
				256, true, true, // tile size and x/y orientation is r2l & t2b
				"","x","y","zoom") {
			public String getTileUrl(int x, int y, int zoom) {
				int zz = 17-zoom;
				int z = 4;
				z = (int)Math.pow(2,(double)zz-1);
				return wms.toWMSURL(x-z, z-1-y, zz, getTileSize(zoom));
			}

		});
	}
}
