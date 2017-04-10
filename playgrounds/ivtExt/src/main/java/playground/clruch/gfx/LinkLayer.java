package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.File;
import java.nio.file.Files;

import javax.swing.JCheckBox;

import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.ZeroScalar;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.io.ObjectFormat;
import playground.clruch.net.OsmLink;
import playground.clruch.net.SimulationObject;
import playground.clruch.utils.gui.RowPanel;

public class LinkLayer extends ViewerLayer {

    private volatile boolean drawLinks = false;
    private volatile boolean drawMaxCars = false;
    private static final Color LINKCOLOR = new Color(153, 153, 102, 64);

    Tensor matrix = null; //

    public LinkLayer(MatsimMapComponent matsimMapComponent) {
        super(matsimMapComponent);

        {
            File file = new File("output/linkstats.obj");
            if (file.isFile())
                try {
                    matrix = ObjectFormat.from(Files.readAllBytes(file.toPath()));
                    System.out.println("loaded: " + Dimensions.of(matrix));
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
        }
    }

    @Override
    void paint(Graphics2D graphics, SimulationObject ref) {
        if (drawLinks) {
            graphics.setColor(LINKCOLOR);
            for (OsmLink osmLink : matsimMapComponent.db.getOsmLinks()) {
                Point p1 = matsimMapComponent.getMapPosition(osmLink.coords[0]);
                if (p1 != null) {
                    Point p2 = matsimMapComponent.getMapPositionAlways(osmLink.coords[1]);
                    graphics.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

        if (drawMaxCars && matrix != null) {
            graphics.setColor(Color.DARK_GRAY);
            int index = 0;
            for (OsmLink osmLink : matsimMapComponent.db.getOsmLinks()) {
                Point middle = matsimMapComponent.getMapPosition(osmLink.getAt(.3333));
                if (middle != null) {
                    Scalar carC = matrix.Get(index, 1);
                    if (!carC.equals(ZeroScalar.get()))
                        graphics.drawString("" + carC, middle.x, middle.y);
                }
                ++index;
            }
        }

    }

    @Override
    void hud(Graphics2D graphics, SimulationObject ref) {
        matsimMapComponent.append("%5d streets", matsimMapComponent.db.getOsmLinksSize());
        matsimMapComponent.appendSeparator();
    }

    public void setDraw(boolean selected) {
        drawLinks = selected;
        matsimMapComponent.repaint();
    }

    public void setDrawMaxCars(boolean selected) {
        drawMaxCars = selected;
        matsimMapComponent.repaint();
    }

    @Override
    protected void createPanel(RowPanel rowPanel) {
        {
            final JCheckBox jCheckBox = new JCheckBox("streets");
            jCheckBox.setSelected(drawLinks);
            jCheckBox.addActionListener(e -> setDraw(jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }
        {
            final JCheckBox jCheckBox = new JCheckBox("maxCars");
            jCheckBox.setSelected(drawMaxCars);
            jCheckBox.addActionListener(e -> setDrawMaxCars(jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }

    }

}
