package playground.andreas.bln.ana.plans2kml;

import java.io.IOException;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.LineStyleType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.StyleType;

import org.matsim.core.gbl.MatsimResource;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKmlStyleFactory;

public class MyKmlStyleFactory extends MatsimKmlStyleFactory{
	
	// TODO [AN] visibility DG
	private static final Double ICONSCALE = Double.valueOf(0.5);
	
	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	private DocumentType document;
//	private KMZWriter writer = null;
	
	
	private StyleType myCarNetworklinkStyle;
	private StyleType myWalkNetworklinkStyle;
	private StyleType myDBNetworklinkStyle;
	private StyleType myMetroBusTramNetworklinkStyle;
	private StyleType myBusNetworklinkStyle;
	private StyleType myTramNetworklinkStyle;
	private StyleType mySubWayNetworklinkStyle;
	private StyleType mySBAHNNetworklinkStyle;
	
	// The order of expression is aabbggrr, where aa=alpha (00 to ff); bb=blue (00 to ff); gg=green (00 to ff); rr=red (00 to ff).
	private static final byte[] CAR = new byte[]{(byte) 255, (byte) 0, (byte) 255, (byte) 255};
	private static final byte[] WALK = new byte[]{(byte) 210, (byte) 70, (byte) 50, (byte) 50};
	private static final byte[] PTRED = new byte[]{(byte) 255, (byte) 15, (byte) 15, (byte) 190};
	private static final byte[] PTBLUE = new byte[]{(byte) 255, (byte) 255, (byte) 0, (byte) 0};
	private static final byte[] PTORANGE = new byte[]{(byte) 255, (byte) 0, (byte) 165, (byte) 255};
	private static final byte[] PTGREEN = new byte[]{(byte) 255, (byte) 0, (byte) 100, (byte) 0};
	private static final byte[] PTVIOLET = new byte[]{(byte) 255, (byte) 139, (byte) 0, (byte) 139};

	public MyKmlStyleFactory(KMZWriter writer, DocumentType document) {
		super(writer, document);
//		this.writer = writer;
		this.document = document;
	}
	
	public StyleType createCarNetworkLinkStyle() throws IOException {
		if (this.myCarNetworklinkStyle == null) {
			this.myCarNetworklinkStyle = this.kmlObjectFactory.createStyleType();
			this.myCarNetworklinkStyle.setId("createCarNetworkLinkStyle");

			LinkType iconLink = this.kmlObjectFactory.createLinkType();
			iconLink.setHref(DEFAULTLINKICON);
//			this.writer.addNonKMLFile(MatsimResource.getAsInputStream(DEFAULTNODEICONRESOURCE), DEFAULTLINKICON);
			IconStyleType iStyle = this.kmlObjectFactory.createIconStyleType();
			iStyle.setIcon(iconLink);
			iStyle.setColor(MatsimKmlStyleFactory.MATSIMWHITE);
			iStyle.setScale(MyKmlStyleFactory.ICONSCALE);
			this.myCarNetworklinkStyle.setIconStyle(iStyle);
			LineStyleType lineStyle = this.kmlObjectFactory.createLineStyleType();
			lineStyle.setColor(MyKmlStyleFactory.CAR);
			lineStyle.setWidth(Double.valueOf(12.0));
			this.myCarNetworklinkStyle.setLineStyle(lineStyle);
			this.document.getAbstractStyleSelectorGroup().add(this.kmlObjectFactory.createStyle(this.myCarNetworklinkStyle));
		}
		return this.myCarNetworklinkStyle;
	}
	
	public StyleType createWalkNetworkLinkStyle() throws IOException {
		if (this.myWalkNetworklinkStyle == null) {
			this.myWalkNetworklinkStyle = this.kmlObjectFactory.createStyleType();
			this.myWalkNetworklinkStyle.setId("createWalkNetworkLinkStyle");

			LinkType iconLink = this.kmlObjectFactory.createLinkType();
			iconLink.setHref(DEFAULTLINKICON);
//			this.writer.addNonKMLFile(MatsimResource.getAsInputStream(DEFAULTNODEICONRESOURCE), DEFAULTLINKICON);
			IconStyleType iStyle = this.kmlObjectFactory.createIconStyleType();
			iStyle.setIcon(iconLink);
			iStyle.setColor(MatsimKmlStyleFactory.MATSIMWHITE);
			iStyle.setScale(MyKmlStyleFactory.ICONSCALE);
			this.myWalkNetworklinkStyle.setIconStyle(iStyle);
			LineStyleType lineStyle = this.kmlObjectFactory.createLineStyleType();
			lineStyle.setColor(MyKmlStyleFactory.WALK);
			lineStyle.setWidth(Double.valueOf(12.0));
			this.myWalkNetworklinkStyle.setLineStyle(lineStyle);
			this.document.getAbstractStyleSelectorGroup().add(this.kmlObjectFactory.createStyle(this.myWalkNetworklinkStyle));
		}
		return this.myWalkNetworklinkStyle;
	}
	
	public StyleType createDBNetworkLinkStyle() throws IOException {
		if (this.myDBNetworklinkStyle == null) {
			this.myDBNetworklinkStyle = this.kmlObjectFactory.createStyleType();
			this.myDBNetworklinkStyle.setId("createDBNetworkLinkStyle");

			LinkType iconLink = this.kmlObjectFactory.createLinkType();
			iconLink.setHref(DEFAULTLINKICON);
//			this.writer.addNonKMLFile(MatsimResource.getAsInputStream(DEFAULTNODEICONRESOURCE), DEFAULTLINKICON);
			IconStyleType iStyle = this.kmlObjectFactory.createIconStyleType();
			iStyle.setIcon(iconLink);
			iStyle.setColor(MatsimKmlStyleFactory.MATSIMWHITE);
			iStyle.setScale(MyKmlStyleFactory.ICONSCALE);
			this.myDBNetworklinkStyle.setIconStyle(iStyle);
			LineStyleType lineStyle = this.kmlObjectFactory.createLineStyleType();
			lineStyle.setColor(MyKmlStyleFactory.PTRED);
			lineStyle.setWidth(Double.valueOf(12.0));
			this.myDBNetworklinkStyle.setLineStyle(lineStyle);
			this.document.getAbstractStyleSelectorGroup().add(this.kmlObjectFactory.createStyle(this.myDBNetworklinkStyle));
		}
		return this.myDBNetworklinkStyle;
	}
	
	public StyleType createMetroBusTramNetworkLinkStyle() throws IOException {
		if (this.myMetroBusTramNetworklinkStyle == null) {
			this.myMetroBusTramNetworklinkStyle = this.kmlObjectFactory.createStyleType();
			this.myMetroBusTramNetworklinkStyle.setId("createMetroBusTramNetworkLinkStyle");

			LinkType iconLink = this.kmlObjectFactory.createLinkType();
			iconLink.setHref(DEFAULTLINKICON);
//			this.writer.addNonKMLFile(MatsimResource.getAsInputStream(DEFAULTNODEICONRESOURCE), DEFAULTLINKICON);
			IconStyleType iStyle = this.kmlObjectFactory.createIconStyleType();
			iStyle.setIcon(iconLink);
			iStyle.setColor(MatsimKmlStyleFactory.MATSIMWHITE);
			iStyle.setScale(MyKmlStyleFactory.ICONSCALE);
			this.myMetroBusTramNetworklinkStyle.setIconStyle(iStyle);
			LineStyleType lineStyle = this.kmlObjectFactory.createLineStyleType();
			lineStyle.setColor(MyKmlStyleFactory.PTORANGE);
			lineStyle.setWidth(Double.valueOf(12.0));
			this.myMetroBusTramNetworklinkStyle.setLineStyle(lineStyle);
			this.document.getAbstractStyleSelectorGroup().add(this.kmlObjectFactory.createStyle(this.myMetroBusTramNetworklinkStyle));
		}
		return this.myMetroBusTramNetworklinkStyle;
	}
	
	public StyleType createBusNetworkLinkStyle() throws IOException {
		if (this.myBusNetworklinkStyle == null) {
			this.myBusNetworklinkStyle = this.kmlObjectFactory.createStyleType();
			this.myBusNetworklinkStyle.setId("createBusNetworkLinkStyle");

			LinkType iconLink = this.kmlObjectFactory.createLinkType();
			iconLink.setHref(DEFAULTLINKICON);
//			this.writer.addNonKMLFile(MatsimResource.getAsInputStream(DEFAULTNODEICONRESOURCE), DEFAULTLINKICON);
			IconStyleType iStyle = this.kmlObjectFactory.createIconStyleType();
			iStyle.setIcon(iconLink);
			iStyle.setColor(MatsimKmlStyleFactory.MATSIMWHITE);
			iStyle.setScale(MyKmlStyleFactory.ICONSCALE);
			this.myBusNetworklinkStyle.setIconStyle(iStyle);
			LineStyleType lineStyle = this.kmlObjectFactory.createLineStyleType();
			lineStyle.setColor(MyKmlStyleFactory.PTVIOLET);
			lineStyle.setWidth(Double.valueOf(12.0));
			this.myBusNetworklinkStyle.setLineStyle(lineStyle);
			this.document.getAbstractStyleSelectorGroup().add(this.kmlObjectFactory.createStyle(this.myBusNetworklinkStyle));
		}
		return this.myBusNetworklinkStyle;
	}
	
	public StyleType createTramNetworkLinkStyle() throws IOException {
		if (this.myTramNetworklinkStyle == null) {
			this.myTramNetworklinkStyle = this.kmlObjectFactory.createStyleType();
			this.myTramNetworklinkStyle.setId("createTramNetworkLinkStyle");

			LinkType iconLink = this.kmlObjectFactory.createLinkType();
			iconLink.setHref(DEFAULTLINKICON);
//			this.writer.addNonKMLFile(MatsimResource.getAsInputStream(DEFAULTNODEICONRESOURCE), DEFAULTLINKICON);
			IconStyleType iStyle = this.kmlObjectFactory.createIconStyleType();
			iStyle.setIcon(iconLink);
			iStyle.setColor(MatsimKmlStyleFactory.MATSIMWHITE);
			iStyle.setScale(MyKmlStyleFactory.ICONSCALE);
			this.myTramNetworklinkStyle.setIconStyle(iStyle);
			LineStyleType lineStyle = this.kmlObjectFactory.createLineStyleType();
			lineStyle.setColor(MyKmlStyleFactory.PTRED);
			lineStyle.setWidth(Double.valueOf(12.0));
			this.myTramNetworklinkStyle.setLineStyle(lineStyle);
			this.document.getAbstractStyleSelectorGroup().add(this.kmlObjectFactory.createStyle(this.myTramNetworklinkStyle));
		}
		return this.myTramNetworklinkStyle;
	}
	
	public StyleType createSubWayNetworkLinkStyle() throws IOException {
		if (this.mySubWayNetworklinkStyle == null) {
			this.mySubWayNetworklinkStyle = this.kmlObjectFactory.createStyleType();
			this.mySubWayNetworklinkStyle.setId("createSubWayNetworkLinkStyle");

			LinkType iconLink = this.kmlObjectFactory.createLinkType();
			iconLink.setHref(DEFAULTLINKICON);
//			this.writer.addNonKMLFile(MatsimResource.getAsInputStream(DEFAULTNODEICONRESOURCE), DEFAULTLINKICON);
			IconStyleType iStyle = this.kmlObjectFactory.createIconStyleType();
			iStyle.setIcon(iconLink);
			iStyle.setColor(MatsimKmlStyleFactory.MATSIMWHITE);
			iStyle.setScale(MyKmlStyleFactory.ICONSCALE);
			this.mySubWayNetworklinkStyle.setIconStyle(iStyle);
			LineStyleType lineStyle = this.kmlObjectFactory.createLineStyleType();
			lineStyle.setColor(MyKmlStyleFactory.PTBLUE);
			lineStyle.setWidth(Double.valueOf(12.0));
			this.mySubWayNetworklinkStyle.setLineStyle(lineStyle);
			this.document.getAbstractStyleSelectorGroup().add(this.kmlObjectFactory.createStyle(this.mySubWayNetworklinkStyle));
		}
		return this.mySubWayNetworklinkStyle;
	}
	
	public StyleType createSBAHNNetworkLinkStyle() throws IOException {
		if (this.mySBAHNNetworklinkStyle == null) {
			this.mySBAHNNetworklinkStyle = this.kmlObjectFactory.createStyleType();
			this.mySBAHNNetworklinkStyle.setId("createSBAHNNetworkLinkStyle");

			LinkType iconLink = this.kmlObjectFactory.createLinkType();
			iconLink.setHref(DEFAULTLINKICON);
//			this.writer.addNonKMLFile(MatsimResource.getAsInputStream(DEFAULTNODEICONRESOURCE), DEFAULTLINKICON);
			IconStyleType iStyle = this.kmlObjectFactory.createIconStyleType();
			iStyle.setIcon(iconLink);
			iStyle.setColor(MatsimKmlStyleFactory.MATSIMWHITE);
			iStyle.setScale(MyKmlStyleFactory.ICONSCALE);
			this.mySBAHNNetworklinkStyle.setIconStyle(iStyle);
			LineStyleType lineStyle = this.kmlObjectFactory.createLineStyleType();
			lineStyle.setColor(MyKmlStyleFactory.PTGREEN);
			lineStyle.setWidth(Double.valueOf(12.0));
			this.mySBAHNNetworklinkStyle.setLineStyle(lineStyle);
			this.document.getAbstractStyleSelectorGroup().add(this.kmlObjectFactory.createStyle(this.mySBAHNNetworklinkStyle));
		}
		return this.mySBAHNNetworklinkStyle;
	}
	

}
