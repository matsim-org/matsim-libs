package playground.clruch.gfx;

import java.awt.Graphics2D;

import playground.clruch.net.SimulationObject;

public abstract class ViewerLayer {

    final MatsimMapComponent matsimMapComponent;

    public ViewerLayer(MatsimMapComponent matsimMapComponent) {
        this.matsimMapComponent = matsimMapComponent;
    }

    public void prepareHeatmaps(SimulationObject ref) {        
    }

    abstract void paint(Graphics2D graphics, SimulationObject ref);

    abstract void hud(Graphics2D graphics, SimulationObject ref);

}
