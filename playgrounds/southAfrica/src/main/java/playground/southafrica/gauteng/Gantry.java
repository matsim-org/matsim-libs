/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.gauteng;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * Class to provide basic descriptive information about the toll gantries that
 * are part of the Gauteng Freeway Improvement Project (GFIP) network.
 * 
 * @author jwjoubert
 */
public enum Gantry {
	TG001(new Coord(28.274520670000257,-25.756189669999923), "Barbet"),
	TG002(new Coord(28.253994640000236,-25.818788859999895), "Mossie"),
	TG003(new Coord(28.230316750000213,-25.844374309999935), "Indlazi"),
	TG004(new Coord(28.179848560000174,-25.873221829999935), "Pikoko"),
	TG005(new Coord(28.16320606000016,-25.897896), "Ivuzi"),
	TG006(new Coord(28.140157560000141,-25.939241639999977), "Flamingo"),
	TG007(new Coord(28.12915617000014,-25.9702457799999), "Ihobe"),
	TG008(new Coord(28.10756050000013,-26.036566939999908), "Sunbird"),
	TG009(new Coord(28.076819250000106,-26.040742719999955), "Tarentaal"),
	TG010(new Coord(28.044290310000093,-26.036525029999954), "Blouvalk"),
	TG011(new Coord(27.98557081000007,-26.059504469999965), "Owl"),
	TG012(new Coord(27.967684,-26.092319279999987), "Pelican"),
	TG013(new Coord(27.938396780000055,-26.131404720000035), "Kingfisher"),
	TG014(new Coord(27.945273670000059,-26.176390750000031), "Ukhozi"),
	TG015(new Coord(27.952426280000058,-26.198386749999987), "Fiscal"),
	TG016(new Coord(27.961606420000063,-26.238868829999976), "Stork"),
	TG017(new Coord(27.949918610000058,-26.268148329999967), "Ilowe"),
	TG018(new Coord(28.107394780000128,-26.062048189999988), "Leeba"),
	TG019(new Coord(28.11717314000013,-26.09252828), "Ibis"),
	TG020(new Coord(28.132589970000147,-26.115697109999946), "Kiewiet"),
	TG021(new Coord(28.132299330000144,-26.152464889999987), "Kwikkie"),
	TG022(new Coord(28.134016780000149,-26.187469309999965), "Starling"),
	TG023(new Coord(28.127876060000141,-26.222170859999959), "Rooivink"),
	TG024(new Coord(28.134995390000149,-26.244848499999939), "Mpshe"),
	TG025(new Coord(28.149439750000163,-26.27344972), "Oxpecker"),
	TG028(new Coord(27.975688420000068,-26.26250642), "Phakwe"),
	TG029(new Coord(28.049400140000099,-26.269552579999949), "Thatha"),
	TG030(new Coord(28.073173890000113,-26.266712279999943), "Lenong"),
	TG031(new Coord(28.114550530000134,-26.25878039), "Lekgwaba"),
	TG032(new Coord(28.140836500000152,-26.1670095799999), "Loerie"),
	TG033(new Coord(28.220391030000215,-26.177556169999914), "Gull"),
	TG034(new Coord(28.260043940000259,-26.172891029999874), "Ilanda"),
	TG035(new Coord(28.295870330000298,-26.178230919999823), "Bee-eater"),
	TG037(new Coord(28.238976580000219,-25.820282719999934), "Hadeda"),
	TG038(new Coord(28.251378890000233,-25.851693889999911), "Ntsu"),
	TG039(new Coord(28.26150378000025,-25.922589499999887), "Heron"),
	TG040(new Coord(28.256920860000246,-25.952959809999911), "Blue Crane"),
	TG041(new Coord(28.27510650000027,-26.042928139999912), "Swael"),
	TG042(new Coord(28.249790750000237,-25.996703139999909), "Letata"),
	TG043(new Coord(28.219606390000216,-26.144710779999915), "Swan"),
	TG044(new Coord(28.227255060000225,-26.162565939999887), "Weaver"),
	TG045(new Coord(28.228864000000225,-26.128579169999927), "Hornbill"),
//	TG047(new Coord(28.389081367565847,-26.167026894864062), "Ugaga"),
//	TG048(new Coord(28.295633652408572,-25.749922518915263), "Inkovu"),
//	TG049(new Coord(28.345920974455389,-25.756285279725059), "Penguin"),
	;
	
	private Coord coord;
	private String birdName;
	
	private Gantry(Coord coord, String name) {
		this.coord = coord;
		this.birdName = name;
	}
	
	/**
	 * Coordinate of the gantry location given in the World Geodesic System 1984
	 * (WGS84) coordinate reference system, i.e. decimal degrees.
	 * 
	 * @return {@link Coord}inate in the WGS84 coordinate reference system.
	 */
	public Coord getCoordWgs84(){
		return this.coord;
	}
	
	/**
	 * Coordinate of the gantry location given in the project coordinate 
	 * reference system adapted for South Africa from the Conic Africa Albers
	 * Equal (based on WGS84).
	 * 
	 * @return {@link Coord}inate in the WGS84_SA_Albers projected coordinate 
	 * 		   reference system.
	 * 
	 * @see {@link TransformationFactory} class for details/specification.
	 */
	public Coord getCoordSaAlbers(){
		return TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers").transform(coord);
	}
	
	/**
	 * Get the colloquial bird name associated with the gantry.
	 * 
	 * @return Bird name. Note: may contain spaces and special characters in the 
	 * 		   name.
	 */
	public String getBirdName(){
		return birdName;
	}
	
}
