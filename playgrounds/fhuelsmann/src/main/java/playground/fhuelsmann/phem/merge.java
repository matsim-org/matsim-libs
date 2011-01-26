package playground.fhuelsmann.phem;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;


public class merge {

	private Map<String,Map<String,Map<String,Map<String,Map<String,ModaleObject>>>>> ds1
		= new TreeMap<String,Map<String,Map<String,Map<String,Map<String,ModaleObject>>>>>();
	
	 private Map<String,Map<String,Map<String,String>>> ds2 =
		 new  TreeMap<String,Map<String,Map<String,String>>>();
	
	private Map<String,Map<String,Map<String,Map<String,Map<String,ModaleObject>>>>> ModaleOutputDataStructure =
		new TreeMap<String,Map<String,Map<String,Map<String,Map<String,ModaleObject>>>>>();

	private Map<String,Map<String,String>> ds3 ;

	public Map<String, Map<String, Map<String, Map<String, Map<String, ModaleObject>>>>> getModaleOutputDataStructure() {
		return ModaleOutputDataStructure;
	}

	public void setModaleOutputDataStructure(
			Map<String, Map<String, Map<String, Map<String, Map<String, ModaleObject>>>>> modaleOutputDataStructure) {
		ModaleOutputDataStructure = modaleOutputDataStructure;
	}

	public merge(
			Map<String, Map<String, Map<String, Map<String, Map<String, ModaleObject>>>>> ds1,
			Map<String, Map<String, Map<String, String>>> ds2, Map<String,Map<String,String>> ds3) {
		super();
		this.ds1 = ds1;
		this.ds2 = ds2;
		this.ds3= ds3;
	}

	public void doit() throws IOException{
		
		FileWriter fstream = 
			new FileWriter("../../detailedEval/teststrecke/sim/outputEmissions/outPhem.txt");
		BufferedWriter out = new BufferedWriter(fstream);
		out.write("Day \t EU \t D/G \t sec \t time \t V \t CO \t FC \t HC \t n_norm \t Nox \t Peparted \t PM \n");
		
		FileWriter summaryInformation = 
			new FileWriter("../../detailedEval/teststrecke/sim/outputEmissions/summary.txt");
		BufferedWriter summaryInformationOut = new BufferedWriter(summaryInformation);
		summaryInformationOut.write("Day \t EU \t D/G \t sec \t time \t V \t CO \t FC \t HC \t n_norm \t Nox \t Peparted \t PM \n");
		
		
		
		for(Entry<String, Map<String, String>> day : this.ds3.entrySet()){	
			System.out.println("(ds3) Day: " +  day.getKey());
			Map<String,Map<String,Map<String,Map<String,ModaleObject>>>> tempMap = 
				new TreeMap<String,Map<String,Map<String,Map<String,ModaleObject>>>>();
				this.ModaleOutputDataStructure.put(day.getKey(), tempMap); // create a day , Date	
			for(Entry<String, String> dataofoneday : this.ds3.get(day.getKey()).entrySet()){
				System.out.println("(ds3) time : " +  dataofoneday.getKey());
				for(Entry<String, Map<String, Map<String, String>>> day1 : this.ds2.entrySet() ){
					System.out.println("(ds2) Day: " +  day1.getKey());
					for(Entry<String,Map<String,String>> time : this.ds2.get(day1.getKey()).entrySet()){
						//System.out.println("(ds2) Time" +  time.getKey());
						int range=Integer.valueOf(dataofoneday.getValue().split(";")[0]); // shortcycle-> traveltime;longCycle
						System.out.println("Range aus s3 : "+range);
						int longCycle = Integer.valueOf(dataofoneday.getValue().split(";")[1]);
							System.out.println("longCycle :"+longCycle);

							
						int startTime= Integer.valueOf(dataofoneday.getKey()); // in time! not in sec
						System.out.println("(ds3) StartTime " +  startTime);
							
					
							for(Entry<String, Map<String, Map<String, Map<String, Map<String, ModaleObject>>>>> day3 : this.ds1.entrySet()){
								System.out.println("(ds3) Day :   " +  day3.getKey() );

								for( Entry<String, Map<String, Map<String, Map<String, ModaleObject>>>> DorG : this.ds1.get(day3.getKey()).entrySet()){
									System.out.println("(ds3) D/G :   " + DorG.getKey() );
									for(Entry<String, Map<String, Map<String, ModaleObject>>> EU : this.ds1.get(day3.getKey()).get(DorG.getKey()).entrySet()){
										
											System.out.println("(ds1) EU :   "  + EU.getKey() );
											System.out.println("(Time) : "+ time.getKey() +" with :" + startTime);

											if(this.ds2.get(day.getKey()).get(longCycle+"") !=null){

											if (this.ds2.get(day.getKey()).get(longCycle+"").get(time.getKey())!=null){

										try{	
										int startTimeInDs3 = Integer.valueOf(this.ds2.get(day.getKey()).get(longCycle+"").get(dataofoneday.getKey()));
											System.out.println("(PassendeFile) "+ longCycle);
											System.out.println("(StartTimeInDs3) : "+ startTimeInDs3);
											
											if (this.ds1.get(day3.getKey()).get(DorG.getKey()).get(EU.getKey()).get(longCycle+"")!=null){
											int count=0;
											
											double segmaV =0.0;
											double segmaC0 =0.0;
											double segmaFC =0.0;
											double segmaHC =0.0;
											double segmaN_norm =0.0;
											double segmaNOx =0;
											double segmaPePrated =0.0;
											double segmaPM =0.0;
											
											
											for(int i=startTimeInDs3 /* sec */; i<(startTimeInDs3+range);i++){//"for" loop for every shortCycle
											System.out.println("index:" +  i);
											
											if (this.ds1.get(day3.getKey()).get(DorG.getKey()).get(EU.getKey()).get(longCycle+"").get(i+"")!=null){

											ModaleObject object =this.ds1.get(day3.getKey()).get(DorG.getKey()).get(EU.getKey()).get(longCycle+"").get(i+"");

										
											if (this.ModaleOutputDataStructure.get(day.getKey()).get(DorG.getKey()) != null){

												if(this.ModaleOutputDataStructure.get(day.getKey()).get(DorG.getKey()).get(EU.getKey())!=null){
													
													if (this.ModaleOutputDataStructure.get(day.getKey()).get(DorG.getKey()).get(EU.getKey()).get(time.getKey()) == null) {
														
														Map<String,ModaleObject> tempMap1 = // sec,object 
															new TreeMap<String,ModaleObject>();
														
														tempMap1.put(time.getKey(), object);
														
														this.ModaleOutputDataStructure.get(day.getKey()).get(DorG.getKey()).get(EU.getKey()).put(time.getKey(), tempMap1);

													}
													else{
														
														this.ModaleOutputDataStructure.get(day.getKey()).get(DorG.getKey()).get(EU.getKey()).get(time.getKey()).put(""+startTimeInDs3, object);

												}
											}
												
										else if(this.ModaleOutputDataStructure.get(day.getKey()).get(DorG.getKey()).get(EU.getKey())==null ){			//if(this.ModaleOutputDataStructure.get(GD).get(eu) !=null){
													
												Map<String,Map<String,ModaleObject>> tempMapOfTime = 
													new TreeMap<String,Map<String,ModaleObject>>();
												Map<String,ModaleObject> tempMapOfTime1 = 
													new TreeMap<String,ModaleObject>();
												tempMapOfTime1.put(""+startTimeInDs3, object);
												tempMapOfTime.put(time.getKey(), tempMapOfTime1);
												this.ModaleOutputDataStructure.get(day.getKey()).get(DorG.getKey()).put(EU.getKey(), tempMapOfTime);
											}
										}
										else if (this.ModaleOutputDataStructure.get(DorG.getKey()) == null){
										

											Map<String,ModaleObject> tempMap1 = // sec,object 
												new TreeMap<String,ModaleObject>();
											
											Map<String,Map<String,ModaleObject>> tempMap2 = // zeit,sec,object 
											
												new TreeMap<String,Map<String,ModaleObject>>();
											
											
											Map<String,Map<String,Map<String,ModaleObject>>> tempMap3 = 
												new TreeMap<String,Map<String,Map<String,ModaleObject>>>(); // eu,zeit,sec,object
											
											tempMap1.put(startTimeInDs3+"", object);
											tempMap2.put(time.getKey(), tempMap1);
											tempMap3.put(EU.getKey(), tempMap2);
											this.ModaleOutputDataStructure.get(day.getKey()).put(DorG.getKey(), tempMap3);
											
															
										}
						
											segmaV  +=Double.valueOf(object.getV())/3600;
											segmaC0 +=Double.valueOf(object.getCO())/3600;
											segmaFC +=Double.valueOf(object.getFC())/3600;
											segmaHC +=Double.valueOf(object.getHC())/3600;
											segmaN_norm +=Double.valueOf(object.getN_norm())/3600;
											segmaNOx +=Double.valueOf(object.getNOx())/3600;
											segmaPePrated +=Double.valueOf(object.getPePrated())/3600;
											segmaPM +=Double.valueOf(object.getPM())/3600;

										int s=	(Integer.valueOf(dataofoneday.getKey())+count++ /*shortCycle*/);
											out.append( 
													day.getKey()+ 
													"\t" + EU.getKey() + 
													"\t"+ DorG.getKey()+ 
													"\t" + i+// current sec
													"\t"+ s +
													"\t" + object.getV() + 
													"\t" + object.getCO() + 
													"\t" + object.getFC() +
													"\t" + object.getHC() +
													"\t" + object.getN_norm() +														
													"\t" + object.getNOx() +
													"\t" + object.getPePrated() +
													"\t" + object.getPM()+
								
													"\n");

										
											
											}//if (this.ds1.get(day3.getKey()).get(DorG.getKey()).get(EU.getKey()).get(longCycle+"").get(i+"")!=null){

										}//for(int i=GstartTime /* sec */; i<(GstartTime+range);i++){

											
											out.append( 
													day.getKey()+ 
													"\t" + EU.getKey() + 
													"\t"+ DorG.getKey()+ 
													"\t" + startTimeInDs3 +
													"\t" + dataofoneday.getKey()+
													
													"\t" + segmaV + 
													"\t" + segmaC0 + 
													"\t" + segmaFC +
													"\t" + segmaHC +
													"\t" + segmaN_norm +														
													"\t" + segmaNOx +
													"\t" + segmaPePrated +
													"\t" + segmaPM+
								
													"\n\n");
											
										summaryInformationOut.append(
													day.getKey()+ 
													"\t" + EU.getKey() + 
													"\t"+ DorG.getKey()+ 
													"\t" + startTimeInDs3 +
													"\t" + dataofoneday.getKey()+
													
													"\t" + segmaV + 
													"\t" + segmaC0 + 
													"\t" + segmaFC +
													"\t" + segmaHC +
													"\t" + segmaN_norm +														
													"\t" + segmaNOx +
													"\t" + segmaPePrated +
													"\t" + segmaPM+
													"\n");
											

											
											

									}//if (this.ds1.get(day3.getKey()).get(DorG.getKey()).get(EU.getKey()).get(longCycle+"")!=null){
										}catch(Exception e){}
								}//if (this.ds2.get(day.getKey()).get(longCycle+"").get(time.getKey())!=null){
											
							}//for(Entry<String, Map<String, Map<String, ModaleObject>>> EU : this.ds1.get(day3.getKey()).get(DorG.getKey()).entrySet()){
					
						}//for( Entry<String, Map<String, Map<String, Map<String, ModaleObject>>>> DorG : this.ds1.get(day3.getKey()).entrySet()){

					}//for(Entry<String, Map<String, Map<String, Map<String, Map<String, ModaleObject>>>>> day3 : this.ds1.entrySet()){
													
				}//	for(Entry<String,Map<String,String>> time : this.ds2.get(day1.getKey()).entrySet()){

			} //for(Entry<String, Map<String, Map<String, String>>> day1 : this.ds2.entrySet() ){

		} //for(Entry<String, String> dataofoneday : this.ds3.get(day.getKey()).entrySet())	
	} //for(Entry<String, Map<String, String>> day : this.ds3.entrySet()){
		}
		//Close the output stream
		out.close();
}//doit	
	}// class
											