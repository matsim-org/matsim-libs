package playground.fzwick;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.neethi.All;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

public class NetworkStatistics {

	public static void main(String[] args) {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("C:/Users/Felix/Documents/VSP/Berlin-Netz/merged-filtered.xml");
		       
        int noFsTag =0;
        int fsTag=0;
        int all=0;
        int motorway=0;
        int motorwayFsTag=0;
        int motorwayNoFsTag=0;
        int trunk=0;
        int trunkFsTag=0;
        int trunkNoFsTag=0;		
        int primary=0;
        int primaryFsTag=0;
        int primaryNoFsTag=0;
        int secondary=0;
        int secondaryFsTag=0;
        int secondaryNoFsTag=0;
        int tertiary=0;
        int tertiaryFsTag=0;
        int tertiaryNoFsTag=0;
        int minor=0;
        int minorFsTag=0;
        int minorNoFsTag=0;
        int residential=0;
        int residentialFsTag=0;
        int residentialNoFsTag=0;
        int livingStreet=0;
        int livingStreetFsTag=0;
        int livingStreetNoFsTag=0;
        double share=0.0;
        double motorwayShare=0.0;
        double trunkShare=0.0;
        double primaryShare=0.0;
        double secondaryShare=0.0;
        double tertiaryShare=0.0;
        double minorShare=0.0;
        double residentialShare=0.0;
        double livingStreetShare=0.0;
                
        for (Link l : network.getLinks().values()){
        	all++;
        	if (l.getFreespeed() ==0.0){
        		noFsTag++;
        	}
        	else{
        	fsTag++;
        	}
        	
        	if (l.getAttributes().toString().contains("motorway")){
        		motorway++;
        		if (l.getFreespeed() ==0.0){
            		motorwayNoFsTag++;
            	}
            	else{
            	motorwayFsTag++;
            	}
        		
        	}
        	
        	if (l.getAttributes().toString().contains("trunk")){
        		trunk++;
        		if (l.getFreespeed() ==0.0){
            		trunkNoFsTag++;
            	}
            	else{
            	trunkFsTag++;
            	}
            	
            }

        	if (l.getAttributes().toString().contains("primary")){
        		primary++;
        		if (l.getFreespeed() ==0.0){
            		primaryNoFsTag++;
            	}
            	else{
            	primaryFsTag++;
            	}
        
        	}
        	
        	if (l.getAttributes().toString().contains("secondary")){
        		secondary++;
        		if (l.getFreespeed() ==0.0){
            		secondaryNoFsTag++;
            	}
            	else{
            	secondaryFsTag++;
            	}
        		
        	}
        	
        	if (l.getAttributes().toString().contains("tertiary")){
        		tertiary++;
        		if (l.getFreespeed() ==0.0){
            		tertiaryNoFsTag++;
            	}
            	else{
            	tertiaryFsTag++;
            	}
        
        	}
        	
        	if (l.getAttributes().toString().contains("minor")){
        		minor++;
        		if (l.getFreespeed() ==0.0){
            		minorNoFsTag++;
            	}
            	else{
            	minorFsTag++;
            	}
//        		
        	}
        	
        	if (l.getAttributes().toString().contains("residential")){
        		residential++;
        		if (l.getFreespeed() ==0.0){
            		residentialNoFsTag++;
            	}
            	else{
            	residentialFsTag++;
            	}
        		
        	}
        	
        	if (l.getAttributes().toString().contains("living_street")){
        		livingStreet++;
        		if (l.getFreespeed() ==0.0){
            		livingStreetNoFsTag++;
            	}
            	else{
            	livingStreetFsTag++;
            	}
        		
        	}
        	
        }
        share =  (double) fsTag / (double) all;
        motorwayShare=(double)motorwayFsTag/(double)motorway;
        trunkShare=(double)trunkFsTag/(double)trunk;
		primaryShare=(double)primaryFsTag/(double)primary;
		secondaryShare=(double)secondaryFsTag/(double)secondary;
		tertiaryShare=(double)tertiaryFsTag/(double)tertiary;
//		minorShare=(double)minorFsTag/(double)minor;
		residentialShare=(double)residentialFsTag/(double)residential;
		livingStreetShare=(double)livingStreetFsTag/(double)livingStreet;
		
		File file = new File( "C:/Users/Felix/Documents/VSP/Berlin-Netz/FreespeedTagsStatistics.xml");
 		try {
 			BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
 		
		bw.write("\tAll \t Motorway \t Trunk \t Primary \t Secondary \t Tertiary \t Minor \t Residential \t livingStreet");
		bw.newLine();
		bw.write("number:"+all+"\t"+motorway+"\t"+trunk+"\t"+primary+"\t"+secondary+"\t"+tertiary+"\t"+minor+"\t"+residential+"\t"+livingStreet);
		bw.newLine();
		bw.write("FsTag:"+fsTag+"\t"+motorwayFsTag+"\t"+trunkFsTag+"\t"+primaryFsTag+"\t"+secondaryFsTag+"\t"+tertiaryFsTag+"\t"+minorFsTag+"\t"+residentialFsTag+"\t"+livingStreetFsTag);
		bw.newLine();
		bw.write("NoFsTag:"+noFsTag+"\t"+motorwayNoFsTag+"\t"+trunkNoFsTag+"\t"+primaryNoFsTag+"\t"+secondaryNoFsTag+"\t"+tertiaryNoFsTag+"\t"+minorNoFsTag+"\t"+residentialNoFsTag+"\t"+livingStreetNoFsTag);
		bw.newLine();
		bw.write("Ratio:"+share+"\t"+motorwayShare+"\t"+trunkShare+"\t"+primaryShare+"\t"+secondaryShare+"\t"+tertiaryShare+"\t"+minorShare+"\t"+residentialShare+"\t"+livingStreetShare);
		bw.close();
 		}
	
 		catch (IOException e) {
			e.printStackTrace();
		}
//        System.out.println("No Freespeed Tag: "+noFSTag);
//        System.out.println("Freespeed Tag:    "+FTag);

	}
}

