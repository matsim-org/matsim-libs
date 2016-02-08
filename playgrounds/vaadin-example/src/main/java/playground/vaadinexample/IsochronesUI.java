package playground.vaadinexample;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.vaadin.addon.leaflet.*;
import org.vaadin.addon.leaflet.util.JTSUtil;

import javax.servlet.annotation.WebServlet;
import java.util.ArrayList;
import java.util.Collection;

@Theme("mytheme")
@SuppressWarnings("serial")
public class IsochronesUI extends UI {

    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = IsochronesUI.class, widgetset = "playground.vaadinexample.AppWidgetSet")
    public static class Servlet extends VaadinServlet {
    }

    enum ContainerProperties {
        TRAVEL_TIME, AREA, N_GEOMETRIES;
    }

    @Override
    protected void init(VaadinRequest request) {
        CoordinateReferenceSystem sourceCRS = MGC.getCRS("EPSG:21037");
        CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84);

        MathTransform coordinateTransformation;
        try {
            coordinateTransformation = CRS.findMathTransform(sourceCRS, targetCRS, true);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }

        HierarchicalContainer container = new HierarchicalContainer();
        container.addContainerProperty(ContainerProperties.TRAVEL_TIME, Double.class, 0.0);
        container.addContainerProperty(ContainerProperties.AREA, Double.class, 0.0);
        container.addContainerProperty(ContainerProperties.N_GEOMETRIES, Integer.class, 0);

        TreeTable table = new TreeTable("Isochrones", container);
        table.setColumnHeader(ContainerProperties.TRAVEL_TIME, "Travel Time [s]");
        table.setColumnHeader(ContainerProperties.AREA, "Area [m^2]");
        table.setColumnHeader(ContainerProperties.N_GEOMETRIES, "Number of sub-geometries");

        ContourService contourService = new ContourService();

        LLayerGroup nodesLayer = new LLayerGroup();
        Collection<NodeWithCost> nodes = contourService.getNodes();
        for (final NodeWithCost node : nodes) {
            try {
                LCircleMarker layer = new LCircleMarker((Point) JTS.transform(node.getGeometry(), coordinateTransformation), 2.0);
                layer.setPopup(String.format("z: %.2f", node.getTime()));
                nodesLayer.addComponent(layer);
            } catch (TransformException e) {
                throw new RuntimeException(e);
            }
        }

        Collection<Contour> contours = new ArrayList<Contour>();
        contours.add(contourService.getContour(10.0 * 60.0, "green"));
        contours.add(contourService.getContour(20.0 * 60.0, "yellow"));
        contours.add(contourService.getContour(30.0 * 60.0, "red"));

        LLayerGroup isochronesLayer = new LLayerGroup();
        for (final Contour contour : contours) {
            Object contourId = container.addItem();
            Item contourItem = container.getItem(contourId);
            contourItem.getItemProperty(ContainerProperties.TRAVEL_TIME).setValue(contour.getZ());
            contourItem.getItemProperty(ContainerProperties.AREA).setValue(contour.getArea());
            contourItem.getItemProperty(ContainerProperties.N_GEOMETRIES).setValue(contour.getNGeometries());
            MultiPolygon geometry = ((MultiPolygon) contour.getGeometry());
            for (int i=0; i < geometry.getNumGeometries(); i++) {
                final Polygon polygon = (Polygon) geometry.getGeometryN(i);
                Object polygonId = container.addItem();
                Item polygonItem = container.getItem(polygonId);
                polygonItem.getItemProperty(ContainerProperties.TRAVEL_TIME).setValue(contour.getZ());
                polygonItem.getItemProperty(ContainerProperties.AREA).setValue(polygon.getArea());
                polygonItem.getItemProperty(ContainerProperties.N_GEOMETRIES).setValue(1 + polygon.getNumInteriorRing());
                Object shellId = container.addItem();
                Item shellItem = container.getItem(shellId);
                shellItem.getItemProperty(ContainerProperties.TRAVEL_TIME).setValue(contour.getZ());
                shellItem.getItemProperty(ContainerProperties.AREA).setValue(Math.abs(CGAlgorithms.signedArea(polygon.getExteriorRing().getCoordinates())));
                shellItem.getItemProperty(ContainerProperties.N_GEOMETRIES).setValue(1);
                container.setParent(shellId, polygonId);
                container.setChildrenAllowed(shellId, false);
                for (int j=0; j < polygon.getNumInteriorRing(); j++) {
                    final LinearRing hole = (LinearRing) polygon.getInteriorRingN(j);
                    Object holeId = container.addItem();
                    Item holeItem = container.getItem(holeId);
                    holeItem.getItemProperty(ContainerProperties.TRAVEL_TIME).setValue(contour.getZ());
                    holeItem.getItemProperty(ContainerProperties.AREA).setValue(- Math.abs(CGAlgorithms.signedArea(hole.getCoordinates())));
                    holeItem.getItemProperty(ContainerProperties.N_GEOMETRIES).setValue(1);
                    container.setParent(holeId, polygonId);
                    container.setChildrenAllowed(holeId, false);
                }
                container.setParent(polygonId, contourId);
                try {
                    LPolygon lPolygon = JTSUtil.toPolygon((Polygon) JTS.transform(polygon, coordinateTransformation));
                    lPolygon.setColor(contour.getColor());
                    lPolygon.setFill(false);
                    lPolygon.addClickListener(new LeafletClickListener() {
                        @Override
                        public void onClick(LeafletClickEvent event) {
                            double area = polygon.getArea();
                            Notification contourInfo = new Notification(Double.toString(area), "Area");
                            contourInfo.show(Page.getCurrent());
                        }
                    });
                    isochronesLayer.addComponent(lPolygon);
                } catch (TransformException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        LTileLayer osmTiles = new LTileLayer("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png");
        osmTiles.setAttributionString("© OpenStreetMap Contributors");

        LMap map = new LMap();
        map.addBaseLayer(osmTiles, "OSM");
        map.addOverlay(nodesLayer, "Nodes");
        map.addOverlay(isochronesLayer, "Isochrones");
        map.zoomToContent();

        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.addComponents(map, table);
        layout.setExpandRatio(map, 0.8f);
        layout.setExpandRatio(table, 0.2f);
        table.setSizeFull();
        layout.setSizeFull();
        setContent(layout);
    }

}
