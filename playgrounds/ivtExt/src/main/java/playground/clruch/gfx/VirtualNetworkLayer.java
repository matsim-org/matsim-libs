package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.tensor.Tensor;
import playground.clruch.net.SimulationObject;

public class VirtualNetworkLayer extends ViewerLayer {
    public static final Color COLOR = new Color(255, 153, 0, 128);
    public PointCloud pc = null;

    private boolean drawCells = true;

    public VirtualNetworkLayer(MatsimJMapViewer matsimJMapViewer) {
        super(matsimJMapViewer);
    }

    @Override
    void hud(Graphics2D graphics, SimulationObject ref) {
    }

    @Override
    void paint(Graphics2D graphics, SimulationObject ref) {
        if (pc != null && drawCells) {
            graphics.setColor(COLOR);
            for (Tensor pnt : pc.tensor) {
                Coord coord = MatsimStaticDatabase.INSTANCE.coordinateTransformation.transform(new Coord( //
                        pnt.Get(0).number().doubleValue(), //
                        pnt.Get(1).number().doubleValue() //
                ));

                Point point = matsimJMapViewer.getMapPosition(coord);
                if (point != null)
                    graphics.drawRect(point.x, point.y, 1, 1);

            }
        }

    }

    public boolean getDrawCells() {
        return drawCells;
    }

    public void setDrawCells(boolean selected) {
        drawCells = selected;
    }

    public void init() {
        drawCells = pc != null;
    }

}
