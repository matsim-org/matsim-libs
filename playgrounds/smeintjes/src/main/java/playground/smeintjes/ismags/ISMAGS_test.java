package playground.smeintjes.ismags;

/* 
 * Copyright (C) 2013 Maarten Houbraken
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Software available at https://github.com/mhoubraken/ISMAGS
 * Author : Maarten Houbraken (maarten.houbraken@intec.ugent.be)
 */

import java.io.IOException;

/**
 * Test class to show how the code can be used. Examples given can be used to
 * recreate the experiments in the paper.
 */
public class ISMAGS_test {

    public static void main(String[] args) throws IOException {
        String foldername = "C:/Users/sumarie/Documents/Meesters/Random networks/random_0.txt";
        String output = "Test ISMAGS/AXX.txt"; //Change folder according to size of motif

        /* Three node motifs */
//        String motif = "a0A";
//        String motif = "a0a";
//        String motif = "A0a";
//        String motif = "X0A";
//        String motif = "X0a";
//        String motif = "X0X";
//        String motif = "aAa";
//        String motif = "aAA";
//        String motif = "AAX";
//        String motif = "AaX";
//        String motif = "XAA";
//        String motif = "XXX";
        String motif = "AXX";
        String linkfiles  = "\"A d X X onedirectional.txt X u X X bidirectional.txt\"";
        
//        String motif = "XZ00ZY";
//        String motif = "XXXZ000Z0Y00ZYY";
//        String linkfiles = "\"X u X X Xu_yeast.txt Y u Y Y Yu_human.txt Z d X Y Z_ortho_yeast_human.txt\"";
//        String linkfiles="\"X d X X Xu_yeast.txt Y d Y Y Yu_human.txt Z d X Y Z_ortho_yeast_human.txt\"";

//        String motif = "GGG";
//        String motif = "SSS";
//        String motif = "GPS";
//        String motif = "SsS";
//        String motif = "SSG";
//        String motif = "SsG";
//        String motif = "GGS";
//        String motif = "GGP";
//        String motif = "ssG";
//        String motif = "PGSPGS";
//        String motif = "P0P00PP00P000P000P0000P000P0P0000PP00000P0PP0";//peterson
//        String motif = "P0P";
//        String motif = "P0P00P";
//        String motif = "P0P00P000P";
//        String linkfiles = "\"G u t t Gu.txt P u t t Pu.txt S d t t Sd.txt\"";

//        String motif = "PPP";//3
//        String motif = "PPPPPP";//4
//        String motif = "PPPPPPPPPP";//5
//        String motif = "PPPPPPPPPPPPPPP";//6
//        String motif = "PPPPPPPPPPPPPPPPPPPPP";//7
//        String motif = "PPPPPPPPPPPPPPPPPPPPPPPPPPPP";//8
//        String motif = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP";//9
//        String motif = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP";//10
//        String linkfiles = "\"P u t t Gu.txt P u t t Pu.txt P u t t Sd.txt\"";
//        String linkfiles = "\"P d t t 20_20_clean_sorted.txt\"";


//        String motif = "WWW";
//        String linkfiles = "\"W u t t Wiki-Vote.txt\"";
//        String motif = "WwWWWW";
//        String linkfiles = "\"W d t t Wiki-Vote.txt\"";

//        String motif = "TTT";
//        String linkfiles = "\"T u t t p2p-Gnutella08.txt\"";
//        String motif = "TtTTTT";      
//        String linkfiles = "\"T d t t p2p-Gnutella08.txt\"";

//        String motif = "NNN";
//        String linkfiles = "\"N u t t p2p-Gnutella30.txt\"";
//        String motif = "NnNNNN";
//        String linkfiles = "\"N d t t p2p-Gnutella30.txt\"";

//        String motif = "DDD";

//        String linkfiles = "\"D u t t CA-CondMat.txt\"";
//        String motif = "DdDDDD";
//        String linkfiles = "\"D d t t CA-CondMat.txt\"";
//
//        String motif = "CCC";

//        String linkfiles = "\"C u t t CA-HepTh.txt\"";     
//        String motif = "CcCCCC";
//        String linkfiles = "\"C d t t CA-HepTh.txt\"";     

        String[] ar = new String[]{"-folder", foldername, "-linkfiles", linkfiles, "-output", foldername + output, "-motif", motif};
//        String[] ar = new String[]{"-folder", foldername, "-linkfiles", linkfiles, "-output", output, "-motif", motif};
        CommandLineInterface.main(ar);

    }
}
