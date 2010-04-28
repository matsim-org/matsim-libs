package playground.rost.controller.gui;

import org.matsim.api.core.v01.network.Network;

import playground.rost.controller.uicontrols.TimePanel;
import playground.rost.controller.vismodule.VisModuleContainerImpl;
import playground.rost.controller.vismodule.implementations.FlowLinkVisModule;
import playground.rost.controller.vismodule.implementations.NodeVisModule;
import playground.rost.eaflow.ea_flow.Flow;
import playground.rost.graph.BoundingBox;
import playground.rost.graph.visnetwork.OneWayVisNetwork;

public class VisualizeFlowGUI extends AbstractBasicMapGUIImpl {

	Network network;
	Flow flow;


	public VisualizeFlowGUI(Network network, Flow flow) {
		super("Basic Flow Visualization");

		this.network = network;
		this.flow = flow;

		OneWayVisNetwork oWVN = new OneWayVisNetwork(network, flow);

		TimePanel tControl = new TimePanel(this, flow.getStartTime(),flow.getEndTime());
		this.ownContainer = tControl;

		this.vMContainer = new VisModuleContainerImpl(this);
		this.vMContainer.addVisModule(new NodeVisModule(vMContainer, network));
		this.vMContainer.addVisModule(new FlowLinkVisModule(vMContainer, network, oWVN, tControl, flow));

		BoundingBox bBox = new BoundingBox();
		bBox.run(network);
		this.map.setBoundingBox(bBox);
		this.buildUI();
		this.map.addMapPaintCallback(this.vMContainer);

	}
}
