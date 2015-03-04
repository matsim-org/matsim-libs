package playground.dhosse.qgis.layerTemplates;

import playground.dhosse.qgis.QGisConstants;
import playground.dhosse.qgis.rendering.QGisRasterRenderer;

public class AccessibilityXmlRenderer extends QGisRasterRenderer {

	public AccessibilityXmlRenderer() {
		super(0.490196, -1, 3, 2, QGisConstants.rasterRendererType.multibandcolor, 1);
	}

}
