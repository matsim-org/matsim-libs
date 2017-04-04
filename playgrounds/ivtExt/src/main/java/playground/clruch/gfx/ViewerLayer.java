package playground.clruch.gfx;

import java.awt.Graphics2D;

import playground.clruch.net.SimulationObject;

public abstract class ViewerLayer {

    final MatsimJMapViewer matsimJMapViewer;

    public ViewerLayer(MatsimJMapViewer matsimJMapViewer) {
        this.matsimJMapViewer = matsimJMapViewer;
    }

    public void perpareHeatmaps(SimulationObject ref) {        
    }

    abstract void paint(Graphics2D graphics, SimulationObject ref);

    abstract void hud(Graphics2D graphics, SimulationObject ref);

}
