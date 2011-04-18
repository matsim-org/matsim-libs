package playground.gregor.sim2d_v2.controller;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;

import playground.gregor.pedvis.PedVisPeekABot;
import playground.gregor.sims.msa.MSATravelTimeCalculatorFactory;

public class PeekabotVisController extends Controller2D{

	private PedVisPeekABot vis;

	public PeekabotVisController(String[] args) {
		super(args);
		setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
	}

	@Override
	protected void loadData() {
		super.loadData();
		this.vis = new PedVisPeekABot(1);
		Link l = this.network.getLinks().get(new IdImpl(0));
		this.vis.setOffsets(l.getCoord().getX(), l.getCoord().getY());
		this.vis.setFloorShapeFile(this.getSim2dConfig().getFloorShapeFile());
		this.vis.drawNetwork(this.network);
		this.events.addHandler(this.vis);
	}

	public static void main(String[] args) {
		Controler controller = new PeekabotVisController(args);
		controller.run();

	}

}
