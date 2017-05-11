package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;

import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Sort;
import ch.ethz.idsc.tensor.sca.Floor;
import playground.clruch.net.SimulationObject;
import playground.clruch.utils.gui.RowPanel;

class IsolineLayer extends ViewerLayer {

    public IsolineLayer(MatsimMapComponent matsimMapComponent) {
        super(matsimMapComponent);
    }

    @Override
    void hud(Graphics2D graphics, SimulationObject ref) {

    }

    @Override
    void paint(Graphics2D graphics, SimulationObject ref) {
        final Dimension dimension = matsimMapComponent.getSize();

        Coord NW = matsimMapComponent.getCoordPositionXY(new Point(0, 0));
        Coord NE = matsimMapComponent.getCoordPositionXY(new Point(dimension.width, 0));
        Coord SW = matsimMapComponent.getCoordPositionXY(new Point(0, dimension.height));
        Coord SE = matsimMapComponent.getCoordPositionXY(new Point(dimension.width, dimension.height));

        Tensor X = Sort.of(Tensors.vectorDouble(NW.getX(), NE.getX(), SW.getX(), SE.getX()));
        Tensor Y = Sort.of(Tensors.vectorDouble(NW.getY(), NE.getY(), SW.getY(), SE.getY()));

        Scalar minX = X.Get(0);
        Scalar maxX = X.Get(3);
        Scalar minY = Y.Get(0);
        Scalar maxY = Y.Get(3);

        double log = Math.floor(Math.log10(maxX.subtract(minX).number().doubleValue()));
        Scalar dX = RealScalar.of(Math.pow(10, log));
        // Floor.of(Log.function.apply().divide(Log.function.apply(RealScalar.of(10))));
        // Scalar dX = (Scalar) Floor.of(maxX.subtract(minX).divide(RealScalar.of(100))).multiply(RealScalar.of(10));
        Scalar dY = (Scalar) Floor.of(maxY.subtract(minY).divide(RealScalar.of(100))).multiply(RealScalar.of(10));
        System.out.println(dX);

        Scalar ofsX = (Scalar) Floor.of(minX.divide(dX)).multiply(dX);
        Scalar ofsY = (Scalar) Floor.of(minY.divide(dY)).multiply(dY);

        graphics.setColor(Color.RED);
        for (int i = 0; i < 10; ++i) {
            Point prev = null;
            for (int j = 0; j < 10; ++j) {
                Scalar pX = ofsX.add(dX.multiply(RealScalar.of(i)));
                Scalar pY = ofsY.add(dY.multiply(RealScalar.of(j)));
                Coord mat = matsimMapComponent.db.referenceFrame.coords_toWGS84.transform( //
                        new Coord(pX.number().doubleValue(), pY.number().doubleValue()));
                Point point = matsimMapComponent.getMapPositionAlways(mat);
                graphics.fillRect(point.x, point.y, 2, 2);
                if (prev != null) {
                    graphics.drawLine(prev.x, prev.y, point.x, point.y);
                }
                prev = point;
            }
        }
    }

    @Override
    protected void createPanel(RowPanel rowPanel) {

    }

}
