/* *********************************************************************** *
 * project: org.matsim.*
 * LinkSetRenderer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.yu.test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import org.matsim.vis.netvis.DisplayableLinkI;
import org.matsim.vis.netvis.DrawableAgentI;
import org.matsim.vis.netvis.VisConfig;
import org.matsim.vis.netvis.gui.NetJComponent;
import org.matsim.vis.netvis.renderers.RendererA;
import org.matsim.vis.netvis.renderers.ValueColorizer;
import org.matsim.vis.netvis.visNet.DisplayLink;
import org.matsim.vis.netvis.visNet.DisplayNet;

public class LinkSetRenderer extends RendererA {

	private static final boolean RANDOMIZE_LANES = false;

	private static final boolean RENDER_CELL_CONTOURS = true;

	private final ValueColorizer colorizer = new ValueColorizer();

	private final DisplayNet network;

	private double laneWidth;

	public LinkSetRenderer(final VisConfig visConfig, final DisplayNet network) {
		super(visConfig);
		this.network = network;

		laneWidth = DisplayLink.LANE_WIDTH * visConfig.getLinkWidthFactor();
	}

	@Override
	public void setTargetComponent(final NetJComponent comp) {
		super.setTargetComponent(comp);
	}

	// -------------------- RENDERING --------------------

	@Override
	protected synchronized void myRendering(final Graphics2D display,
			final AffineTransform boxTransform) {
		String test = getVisConfig().get("ShowAgents");
		boolean drawAgents = test == null || test.equals("true");
		laneWidth = DisplayLink.LANE_WIDTH
				* getVisConfig().getLinkWidthFactor();

		NetJComponent comp = getNetJComponent();

		AffineTransform originalTransform = display.getTransform();

		display.setStroke(new BasicStroke(Math.round(0.05 * laneWidth)));

		for (DisplayableLinkI link : network.getLinks().values()) {
			if (!comp.checkLineInClip(link.getStartEasting(), link
					.getStartNorthing(), link.getEndEasting(), link
					.getEndNorthing()))
				continue;

			if (link.getStartEasting() == link.getEndEasting()
					&& link.getStartNorthing() == link.getEndNorthing())
				continue;

			AffineTransform linkTransform = new AffineTransform(
					originalTransform);
			linkTransform.concatenate(boxTransform);
			linkTransform.concatenate(link.getLinear2PlaneTransform());

			/*
			 * (1) RENDER LINK
			 */

			display.setTransform(linkTransform);

			final int lanes = link
					.getLanesAsInt(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
			final int cellLength_m = (int) Math.round(link.getLength_m()
					/ link.getDisplayValueCount());
			final int cellWidth_m = (int) Math.round(laneWidth * lanes);
			int cellStart_m = 0;

			for (int i = 0; i < link.getDisplayValueCount(); i++) {

				display.setColor(colorizer.getColor(link.getDisplayValue(i)));

				display.fillRect(cellStart_m, -cellWidth_m, cellLength_m,
						cellWidth_m);

				if (RENDER_CELL_CONTOURS) {
					display.setColor(Color.BLACK);
					int linkId = Integer.parseInt(link.getId().toString());
					switch (linkId) {
					case 106727:
						display.setColor(Color.GREEN);
					case 111575:
						display.setColor(Color.GREEN);
					case 110650:
						display.setColor(Color.GREEN);
					case 106730:
						display.setColor(Color.GREEN);
					case 110916:
						display.setColor(Color.GREEN);
					case 111546:
						display.setColor(Color.GREEN);
					case 111580:
						display.setColor(Color.GREEN);
					case 111654:
						display.setColor(Color.GREEN);
					case 110654:
						display.setColor(Color.GREEN);
					case 110604:
						display.setColor(Color.GREEN);
					case 106110:
						display.setColor(Color.GREEN);
					case 111540:
						display.setColor(Color.GREEN);
					case 108054:
						display.setColor(Color.GREEN);
					case 108051:
						display.setColor(Color.GREEN);
					case 111620:
						display.setColor(Color.GREEN);
					case 111533:
						display.setColor(Color.GREEN);
					case 111604:
						display.setColor(Color.GREEN);
					case 111565:
						display.setColor(Color.GREEN);
					case 111600:
						display.setColor(Color.GREEN);
					case 111673:
						display.setColor(Color.GREEN);
					case 106474:
						display.setColor(Color.GREEN);
					case 106111:
						display.setColor(Color.GREEN);
					case 111633:
						display.setColor(Color.GREEN);
					case 111040:
						display.setColor(Color.GREEN);
					case 106130:
						display.setColor(Color.GREEN);
					case 111517:
						display.setColor(Color.GREEN);
					case 106759:
						display.setColor(Color.GREEN);
					case 111566:
						display.setColor(Color.GREEN);
					case 111655:
						display.setColor(Color.GREEN);
					case 110576:
						display.setColor(Color.GREEN);
					case 111619:
						display.setColor(Color.GREEN);
					case 111627:
						display.setColor(Color.GREEN);
					case 111656:
						display.setColor(Color.GREEN);
					case 110918:
						display.setColor(Color.GREEN);
					case 110583:
						display.setColor(Color.GREEN);
					case 110563:
						display.setColor(Color.GREEN);
					case 105660:
						display.setColor(Color.GREEN);
					case 105693:
						display.setColor(Color.GREEN);
					case 110459:
						display.setColor(Color.GREEN);
					case 108026:
						display.setColor(Color.GREEN);
					case 110562:
						display.setColor(Color.GREEN);
					case 110657:
						display.setColor(Color.GREEN);
					case 110912:
						display.setColor(Color.GREEN);
					case 111609:
						display.setColor(Color.GREEN);
					case 111558:
						display.setColor(Color.GREEN);
					case 106956:
						display.setColor(Color.GREEN);
					case 106468:
						display.setColor(Color.GREEN);
					case 111515:
						display.setColor(Color.GREEN);
					case 111552:
						display.setColor(Color.GREEN);
					case 111574:
						display.setColor(Color.GREEN);
					case 111539:
						display.setColor(Color.GREEN);
					case 108057:
						display.setColor(Color.GREEN);
					case 108031:
						display.setColor(Color.GREEN);
					case 106476:
						display.setColor(Color.GREEN);
					case 111532:
						display.setColor(Color.GREEN);
					case 111665:
						display.setColor(Color.GREEN);
					case 111647:
						display.setColor(Color.GREEN);
					case 111042:
						display.setColor(Color.GREEN);
					case 111516:
						display.setColor(Color.GREEN);
					case 110660:
						display.setColor(Color.GREEN);
					case 111628:
						display.setColor(Color.GREEN);
					case 105655:
						display.setColor(Color.GREEN);
					case 106105:
						display.setColor(Color.GREEN);
					case 107165:
						display.setColor(Color.GREEN);
					case 103326:
						display.setColor(Color.BLUE);
					case 106115:
						display.setColor(Color.GREEN);
					case 111611:
						display.setColor(Color.GREEN);
					case 111661:
						display.setColor(Color.GREEN);
					case 111605:
						display.setColor(Color.GREEN);
					case 101566:
						display.setColor(Color.RED);
					case 110909:
						display.setColor(Color.GREEN);
					case 111660:
						display.setColor(Color.GREEN);
					case 106067:
						display.setColor(Color.GREEN);
					case 106462:
						display.setColor(Color.GREEN);
					case 111679:
						display.setColor(Color.GREEN);
					case 106123:
						display.setColor(Color.GREEN);
					case 111688:
						display.setColor(Color.GREEN);
					case 110579:
						display.setColor(Color.GREEN);
					case 111497:
						display.setColor(Color.GREEN);
					case 103325:
						display.setColor(Color.BLUE);
					case 106306:
						display.setColor(Color.GREEN);
					case 110554:
						display.setColor(Color.GREEN);
					case 106118:
						display.setColor(Color.GREEN);
					case 108025:
						display.setColor(Color.GREEN);
					case 111509:
						display.setColor(Color.GREEN);
					case 110893:
						display.setColor(Color.GREEN);
					case 111495:
						display.setColor(Color.GREEN);
					case 111514:
						display.setColor(Color.GREEN);
					case 111508:
						display.setColor(Color.GREEN);
					case 106120:
						display.setColor(Color.GREEN);
					case 110572:
						display.setColor(Color.GREEN);
					case 103771:
						display.setColor(Color.ORANGE);
					case 111625:
						display.setColor(Color.GREEN);
					case 111506:
						display.setColor(Color.GREEN);
					case 111590:
						display.setColor(Color.GREEN);
					case 111527:
						display.setColor(Color.GREEN);
					case 111684:
						display.setColor(Color.GREEN);
					case 111591:
						display.setColor(Color.GREEN);
					case 111584:
						display.setColor(Color.GREEN);
					case 106070:
						display.setColor(Color.GREEN);
					case 106464:
						display.setColor(Color.GREEN);
					case 111535:
						display.setColor(Color.GREEN);
					case 106127:
						display.setColor(Color.GREEN);
					case 106760:
						display.setColor(Color.GREEN);
					case 106733:
						display.setColor(Color.GREEN);
					case 111520:
						display.setColor(Color.GREEN);
					case 111571:
						display.setColor(Color.GREEN);
					case 111637:
						display.setColor(Color.GREEN);
					case 111564:
						display.setColor(Color.GREEN);
					case 110557:
						display.setColor(Color.GREEN);
					case 101564:
						display.setColor(Color.RED);
					case 111555:
						display.setColor(Color.GREEN);
					case 110895:
						display.setColor(Color.BLUE);
					case 110559:
						display.setColor(Color.GREEN);
					case 110915:
						display.setColor(Color.GREEN);
					case 110584:
						display.setColor(Color.GREEN);
					case 108045:
						display.setColor(Color.GREEN);
					case 106122:
						display.setColor(Color.GREEN);
					case 110910:
						display.setColor(Color.GREEN);
					case 110453:
						display.setColor(Color.GREEN);
					case 111617:
						display.setColor(Color.BLUE);
					case 111646:
						display.setColor(Color.GREEN);
					case 111531:
						display.setColor(Color.GREEN);
					case 108039:
						display.setColor(Color.GREEN);
					case 111507:
						display.setColor(Color.GREEN);
					case 110904:
						display.setColor(Color.GREEN);
					case 111526:
						display.setColor(Color.GREEN);
					case 111567:
						display.setColor(Color.GREEN);
					case 111658:
						display.setColor(Color.GREEN);
					case 111498:
						display.setColor(Color.GREEN);
					case 110564:
						display.setColor(Color.GREEN);
					case 111599:
						display.setColor(Color.GREEN);
					case 111631:
						display.setColor(Color.GREEN);
					case 105656:
						display.setColor(Color.GREEN);
					case 111639:
						display.setColor(Color.GREEN);
					case 106026:
						display.setColor(Color.GREEN);
					case 106736:
						display.setColor(Color.GREEN);
					case 110773:
						display.setColor(Color.GREEN);
					case 108038:
						display.setColor(Color.GREEN);
					case 110592:
						display.setColor(Color.GREEN);
					case 107163:
						display.setColor(Color.GREEN);
					case 111510:
						display.setColor(Color.GREEN);
					case 111606:
						display.setColor(Color.GREEN);
					case 106724:
						display.setColor(Color.GREEN);
					case 101563:
						display.setColor(Color.RED);
					case 110553:
						display.setColor(Color.GREEN);
					case 110891:
						display.setColor(Color.GREEN);
					case 111521:
						display.setColor(Color.GREEN);
					case 111505:
						display.setColor(Color.GREEN);
					case 108041:
						display.setColor(Color.GREEN);
					case 106124:
						display.setColor(Color.GREEN);
					case 110578:
						display.setColor(Color.GREEN);
					case 110561:
						display.setColor(Color.GREEN);
					case 111041:
						display.setColor(Color.GREEN);
					case 106025:
						display.setColor(Color.GREEN);
					case 108046:
						display.setColor(Color.GREEN);
					case 106114:
						display.setColor(Color.GREEN);
					case 110581:
						display.setColor(Color.GREEN);
					case 106119:
						display.setColor(Color.GREEN);
					case 111501:
						display.setColor(Color.GREEN);
					case 111039:
						display.setColor(Color.GREEN);
					case 111568:
						display.setColor(Color.GREEN);
					case 111496:
						display.setColor(Color.GREEN);
					case 111597:
						display.setColor(Color.GREEN);
					case 110577:
						display.setColor(Color.GREEN);
					case 106103:
						display.setColor(Color.GREEN);
					case 106469:
						display.setColor(Color.GREEN);
					case 111616:
						display.setColor(Color.GREEN);
					case 106106:
						display.setColor(Color.GREEN);
					case 106459:
						display.setColor(Color.GREEN);
					case 111542:
						display.setColor(Color.GREEN);
					case 111581:
						display.setColor(Color.GREEN);
					case 111538:
						display.setColor(Color.GREEN);
					case 111624:
						display.setColor(Color.GREEN);
					case 108048:
						display.setColor(Color.GREEN);
					case 105689:
						display.setColor(Color.GREEN);
					case 111687:
						display.setColor(Color.GREEN);
					case 106958:
						display.setColor(Color.GREEN);
					case 111615:
						display.setColor(Color.GREEN);
					case 111659:
						display.setColor(Color.GREEN);
					case 110653:
						display.setColor(Color.GREEN);
					case 111608:
						display.setColor(Color.GREEN);
					case 111543:
						display.setColor(Color.GREEN);
					case 111524:
						display.setColor(Color.GREEN);
					case 110558:
						display.setColor(Color.GREEN);
					case 106128:
						display.setColor(Color.GREEN);
					case 111614:
						display.setColor(Color.GREEN);
					case 110914:
						display.setColor(Color.GREEN);
					case 110651:
						display.setColor(Color.GREEN);
					case 106116:
						display.setColor(Color.GREEN);
					case 111680:
						display.setColor(Color.GREEN);
					case 111528:
						display.setColor(Color.GREEN);
					case 111504:
						display.setColor(Color.GREEN);
					case 111576:
						display.setColor(Color.GREEN);
					case 106758:
						display.setColor(Color.GREEN);
					case 111649:
						display.setColor(Color.GREEN);
					case 111503:
						display.setColor(Color.GREEN);
					case 106068:
						display.setColor(Color.GREEN);
					case 111494:
						display.setColor(Color.GREEN);
					case 108032:
						display.setColor(Color.GREEN);
					case 108043:
						display.setColor(Color.GREEN);
					case 111641:
						display.setColor(Color.GREEN);
					case 110917:
						display.setColor(Color.GREEN);
					case 111626:
						display.setColor(Color.GREEN);
					case 111492:
						display.setColor(Color.GREEN);
					case 111596:
						display.setColor(Color.GREEN);
					case 110571:
						display.setColor(Color.GREEN);
					case 111669:
						display.setColor(Color.GREEN);
					case 105657:
						display.setColor(Color.GREEN);
					case 111636:
						display.setColor(Color.GREEN);
					case 106465:
						display.setColor(Color.GREEN);
					case 106467:
						display.setColor(Color.GREEN);
					case 105692:
						display.setColor(Color.GREEN);
					case 111559:
						display.setColor(Color.GREEN);
					case 111623:
						display.setColor(Color.GREEN);
					case 111621:
						display.setColor(Color.GREEN);
					case 111653:
						display.setColor(Color.GREEN);
					case 106460:
						display.setColor(Color.GREEN);
					case 111601:
						display.setColor(Color.GREEN);
					case 110471:
						display.setColor(Color.GREEN);
					case 106732:
						display.setColor(Color.GREEN);
					case 110913:
						display.setColor(Color.GREEN);
					case 111594:
						display.setColor(Color.GREEN);
					case 108040:
						display.setColor(Color.GREEN);
					case 108027:
						display.setColor(Color.GREEN);
					case 111544:
						display.setColor(Color.GREEN);
					case 106726:
						display.setColor(Color.GREEN);
					case 111536:
						display.setColor(Color.GREEN);
					case 111662:
						display.setColor(Color.GREEN);
					case 106723:
						display.setColor(Color.GREEN);
					case 111573:
						display.setColor(Color.GREEN);
					case 110460:
						display.setColor(Color.GREEN);
					case 111534:
						display.setColor(Color.GREEN);
					case 106132:
						display.setColor(Color.GREEN);
					case 111683:
						display.setColor(Color.GREEN);
					case 106129:
						display.setColor(Color.GREEN);
					case 111676:
						display.setColor(Color.GREEN);
					case 111583:
						display.setColor(Color.GREEN);
					case 106470:
						display.setColor(Color.GREEN);
					case 111493:
						display.setColor(Color.GREEN);
					case 111648:
						display.setColor(Color.GREEN);
					case 111634:
						display.setColor(Color.GREEN);
					case 111678:
						display.setColor(Color.GREEN);
					case 111644:
						display.setColor(Color.GREEN);
					case 111651:
						display.setColor(Color.GREEN);
					case 108029:
						display.setColor(Color.GREEN);
					case 111577:
						display.setColor(Color.GREEN);
					case 106104:
						display.setColor(Color.GREEN);
					case 111523:
						display.setColor(Color.GREEN);
					case 111675:
						display.setColor(Color.GREEN);
					case 111525:
						display.setColor(Color.GREEN);
					case 111607:
						display.setColor(Color.GREEN);
					case 111553:
						display.setColor(Color.GREEN);
					case 106731:
						display.setColor(Color.GREEN);
					case 111499:
						display.setColor(Color.GREEN);
					case 106722:
						display.setColor(Color.GREEN);
					case 106957:
						display.setColor(Color.GREEN);
					case 111612:
						display.setColor(Color.GREEN);
					case 106108:
						display.setColor(Color.GREEN);
					case 108055:
						display.setColor(Color.GREEN);
					case 106107:
						display.setColor(Color.GREEN);
					case 106458:
						display.setColor(Color.GREEN);
					case 111595:
						display.setColor(Color.GREEN);
					case 109725:
						display.setColor(Color.GREEN);
					case 108037:
						display.setColor(Color.GREEN);
					case 106069:
						display.setColor(Color.GREEN);
					case 106735:
						display.setColor(Color.GREEN);
					case 106953:
						display.setColor(Color.GREEN);
					case 110582:
						display.setColor(Color.GREEN);
					case 110659:
						display.setColor(Color.GREEN);
					case 111556:
						display.setColor(Color.GREEN);
					case 111549:
						display.setColor(Color.GREEN);
					case 108053:
						display.setColor(Color.GREEN);
					case 111557:
						display.setColor(Color.GREEN);
					case 110894:
						display.setColor(Color.GREEN);
					case 111667:
						display.setColor(Color.GREEN);
					case 111529:
						display.setColor(Color.GREEN);
					case 111554:
						display.setColor(Color.GREEN);
					case 111632:
						display.setColor(Color.GREEN);
					case 111650:
						display.setColor(Color.GREEN);
					case 106955:
						display.setColor(Color.GREEN);
					case 105659:
						display.setColor(Color.GREEN);
					case 111588:
						display.setColor(Color.GREEN);
					case 110573:
						display.setColor(Color.GREEN);
					case 111664:
						display.setColor(Color.GREEN);
					case 111550:
						display.setColor(Color.GREEN);
					case 111638:
						display.setColor(Color.GREEN);
					case 105658:
						display.setColor(Color.GREEN);
					case 106466:
						display.setColor(Color.GREEN);
					case 111512:
						display.setColor(Color.GREEN);
					case 111629:
						display.setColor(Color.GREEN);
					case 106738:
						display.setColor(Color.GREEN);
					case 106457:
						display.setColor(Color.GREEN);
					case 111603:
						display.setColor(Color.GREEN);
					case 106728:
						display.setColor(Color.GREEN);
					case 107164:
						display.setColor(Color.GREEN);
					case 106729:
						display.setColor(Color.GREEN);
					case 111572:
						display.setColor(Color.GREEN);
					case 111530:
						display.setColor(Color.GREEN);
					case 111502:
						display.setColor(Color.GREEN);
					case 111652:
						display.setColor(Color.GREEN);
					case 108047:
						display.setColor(Color.GREEN);
					case 110560:
						display.setColor(Color.GREEN);
					case 111672:
						display.setColor(Color.GREEN);
					case 106304:
						display.setColor(Color.GREEN);
					case 111579:
						display.setColor(Color.GREEN);
					case 111491:
						display.setColor(Color.GREEN);
					case 110903:
						display.setColor(Color.GREEN);
					case 106305:
						display.setColor(Color.GREEN);
					case 106475:
						display.setColor(Color.GREEN);
					case 111569:
						display.setColor(Color.GREEN);
					case 106112:
						display.setColor(Color.GREEN);
					case 110892:
						display.setColor(Color.GREEN);
					case 105690:
						display.setColor(Color.GREEN);
					case 111668:
						display.setColor(Color.GREEN);
					case 108052:
						display.setColor(Color.GREEN);
					case 111670:
						display.setColor(Color.GREEN);
					case 111560:
						display.setColor(Color.GREEN);
					case 106461:
						display.setColor(Color.GREEN);
					case 110575:
						display.setColor(Color.GREEN);
					case 106109:
						display.setColor(Color.GREEN);
					case 110574:
						display.setColor(Color.GREEN);
					case 109726:
						display.setColor(Color.GREEN);
					case 106463:
						display.setColor(Color.GREEN);
					case 111561:
						display.setColor(Color.GREEN);
					case 108044:
						display.setColor(Color.GREEN);
					case 111562:
						display.setColor(Color.GREEN);
					case 106121:
						display.setColor(Color.GREEN);
					case 106472:
						display.setColor(Color.GREEN);
					case 107166:
						display.setColor(Color.GREEN);
					case 110655:
						display.setColor(Color.GREEN);
					case 106131:
						display.setColor(Color.GREEN);
					case 111547:
						display.setColor(Color.GREEN);
					case 110896:
						display.setColor(Color.BLUE);
					case 111630:
						display.setColor(Color.GREEN);
					case 110580:
						display.setColor(Color.GREEN);
					case 110551:
						display.setColor(Color.GREEN);
					case 111642:
						display.setColor(Color.GREEN);
					case 110901:
						display.setColor(Color.GREEN);
					case 111657:
						display.setColor(Color.GREEN);
					case 106737:
						display.setColor(Color.GREEN);
					case 108028:
						display.setColor(Color.GREEN);
					case 103772:
						display.setColor(Color.ORANGE);
					case 111585:
						display.setColor(Color.GREEN);
					case 111587:
						display.setColor(Color.GREEN);
					case 111671:
						display.setColor(Color.GREEN);
					case 106725:
						display.setColor(Color.GREEN);
					case 110652:
						display.setColor(Color.GREEN);
					case 111582:
						display.setColor(Color.GREEN);
					case 111513:
						display.setColor(Color.GREEN);
					case 111519:
						display.setColor(Color.GREEN);
					case 111592:
						display.setColor(Color.GREEN);
					case 108030:
						display.setColor(Color.GREEN);
					case 106721:
						display.setColor(Color.GREEN);
					case 110603:
						display.setColor(Color.GREEN);
					case 110552:
						display.setColor(Color.GREEN);
					case 111586:
						display.setColor(Color.GREEN);
					case 110591:
						display.setColor(Color.GREEN);
					case 108056:
						display.setColor(Color.GREEN);
					case 110902:
						display.setColor(Color.GREEN);
					case 111645:
						display.setColor(Color.GREEN);
					case 111541:
						display.setColor(Color.GREEN);
					case 111589:
						display.setColor(Color.GREEN);
					case 111551:
						display.setColor(Color.GREEN);
					case 111518:
						display.setColor(Color.GREEN);
					case 110911:
						display.setColor(Color.GREEN);
					case 106117:
						display.setColor(Color.GREEN);
					case 106303:
						display.setColor(Color.GREEN);
					case 111548:
						display.setColor(Color.GREEN);
					case 111663:
						display.setColor(Color.GREEN);
					case 110656:
						display.setColor(Color.GREEN);
					case 110772:
						display.setColor(Color.GREEN);
					case 106954:
						display.setColor(Color.GREEN);
					case 111643:
						display.setColor(Color.GREEN);
					case 106473:
						display.setColor(Color.GREEN);
					case 111500:
						display.setColor(Color.GREEN);
					case 111598:
						display.setColor(Color.GREEN);
					case 111570:
						display.setColor(Color.GREEN);
					case 105694:
						display.setColor(Color.BLUE);
					case 106757:
						display.setColor(Color.GREEN);
					case 108058:
						display.setColor(Color.GREEN);
					case 110658:
						display.setColor(Color.GREEN);
					case 111563:
						display.setColor(Color.GREEN);
					case 108050:
						display.setColor(Color.GREEN);
					case 106471:
						display.setColor(Color.GREEN);
					case 111677:
						display.setColor(Color.GREEN);
					case 108042:
						display.setColor(Color.GREEN);
					case 111674:
						display.setColor(Color.GREEN);
					case 111043:
						display.setColor(Color.GREEN);
					case 111578:
						display.setColor(Color.GREEN);
					case 111537:
						display.setColor(Color.GREEN);
					case 111613:
						display.setColor(Color.GREEN);
					case 101565:
						display.setColor(Color.RED);
					case 111635:
						display.setColor(Color.GREEN);
					case 111511:
						display.setColor(Color.GREEN);
					case 110472:
						display.setColor(Color.GREEN);
					case 111602:
						display.setColor(Color.GREEN);
					case 110649:
						display.setColor(Color.GREEN);
					case 111610:
						display.setColor(Color.GREEN);
					case 111622:
						display.setColor(Color.GREEN);
					case 111522:
						display.setColor(Color.GREEN);
					case 111044:
						display.setColor(Color.GREEN);
					case 111640:
						display.setColor(Color.GREEN);
					case 110774:
						display.setColor(Color.GREEN);
					case 110771:
						display.setColor(Color.GREEN);
					case 111593:
						display.setColor(Color.GREEN);
					case 105691:
						display.setColor(Color.GREEN);
					case 111545:
						display.setColor(Color.GREEN);
					case 111618:
						display.setColor(Color.BLUE);
					case 108049:
						display.setColor(Color.GREEN);
					case 111666:
						display.setColor(Color.GREEN);
					case 110454:
						display.setColor(Color.GREEN);
					case 106113:
						display.setColor(Color.GREEN);
					case 106734:
						display.setColor(Color.GREEN);
					}
					display.drawRect(cellStart_m, -cellWidth_m, cellLength_m,
							cellWidth_m);
				}

				cellStart_m += cellLength_m;
			}

			display.setColor(Color.BLACK);
			display.drawRect(0, -cellWidth_m, cellLength_m
					* link.getDisplayValueCount(), cellWidth_m);

			/*
			 * (2) RENDER VEHICLES
			 * 
			 * IMPORTANT: If you modify this, ensure proper rendering of agents
			 * on multi-lane links!
			 */

			final double agentWidth = laneWidth;
			final double agentLength = agentWidth;

			final boolean flip = link.getStartEasting() <= link.getEndEasting();

			if (flip) {
				AffineTransform flipTransform = AffineTransform
						.getScaleInstance(1, -1);
				linkTransform.concatenate(flipTransform);
				display.setTransform(linkTransform);
			}

			if (link.getMovingAgents() != null && drawAgents)
				for (DrawableAgentI agent : link.getMovingAgents()) {
					final int lane = RANDOMIZE_LANES ? agent.hashCode() % lanes
							+ 1 : agent.getLane();

					final int x = (int) Math.round(agent.getPosInLink_m() - 0.5
							* agentLength);

					final int y = (int) Math.round(agentWidth
							* ((flip ? lanes : 0) - lane));

					display.setColor(Color.BLUE);
					display.fillOval(x, y, (int) Math.round(agentLength),
							(int) Math.round(agentWidth));
				}
		}

		display.setTransform(originalTransform);
	}
}
