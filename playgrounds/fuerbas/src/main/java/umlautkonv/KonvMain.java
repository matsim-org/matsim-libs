package umlautkonv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class KonvMain {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		File input = new File ("/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/rawdata/Wegeinland.dat");
		File output = new File("/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/rawdata/Wegeinland.conv.dat");
		
		FileReader reader = new FileReader(input);
		
		FileInputStream einlesen = new FileInputStream(input);
		FileOutputStream schreiben = new FileOutputStream(output);
		
		int[] in = new int[(int)input.length()+1];
		
		int tmp=0;
		
		for (int ii=0; ii<input.length(); ii++) {
			if (ii==0) in[ii]=einlesen.read(); 
			else {
				in[ii]=einlesen.read();
				switch (in[ii]){
				case 95:  
					if ( (in[ii-4]== 103 || in[ii-4]==71) && in[ii-3]==114 && in[ii-2]==195 && in[ii-1]==140) 
						{ in[ii-2]=0; in[ii-1]=0; in[ii]=117;} 		//u gruningen
					else if ( in[ii-4]== 104 && in[ii-3]==98 && in[ii-2]==195 && in[ii-1]==140) 
						{ in[ii-2]=0; in[ii-1]=0; in[ii]=117;}	//u kirchbuhlstr
					else if ( (in[ii-4]== 34 ||in[ii-4]== 32 || in[ii-4]== 9) && (in[ii-3]==90 || in[ii-3]==122) && in[ii-2]==195 && in[ii-1]==140) 
						{ in[ii-2]=0; in[ii-1]=0; in[ii]=117;}	//u Zurich
					else if ( (in[ii-4]== 34 ||in[ii-4]== 32 || in[ii-4]== 9) && in[ii-3]==78 && in[ii-2]==195 && in[ii-1]==140) 
						{ in[ii-2]=0; in[ii-1]=0; in[ii]=105;}	//i Nijar
					else if ( in[ii-4]== 110 && in[ii-3]==98 && in[ii-2]==195 && in[ii-1]==140) 
						{ in[ii-2]=0; in[ii-1]=0; in[ii]=117;}	//u Weissenbuhl
					else if ( (in[ii-4]== 34 ||in[ii-4]== 32 || in[ii-4]== 9) && in[ii-3]==68 && in[ii-2]==195 && in[ii-1]==140) 
						{ in[ii-2]=0; in[ii-1]=0; in[ii]=117;}	//u Dubendorf
					else if ( in[ii-4]== 122 && in[ii-3]==98 && in[ii-2]==195 && in[ii-1]==140) 
						{ in[ii-2]=0; in[ii-1]=0; in[ii]=117;}	//u Kreuzbuhlstr
					else if ( (in[ii-4]== 34 ||in[ii-4]== 32 || in[ii-4]== 9) && in[ii-3]==66 && in[ii-2]==195 && in[ii-1]==140) 
						{ in[ii-2]=0; in[ii-1]=0; in[ii]=117;}	//u Bulach
					else if ( (in[ii-4]== 34 ||in[ii-4]== 32 || in[ii-4]== 9) && (in[ii-3]==76 || in[ii-3]==108) && in[ii-2]==195 && in[ii-1]==140) 
						{ in[ii-2]=0; in[ii-1]=0; in[ii]=117;}	//u Lurlibachstr
					break;
				case 138: in[ii]=97; in[ii-1]=0; break;		//ä --> a
				case 142: in[ii]=101; in[ii-1]=0; break;	//é --> e
				case 143: in[ii]=101; in[ii-1]=0; break;	//è --> e
				case 154: in[ii]=111; in[ii-1]=0; break;	//ö --> o
				case 159: in[ii]=117; in[ii-1]=0; break;	//ü --> u				
				case 162: if (in[ii-7]== 195 && in[ii-6]==131 && in[ii-5]==194 && in[ii-4]==131 && in[ii-3]==195 && in[ii-2]==130 && in[ii-1]==194) 
					{ in[ii-7]=0; in[ii-6]=0 ; in[ii-5]=0; in[ii-4]=0; in[ii-3]=0; in[ii-2]=0; in[ii-1]=0; in[ii]=97; } break;	//â --> a	
				case 165: if (in[ii-7]== 195 && in[ii-6]==131 && in[ii-5]==194 && in[ii-4]==131 && in[ii-3]==195 && in[ii-2]==130 && in[ii-1]==194)
					{ in[ii-7]=0; in[ii-6]=0 ; in[ii-5]=0; in[ii-4]=0; in[ii-3]=0; in[ii-2]=0; in[ii-1]=0; in[ii]=97; } break;	//°a --> a
				case 167: if (in[ii-7]== 195 && in[ii-6]==131 && in[ii-5]==194 && in[ii-4]==131 && in[ii-3]==195 && in[ii-2]==130 && in[ii-1]==194) 
					{ in[ii-7]=0; in[ii-6]=0 ; in[ii-5]=0; in[ii-4]=0; in[ii-3]=0; in[ii-2]=0; in[ii-1]=0; in[ii]=99; } break; 	//c
				case 169: if (in[ii-7]== 195 && in[ii-6]==131 && in[ii-5]==194 && in[ii-4]==131 && in[ii-3]==195 && in[ii-2]==130 && in[ii-1]==194) 
					{ in[ii-7]=0; in[ii-6]=0 ; in[ii-5]=0; in[ii-4]=0; in[ii-3]=0; in[ii-2]=0; in[ii-1]=0; in[ii]=101; } break; 	//é --> e	
				case 182: in[ii]=111; in[ii-1]=0; in[ii-2]=0; in[ii-3]=0; break; //ö --> o 
				case 381: //if (in[ii-7]== 195 && in[ii-6]==131 && in[ii-5]==194 && in[ii-4]==131 && in[ii-3]==195 && in[ii-2]==130 && in[ii-1]==194) 
					{ in[ii-7]=0; in[ii-6]=0 ; in[ii-5]=0; in[ii-4]=0; in[ii-3]=0; in[ii-2]=0; in[ii-1]=0; in[ii]=111; } break;	//ô --> o				
				default: ;
				}
			}
		}
		
		
		for (int ii=0; ii<=input.length(); ii++) {
			if (in[ii]!=0) schreiben.write(in[ii]);
		}
			

		
		

	}
	

}
