// License: GPL. For details, see Readme.txt file.
package playground.clruch.gfx;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import playground.clruch.jmapviewer.Coordinate;
import playground.clruch.jmapviewer.JMapViewer;
import playground.clruch.jmapviewer.JMapViewerTree;
import playground.clruch.jmapviewer.Layer;
import playground.clruch.jmapviewer.LayerGroup;
import playground.clruch.jmapviewer.MapMarkerDot;
import playground.clruch.jmapviewer.OsmTileLoader;
import playground.clruch.jmapviewer.events.JMVCommandEvent;
import playground.clruch.jmapviewer.interfaces.JMapViewerEventListener;
import playground.clruch.jmapviewer.interfaces.TileLoader;
import playground.clruch.jmapviewer.interfaces.TileSource;
import playground.clruch.jmapviewer.tilesources.BingAerialTileSource;
import playground.clruch.jmapviewer.tilesources.OsmTileSource;

/** Demonstrates the usage of {@link JMapViewer}
 *
 * @author Jan Peter Stotz */
public class MatsimViewer {
  final JFrame jFrame = new JFrame();
  private final JMapViewerTree treeMap;
  private final JLabel zoomLabel;
  private final JLabel zoomValue;
  private final JLabel mperpLabelName;
  private final JLabel mperpLabelValue;

  /** Constructs the {@code Demo}. */
  public MatsimViewer(JMapViewer jMapViewer) {
    jFrame.setSize(800, 600);
    treeMap = new JMapViewerTree(jMapViewer, "Zones", false);
    getJMapViewer().setTileGridVisible(true);
    getJMapViewer().addJMVListener(new JMapViewerEventListener() {
      @Override
      public void processCommand(JMVCommandEvent command) {
        if (command.getCommand().equals(JMVCommandEvent.COMMAND.ZOOM) || command.getCommand().equals(JMVCommandEvent.COMMAND.MOVE)) {
          updateZoomParameters();
        }
      }
    });
    jFrame.setLayout(new BorderLayout());
    jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    // jFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    JPanel panel = new JPanel(new BorderLayout());
    JPanel panelTop = new JPanel();
    JPanel panelBottom = new JPanel();
    mperpLabelName = new JLabel("Meters/Pixels: ");
    mperpLabelValue = new JLabel(String.format("%s", getJMapViewer().getMeterPerPixel()));
    zoomLabel = new JLabel("Zoom: ");
    zoomValue = new JLabel(String.format("%s", getJMapViewer().getZoom()));
    jFrame.add(panel, BorderLayout.NORTH);
    panel.add(panelTop, BorderLayout.NORTH);
    panel.add(panelBottom, BorderLayout.SOUTH);
    JComboBox<TileSource> tileSourceSelector = new JComboBox<>(new TileSource[] { //
        new OsmTileSource.Mapnik(), //
        new OsmTileSource.CycleMap(), //
        new BingAerialTileSource(), });
    tileSourceSelector.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        getJMapViewer().setTileSource((TileSource) e.getItem());
      }
    });
    JComboBox<TileLoader> tileLoaderSelector;
    tileLoaderSelector = new JComboBox<>(new TileLoader[] { new OsmTileLoader(getJMapViewer()) });
    tileLoaderSelector.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        getJMapViewer().setTileLoader((TileLoader) e.getItem());
      }
    });
    getJMapViewer().setTileLoader((TileLoader) tileLoaderSelector.getSelectedItem());
    panelTop.add(tileSourceSelector);
    panelTop.add(tileLoaderSelector);
    final JCheckBox showMapMarker = new JCheckBox("markers");
    showMapMarker.setSelected(getJMapViewer().getMapMarkersVisible());
    showMapMarker.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        getJMapViewer().setMapMarkerVisible(showMapMarker.isSelected());
      }
    });
    panelBottom.add(showMapMarker);
    ///
    final JCheckBox showTreeLayers = new JCheckBox("Tree");
    showTreeLayers.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        treeMap.setTreeVisible(showTreeLayers.isSelected());
      }
    });
    panelBottom.add(showTreeLayers);
    ///
    final JCheckBox showToolTip = new JCheckBox("ToolTip");
    showToolTip.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        getJMapViewer().setToolTipText(null);
      }
    });
    panelBottom.add(showToolTip);
    ///
    final JCheckBox showTileGrid = new JCheckBox("grid");
    showTileGrid.setSelected(getJMapViewer().isTileGridVisible());
    showTileGrid.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        getJMapViewer().setTileGridVisible(showTileGrid.isSelected());
      }
    });
    panelBottom.add(showTileGrid);
    final JCheckBox scrollWrapEnabled = new JCheckBox("wrap");
    scrollWrapEnabled.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        getJMapViewer().setScrollWrapEnabled(scrollWrapEnabled.isSelected());
      }
    });
    panelBottom.add(scrollWrapEnabled);
    panelTop.add(zoomLabel);
    panelTop.add(zoomValue);
    panelTop.add(mperpLabelName);
    panelTop.add(mperpLabelValue);
    jFrame.add(treeMap, BorderLayout.CENTER);
    //
    LayerGroup layerGroup = new LayerGroup("Switzerland");
    Layer layer = layerGroup.addLayer("Basel");
    getJMapViewer().addMapMarker(new MapMarkerDot(layer, "Basel", 47.55814, 7.58769));
    getJMapViewer().addMapMarker(new MapMarkerDot(layer, "P1", 47.63653545660296, 7.500400543212891));
    getJMapViewer().addMapMarker(new MapMarkerDot(layer, "P2", 47.5555321038439, 7.61552095413208));
    getJMapViewer().addMapMarker(new MapMarkerDot(layer, "P3", 47.46860880520117, 7.800625562667847));
    getJMapViewer().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        System.out.println(getJMapViewer().getPosition(e.getPoint()).toString());
      }
    });
    getJMapViewer().setZoomContolsVisible(false);
    getJMapViewer().setDisplayPosition(new Point(), new Coordinate(47.55814, 7.58769), 11);
    getJMapViewer().addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        Point p = e.getPoint();
        boolean cursorHand = getJMapViewer().getAttribution().handleAttributionCursor(p);
        if (cursorHand) {
          getJMapViewer().setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
          getJMapViewer().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        if (showToolTip.isSelected())
          getJMapViewer().setToolTipText(getJMapViewer().getPosition(p).toString());
        // System.out.println("asdfasdf");
      }
    });
  }

  private JMapViewer getJMapViewer() {
    return treeMap.getViewer();
  }

  private void updateZoomParameters() {
    if (mperpLabelValue != null)
      mperpLabelValue.setText(String.format("%10.2e", getJMapViewer().getMeterPerPixel()));
    if (zoomValue != null)
      zoomValue.setText(String.format("%s", getJMapViewer().getZoom()));
  }

}
