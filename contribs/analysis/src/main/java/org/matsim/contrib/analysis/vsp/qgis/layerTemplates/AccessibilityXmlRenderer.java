package org.matsim.contrib.analysis.vsp.qgis.layerTemplates;

import org.matsim.contrib.analysis.vsp.qgis.QGisConstants;
import org.matsim.contrib.analysis.vsp.qgis.rendering.QGisRasterRenderer;

public class AccessibilityXmlRenderer extends QGisRasterRenderer {

	public AccessibilityXmlRenderer() {
		super(0.490196, -1, 3, 2, QGisConstants.rasterRendererType.multibandcolor, 1);
	}

}
