/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.gregor.vis;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.gicentre.utils.move.ZoomPan;
import org.matsim.core.gbl.MatsimRandom;

import playground.gregor.casim.simulation.physics.AbstractCANetwork;
import playground.gregor.proto.ProtoFrame;
import playground.gregor.proto.ProtoFrame.CtrlMsg;
import playground.gregor.proto.ProtoFrame.CtrlMsgRsp;
import playground.gregor.proto.ProtoFrame.Frame;
import playground.gregor.proto.ProtoFrame.Frame.Event;
import playground.gregor.proto.ProtoFrame.Frame.Event.Type;
import playground.gregor.proto.ProtoFrame.FrameRqst;
import playground.gregor.proto.ProtoFrame.FrameServerService.BlockingInterface;
import playground.gregor.proto.ProtoScenario.Scenario;
import playground.gregor.proto.ProtoScenario.Scenario.Network.Link;
import playground.gregor.proto.ProtoScenario.Scenario.Network.Node;
import playground.gregor.proto.ProtoScenario.ScnReq;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.Control;
import playground.gregor.vis.drawing.EventsBasedVisDebugger;
import playground.gregor.vis.drawing.InfoBox;
import processing.core.PVector;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.RpcClientChannel;
import com.googlecode.protobuf.pro.duplex.client.DuplexTcpClientPipelineFactory;
import com.googlecode.protobuf.pro.duplex.execute.RpcServerCallExecutor;
import com.googlecode.protobuf.pro.duplex.execute.ThreadPoolCallExecutor;

public class CASimVisClient {
	private static Logger log = Logger.getLogger(CASimVisClient.class);
	private BlockingInterface visService;
	private RpcController cntr;
	private EventsBasedVisDebugger drawer;

	private final Map<String, CircleProperty> circProps = new HashMap<>();
	private final Map<String, LinkInfo> linkInfos = new HashMap<>();
	private final CircleProperty defaultCp = new CircleProperty();
	private long lastUpdate;
	private Control keyControl;
	private double dT = 1. / 25;

	private final String id = MatsimRandom.getRandom().nextDouble() + ""; // TODO
																			// create
																			// unique
																			// id

	public CASimVisClient(BlockingInterface visService, RpcController cntr) {
		this.visService = visService;
		this.cntr = cntr;
	}

	private void init() {
		this.defaultCp.a = 255;
		this.defaultCp.minScale = 0;
		this.defaultCp.rr = .19f;

		this.keyControl = new Control(this.drawer.zoomer, 90, null);
		this.drawer.addKeyControl(this.keyControl);
	}

	private void run() {

		try {
			CtrlMsg regReq = CtrlMsg.newBuilder()
					.setCtrlMsgTyp(CtrlMsg.Type.REGISTER).setId(id).build();
			CtrlMsgRsp regRsp = visService.ctrl(cntr, regReq);

			ScnReq scReq = ScnReq.newBuilder().build();
			Scenario scRsp = visService.reqScn(cntr, scReq);
			this.drawer = new EventsBasedVisDebugger(scRsp, null);
			this.drawer.addAdditionalDrawer(new InfoBox());

			init();
			ZoomPan zoomer = this.drawer.zoomer;
			int w = this.drawer.getWidth();
			int h = this.drawer.getHeight();
			PVector br = new PVector(w, h);
			PVector tl = new PVector(0, 0);

			loadScenario(scRsp);

			// log.info(rsp);
			while (true) {

				PVector brC = zoomer.getDispToCoord(br);
				PVector tlC = zoomer.getDispToCoord(tl);

				FrameRqst frReq = FrameRqst.newBuilder()
						.setTlX(tlC.x - drawer.getOffsetX() - 100)
						.setTlY(-(drawer.getOffsetY() + tlC.y) + 100)
						.setBrX(brC.x - drawer.getOffsetX() + 100)
						.setBrY(-(drawer.getOffsetY() + brC.y) - 100)
						.setTime(0).setId(id).build();
				Frame frame = visService.reqFrame(cntr, frReq);
				handleFrame(frame);
			}

		} catch (ServiceException e) {
			throw new RuntimeException(e);
		}
	}

	private void loadScenario(Scenario scRsp) {
		Map<String, Node> nodes = new HashMap<>();
		for (Node n : scRsp.getNet().getNodesList()) {
			nodes.put(n.getId(), n);
		}
		Set<String> handled = new HashSet<>();
		for (Link l : scRsp.getNet().getLinksList()) {

			Node from = nodes.get(l.getFromNodeId());
			Node to = nodes.get(l.getToNodeId());
			StringBuffer b1 = new StringBuffer();
			StringBuffer b2 = new StringBuffer();
			b1.append(from.getId());
			b1.append('_');
			b1.append(to.getId());
			String id = b1.toString();
			b2.append(to.getId());
			b2.append('_');
			b2.append(from.getId());
			String refId = b2.toString();

			LinkInfo i = new LinkInfo();
			i.x0 = from.getX();
			i.y0 = from.getY();
			i.x1 = to.getX();
			i.y1 = to.getY();
			i.width = l.getCapacity() * 3;
			// LinkInfoPoly p = new LinkInfoPoly();
			// double dx = (to.getX() - from.getX());
			// double dy = (to.getY() - from.getY());
			// double length = Math.sqrt(dx * dx + dy * dy);
			// dx /= length;
			// dy /= length;
			// double x0 = from.getX() - dy * l.getCapacity() / 2;
			// double y0 = from.getY() + dx * l.getCapacity() / 2;
			// double x1 = from.getX() + dy * l.getCapacity() / 2;
			// double y1 = from.getY() - dx * l.getCapacity() / 2;
			// double x2 = to.getX() + dy * l.getCapacity() / 2;
			// double y2 = to.getY() - dx * l.getCapacity() / 2;
			// double x3 = to.getX() - dy * l.getCapacity() / 2;
			// double y3 = to.getY() + dx * l.getCapacity() / 2;
			// p.x = new double[] { x0, x1, x2, x3, x0 };
			// p.y = new double[] { y0, y1, y2, y3, y0 };
			//
			double area = l.getLength() * l.getCapacity();
			i.area = area;
			//
			// this.linkPolys.put(l.getId(), p);
			this.linkInfos.put(l.getId(), i);
			if (handled.contains(refId)) {
				continue;
			}
			handled.add(id);

			this.drawer.addLineStatic(from.getX(), from.getY(), to.getX(),
					to.getY(), 192, 192, 192, 255, 0, 3 * l.getCapacity());

		}

	}

	private void handleFrame(Frame frame) {
		for (Event e : frame.getEvntList()) {
			if (e.getEvntType() == Type.POS) {
				handlePosEvent(e);
			} else if (e.getEvntType() == Type.LINK_INF) {
				handleLinkInfEvent(e);
			}
		}
		update(frame.getTime());

	}

	private void update(double time) {
		this.keyControl.awaitPause();
		this.keyControl.awaitScreenshot();
		this.keyControl.update(time);
		long timel = System.currentTimeMillis();

		long last = this.lastUpdate;
		long diff = timel - last;
		if (diff < this.dT * 1000 / this.keyControl.getSpeedup()) {
			long wait = (long) (this.dT * 1000 / this.keyControl.getSpeedup() - diff);
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.drawer.update(time);
		this.lastUpdate = System.currentTimeMillis();

	}

	private void handlePosEvent(Event event) {
		double dx = event.getVy();
		double dy = -event.getVx();
		double length = Math.sqrt(dx * dx + dy * dy);
		dx /= length;
		dy /= length;
		double x0 = event.getX() + event.getVx();
		double y0 = event.getY() + event.getVy();
		double al = .20;
		double x1 = x0 + dy * al - dx * al / 4;
		double y1 = y0 - dx * al - dy * al / 4;
		double x2 = x0 + dy * al + dx * al / 4;
		double y2 = y0 - dx * al + dy * al / 4;
		double z = this.drawer.zoomer.getZoomScale();
		int a = 255;
		if (z >= 48 && z < 80) {
			z -= 48;
			a = (int) (255. / 32 * z + .5);
		}
		this.drawer.addLine(event.getX(), event.getY(),
				event.getX() + event.getVx() + dy * al,
				event.getY() + event.getVy() - dx * al, 0, 0, 0, a, 50);
		this.drawer.addTriangle(x0, y0, x1, y1, x2, y2, 0, 0, 0, a, 50, true);

		CircleProperty cp = this.circProps.get(event.getId());
		if (cp == null) {
			cp = createAndAddCircleProperty(event);

		}

		this.drawer.addCircle(event.getX(), event.getY(), cp.rr, cp.r, cp.g,
				cp.b, cp.a, cp.minScale, cp.fill);
		this.drawer.addText(event.getX(), event.getY(), event.getId(), 300);
	}

	private CircleProperty createAndAddCircleProperty(Event event) {
		CircleProperty cp = new CircleProperty();
		cp.rr = (float) Math.sqrt((1 / AbstractCANetwork.RHO_HAT) / Math.PI);
		int nr = event.getId().hashCode() % 100;
		int color = (nr / 10) % 3;
		// if (Integer.parseInt(a.getId().toString()) < 0) {
		// color = 1;
		// } else {
		// color = 2;
		// }
		if (color == 1) {
			cp.r = 255;
			cp.g = 255 - nr;
			cp.b = 0;
			cp.a = 255;
		} else if (color == 2) {
			cp.r = nr - nr;
			cp.g = 0;
			cp.b = 255;
			cp.a = 255;
		} else {
			cp.r = 0;
			cp.g = 255;
			cp.b = 255 - nr;
			cp.a = 255;
		}
		this.circProps.put(event.getId(), cp);
		return cp;
	}

	private void handleLinkInfEvent(Event e) {
		LinkInfo i = this.linkInfos.get(e.getId());
		double density = e.getNrAgents() / i.area;
		int r, g, b;
		if (density < 1) {
			r = 0;
			g = 255;
			b = 0;
		} else if (density < 1.5) {
			r = 255;
			g = 255;
			b = 0;
		} else if (density < 2) {
			r = 255;
			g = 64;
			b = 0;
		} else if (density < 4) {
			r = 255;
			g = 0;
			b = 0;
		} else {
			r = 128;
			g = 0;
			b = 128;
		}
		// this.drawer.addPolygon(p.x, p.y, r, g, b, 255, 0);
		this.drawer.addLine(i.x0, i.y0, i.x1, i.y1, r, g, b, 255, 0, i.width);

	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("usage: VisClient <server> <port>");
			System.exit(-1);
		}
		String serv = args[0];
		int port = Integer.parseInt(args[1]);

		PeerInfo server = new PeerInfo(serv, port);
		DuplexTcpClientPipelineFactory clientFactory = new DuplexTcpClientPipelineFactory();

		// in case client acts as server, which is the reason behind a duplex
		// connection indeed.
		RpcServerCallExecutor executor = new ThreadPoolCallExecutor(3, 100);
		clientFactory.setRpcServerCallExecutor(executor);

		clientFactory.setConnectResponseTimeoutMillis(1000);

		// clientFactory.getRpcServiceRegistry().registerService();

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(new NioEventLoopGroup());
		bootstrap.handler(clientFactory);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
		bootstrap.option(ChannelOption.SO_SNDBUF, 1048576);
		bootstrap.option(ChannelOption.SO_RCVBUF, 1048576);

		RpcClientChannel channel = clientFactory.peerWith(server, bootstrap);

		BlockingInterface visService = ProtoFrame.FrameServerService
				.newBlockingStub(channel);
		RpcController cntr = channel.newRpcController();
		clientFactory
				.getRpcServiceRegistry()
				.registerService(
						ProtoFrame.FrameServerService
								.newReflectiveBlockingService(new BlockingVisServiceImpl(
										null, null)));

		new CASimVisClient(visService, cntr).run();
		//
		// channel.close();
		// executor.shutdown();
		// System.exit(0);
	}

	private static final class CircleProperty {
		boolean fill = true;
		float rr;
		int r, g, b, a, minScale = 0;
	}

	private static final class LinkInfoPoly {
		double[] x;
		double[] y;
		double area;
	}

	private static final class LinkInfo {
		public double area;
		double x0;
		double x1;
		double y0;
		double y1;
		double width;
	}
}
