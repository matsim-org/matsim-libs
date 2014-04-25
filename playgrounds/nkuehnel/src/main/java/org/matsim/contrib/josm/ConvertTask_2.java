package org.matsim.contrib.josm;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * The Task that handles the convert action
 * 
 * 
 */
public class ConvertTask_2 extends PleaseWaitRunnable {

	private NetworkLayer newLayer;
	private Layer layer;

	public ConvertTask_2() {
		super("Convert to MATSim network");
		layer = Main.main.getActiveLayer();
	}

	@Override
	protected void cancel() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void realRun() throws SAXException, IOException,
			OsmTransferException {
		this.progressMonitor.setTicksCount(2);
		this.progressMonitor.setTicks(0);

		Network tempNetwork = NetworkImpl.createNetwork();

		Converter_2 converter;
		converter = new Converter_2((OsmDataLayer) layer, tempNetwork);
		this.progressMonitor.setTicks(1);
		this.progressMonitor.setCustomText("converting osm data..");
		converter.convert();
		this.progressMonitor.setTicks(2);
		
	}

	@Override
	protected void finish() {
		Main.main.removeLayer(layer);
		Main.main.addLayer(layer);
	}

}
