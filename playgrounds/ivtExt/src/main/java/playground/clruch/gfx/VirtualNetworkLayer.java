// code by jph
package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.util.Map.Entry;

import javax.swing.JCheckBox;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.core.networks.VirtualLink;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.queuey.view.gheat.gui.ColorSchemes;
import ch.ethz.idsc.queuey.view.util.gui.RowPanel;
import ch.ethz.idsc.queuey.view.util.gui.SpinnerLabel;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.red.Max;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationObject;
import playground.clruch.netdata.NetworkCreatorUtils;

public class VirtualNetworkLayer extends ViewerLayer {
    public static final Color COLOR = new Color(128, 153 / 2, 0, 128);
    private VirtualNetwork<Link> virtualNetwork = null;
    private boolean drawVNodes = true;
    private boolean drawVLinks = false;
    VirtualNodeGeometry virtualNodeGeometry = null;
    private VirtualNodeShader virtualNodeShader = VirtualNodeShader.None;
    private ColorSchemes colorSchemes = ColorSchemes.Jet;

    // TODO make this functionality part of tensor library
    public static Tensor normalize1Norm(Tensor count) {
        Tensor prob = count; // SoftmaxLayer.of(count);
        Scalar max = prob.flatten(0).reduce(Max::of).get().Get();        
        if (Scalars.nonZero(max))
            prob = prob.multiply(RealScalar.of(224).divide(max));
        return prob;
    }

    public VirtualNetworkLayer(MatsimMapComponent matsimMapComponent) {
        super(matsimMapComponent);
    }

    @Override
    void hud(Graphics2D graphics, SimulationObject ref) {
    }

    private static Color halfAlpha(Color color) {
        int rgb = color.getRGB() & 0xffffff;
        int alpha = color.getAlpha() / 2;
        return new Color(rgb | (alpha << 24), true);
    }

    @Override
    void paint(Graphics2D graphics, SimulationObject ref) {

        if (drawVNodes) {
            {
                if (virtualNodeShader.renderBoundary()) {
                    graphics.setColor(new Color(128, 128, 128, 128 + 16));
                    for (Entry<VirtualNode<Link>, Shape> entry : virtualNodeGeometry.getShapes(matsimMapComponent).entrySet())
                        graphics.draw(entry.getValue());
                }
            }
            switch (virtualNodeShader) {
            case None:
                graphics.setColor(new Color(128, 128, 128, 64));
                for (Entry<VirtualNode<Link>, Shape> entry : virtualNodeGeometry.getShapes(matsimMapComponent).entrySet())
                    graphics.fill(entry.getValue());
                break;
            case VehicleCount: {
                Tensor count = new VehicleCountVirtualNodeFunction(matsimMapComponent.db, virtualNetwork).evaluate(ref);
                Tensor prob = normalize1Norm(count);
                for (Entry<VirtualNode<Link>, Shape> entry : virtualNodeGeometry.getShapes(matsimMapComponent).entrySet()) {
                    final int i = 255 - prob.Get(entry.getKey().getIndex()).number().intValue();
                    graphics.setColor(halfAlpha(colorSchemes.colorScheme.get(i)));
                    graphics.fill(entry.getValue());
                }
                break;
            }
            case RequestCount: {
                Tensor count = new RequestCountVirtualNodeFunction(matsimMapComponent.db, virtualNetwork).evaluate(ref);
                Tensor prob = normalize1Norm(count);
                for (Entry<VirtualNode<Link>, Shape> entry : virtualNodeGeometry.getShapes(matsimMapComponent).entrySet()) {
                    // graphics.setColor(new Color(128, 128, 128, prob.Get(entry.getKey().index).number().intValue()));
                    final int i = 255 - prob.Get(entry.getKey().getIndex()).number().intValue();
                    graphics.setColor(halfAlpha(colorSchemes.colorScheme.get(i)));
                    graphics.fill(entry.getValue());
                }
                break;
            }
            case MeanRequestDistance: {
                Tensor count = new MeanRequestDistanceVirtualNodeFunction(matsimMapComponent.db, virtualNetwork).evaluate(ref);
                Tensor prob = normalize1Norm(count);
                for (Entry<VirtualNode<Link>, Shape> entry : virtualNodeGeometry.getShapes(matsimMapComponent).entrySet()) {
                    // graphics.setColor(new Color(128, 128, 128, prob.Get(entry.getKey().index).number().intValue()));
                    final int i = 255 - prob.Get(entry.getKey().getIndex()).number().intValue();
                    graphics.setColor(halfAlpha(colorSchemes.colorScheme.get(i)));
                    graphics.fill(entry.getValue());
                }
                break;
            }
            case MeanRequestWaiting: {
                Tensor count = new RequestWaitingVirtualNodeFunction( //
                        matsimMapComponent.db, virtualNetwork, //
                        RequestWaitingVirtualNodeFunction::meanOrZero).evaluate(ref);
                Tensor prob = normalize1Norm(count);
                for (Entry<VirtualNode<Link>, Shape> entry : virtualNodeGeometry.getShapes(matsimMapComponent).entrySet()) {
                    // graphics.setColor(new Color(128, 128, 128, prob.Get(entry.getKey().index).number().intValue()));
                    final int i = 255 - prob.Get(entry.getKey().getIndex()).number().intValue();
                    graphics.setColor(halfAlpha(colorSchemes.colorScheme.get(i)));
                    graphics.fill(entry.getValue());
                }
                break;
            }
            case MedianRequestWaiting: {
                Tensor count = new RequestWaitingVirtualNodeFunction( //
                        matsimMapComponent.db, virtualNetwork, //
                        RequestWaitingVirtualNodeFunction::medianOrZero).evaluate(ref);
                Tensor prob = normalize1Norm(count);
                for (Entry<VirtualNode<Link>, Shape> entry : virtualNodeGeometry.getShapes(matsimMapComponent).entrySet()) {
                    // graphics.setColor(new Color(128, 128, 128, prob.Get(entry.getKey().index).number().intValue()));
                    final int i = 255 - prob.Get(entry.getKey().getIndex()).number().intValue();
                    graphics.setColor(halfAlpha(colorSchemes.colorScheme.get(i)));
                    graphics.fill(entry.getValue());
                }
                break;
            }
            case MaxRequestWaiting: {
                // TODO show numbers!
                Tensor count = new RequestWaitingVirtualNodeFunction( //
                        matsimMapComponent.db, virtualNetwork, //
                        RequestWaitingVirtualNodeFunction::maxOrZero).evaluate(ref);
                Tensor prob = normalize1Norm(count);
                for (Entry<VirtualNode<Link>, Shape> entry : virtualNodeGeometry.getShapes(matsimMapComponent).entrySet()) {
                    // graphics.setColor(new Color(128, 128, 128, prob.Get(entry.getKey().index).number().intValue()));
                    final int i = 255 - prob.Get(entry.getKey().getIndex()).number().intValue();
                    graphics.setColor(halfAlpha(colorSchemes.colorScheme.get(i)));
                    graphics.fill(entry.getValue());
                }
                break;
            }
            default:
                break;
            }
        }
        if (drawVLinks && virtualNetwork != null) {
            final MatsimStaticDatabase db = matsimMapComponent.db;

            graphics.setColor(new Color(255, 0, 0, 64));
            for (VirtualLink<Link> vl : virtualNetwork.getVirtualLinks()) {
                VirtualNode<Link> n1 = vl.getFrom();
                VirtualNode<Link> n2 = vl.getTo();
                Coord c1 = db.referenceFrame.coords_toWGS84.transform(NetworkCreatorUtils.fromTensor(n1.getCoord()));
                Coord c2 = db.referenceFrame.coords_toWGS84.transform(NetworkCreatorUtils.fromTensor(n2.getCoord()));
                Point p1 = matsimMapComponent.getMapPositionAlways(c1);
                Point p2 = matsimMapComponent.getMapPositionAlways(c2);
                graphics.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
    }

    void setDrawVNodes(boolean selected) {
        drawVNodes = selected;
        matsimMapComponent.repaint();
    }

    void setDrawVLinks(boolean selected) {
        drawVLinks = selected;
        matsimMapComponent.repaint();
    }

    @Override
    protected void createPanel(RowPanel rowPanel) {
        {
            final JCheckBox jCheckBox = new JCheckBox("nodes");
            jCheckBox.setToolTipText("voronoi boundaries of virtual nodes");
            jCheckBox.setSelected(drawVNodes);
            jCheckBox.addActionListener(e -> setDrawVNodes(jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }
        {
            SpinnerLabel<VirtualNodeShader> spinnerLabel = new SpinnerLabel<>();
            spinnerLabel.setToolTipText("virtual node shader");
            spinnerLabel.setArray(VirtualNodeShader.values());
            spinnerLabel.setMenuHover(true);
            spinnerLabel.setValue(virtualNodeShader);
            spinnerLabel.addSpinnerListener(cs -> {
                virtualNodeShader = cs;
                matsimMapComponent.repaint();
            });
            spinnerLabel.getLabelComponent().setPreferredSize(new Dimension(100, DEFAULT_HEIGHT));
            rowPanel.add(spinnerLabel.getLabelComponent());
        }
        {
            SpinnerLabel<ColorSchemes> spinnerLabel = new SpinnerLabel<>();
            spinnerLabel.setToolTipText("color scheme");
            spinnerLabel.setArray(ColorSchemes.values());
            spinnerLabel.setValue(colorSchemes);
            spinnerLabel.addSpinnerListener(cs -> {
                colorSchemes = cs;
                matsimMapComponent.repaint();
            });
            spinnerLabel.getLabelComponent().setPreferredSize(new Dimension(100, DEFAULT_HEIGHT));
            spinnerLabel.setMenuHover(true);
            rowPanel.add(spinnerLabel.getLabelComponent());
        }

        {
            final JCheckBox jCheckBox = new JCheckBox("links");
            jCheckBox.setToolTipText("virtual links between nodes");
            jCheckBox.setSelected(drawVLinks);
            jCheckBox.addActionListener(e -> setDrawVLinks(jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }
    }

    public void setVirtualNetwork(VirtualNetwork<Link> virtualNetwork) {
        this.virtualNetwork = virtualNetwork;
        virtualNodeGeometry = new VirtualNodeGeometry(matsimMapComponent.db, virtualNetwork);
    }
}
