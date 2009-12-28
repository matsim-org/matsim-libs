package playground.gregor.snapshots.postprocessors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.evacuation.otfvis.drawer.OTFBackgroundTexturesDrawer;


public class WrongDirectionArrowsFromEvents extends ConfluenceArrowsFromEvents{



	private final OTFBackgroundTexturesDrawer wrongDir;

	public WrongDirectionArrowsFromEvents(OTFBackgroundTexturesDrawer arrows, OTFBackgroundTexturesDrawer wrongDir,
			NetworkLayer network) {
		super(arrows, network);
		this.wrongDir = wrongDir;
	}

	@Override
	public void createArrows() {
		for (NodeInfo ni : this.infos.values()) {
			if (ni.outLinks.size() < ni.node.getOutLinks().size()) {
				for (Link l : ni.node.getOutLinks().values()) {
					if (ni.outLinks.contains(l)) {
						continue;
					}
					double xDiff = l.getToNode().getCoord().getX() - l.getFromNode().getCoord().getX();
					CoordImpl f = (CoordImpl) l.getFromNode().getCoord();
					CoordImpl t = (CoordImpl) l.getToNode().getCoord();
					double euclLength = f.calcDistance(t);


					double cangle = xDiff / euclLength;
					double angle;
					if (l.getToNode().getCoord().getY() > l.getFromNode().getCoord().getY() ) {
						angle = Math.acos(cangle);

					} else {
						angle = 2*Math.PI - Math.acos(cangle);
					}
					
					
					double theta = 0.0;
					double dx = f.getX() - t.getX();
					double dy = f.getY() - t.getY();
					if (dx > 0) {
						theta = Math.atan(dy/dx);
					} else if (dx < 0) {
						theta = Math.PI + Math.atan(dy/dx);
					} else { // i.e. DX==0
						if (dy > 0) {
							theta = PI_HALF;
						} else {
							theta = -PI_HALF;
						}
					}
					if (theta < 0.0) theta += TWO_PI;
					
					Coord c = new CoordImpl(l.getCoord().getX() + Math.sin(theta) * 10,l.getCoord().getY() - Math.cos(theta)*10);
					
					double effLinkLength = Math.min(l.getLength(),100);
					this.sbg.addLocation(c, angle, 2*effLinkLength/3);
					this.wrongDir.addLocation(c, 0., 2*effLinkLength/3);
				}
			}

		}
	}



}
