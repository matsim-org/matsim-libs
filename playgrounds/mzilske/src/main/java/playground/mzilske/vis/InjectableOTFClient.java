package playground.mzilske.vis;

import java.awt.BorderLayout;
import java.awt.geom.Rectangle2D.Double;
import java.rmi.RemoteException;
import java.util.List;

import org.matsim.vis.otfvis.OTFClient;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFQueryControl;
import org.matsim.vis.otfvis.gui.OTFQueryControlToolBar;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.gui.QueryEntry;
import org.matsim.vis.otfvis.gui.OTFQueryControl.IdResolver;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

public final class InjectableOTFClient extends OTFClient {

	protected OTFConnectionManager connect = new OTFConnectionManager();

	public InjectableOTFClient() {
		super();

		this.connect.connectWriterToReader(LinkHandler.Writer.class, LinkHandler.class);
		this.connect.connectWriterToReader(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);

		this.connect.connectReaderToReceiver(OTFAgentsListHandler.class,  OGLAgentPointLayer.AgentPointDrawer.class);
		this.connect.connectReaderToReceiver(LinkHandler.class,  SimpleStaticNetLayer.SimpleQuadDrawer.class);

		this.connect.connectReceiverToLayer(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);		
		this.connect.connectReceiverToLayer(AgentPointDrawer.class, OGLAgentPointLayer.class);

	}

	protected OTFClientQuad getRightDrawerComponent() throws RemoteException {
		OTFConnectionManager connectR = this.connect.clone();
		OTFClientQuad clientQ2 = createNewView(null, connectR, this.hostControlBar.getOTFHostControl());
		return clientQ2;
	}

	@Override
	protected OTFDrawer createDrawer(){
		try {
			frame.getContentPane().add(new OTFTimeLine("time", hostControlBar), BorderLayout.SOUTH);
			OTFOGLDrawer mainDrawer = new OTFOGLDrawer(this.getRightDrawerComponent());
			if (hostControlBar.getOTFHostControl().isLiveHost()) {
				final OTFQueryControl queryControl = new OTFQueryControl(hostControlBar, OTFClientControl.getInstance().getOTFVisConfig());
				queryControl.getQueries().clear();
				queryControl.getQueries().add(new QueryEntry("agentPlan", "show the current plan of an agent", EventBasedQueryAgentPlan.class));
				queryControl.setAgentIdResolver(new IdResolver() {

					@Override
					public List<String> resolveId(Double origRect) {
						EventBasedQueryAgentId.Result agentIdQuery = (EventBasedQueryAgentId.Result) queryControl.createQuery(new EventBasedQueryAgentId(origRect));
						return agentIdQuery.agentIds;
					}


				});
				OTFQueryControlToolBar queryControlBar = new OTFQueryControlToolBar(queryControl, OTFClientControl.getInstance().getOTFVisConfig());
				queryControl.setQueryTextField(queryControlBar.getTextField());
				frame.getContentPane().add(queryControlBar, BorderLayout.SOUTH);
				mainDrawer.setQueryHandler(queryControl);
			}
			return mainDrawer;
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	@Override
	protected OTFVisConfig createOTFVisConfig() {
		try {
			saver = new SettingsSaver(this.masterHostControl.getAddress());
			return this.masterHostControl.getOTFServer().getOTFVisConfig();
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

}
