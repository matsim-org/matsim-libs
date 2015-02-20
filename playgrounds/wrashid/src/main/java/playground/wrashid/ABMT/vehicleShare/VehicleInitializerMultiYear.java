package playground.wrashid.ABMT.vehicleShare;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.LegImpl;
import org.matsim.api.core.v01.Id;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

/**
 * 
 * @author wrashid
 *
 */
public class VehicleInitializerMultiYear implements IterationStartsListener, StartupListener {

	
	private static Random random = new Random(GlobalTESFParameters.tesfSeed);
	
	
	protected static final Logger log = Logger.getLogger(VehicleInitializerMultiYear.class);

	public static IntegerValueHashMap<Id> vehicleExpiryYear = new IntegerValueHashMap<Id>();

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		
		
		
		
		if (isLastIteration(event)){
			//falls person momentan 
		}
	}

	private boolean isLastIteration(IterationStartsEvent event) {
		return event.getIteration()==Integer.parseInt(event.getControler().getConfig().getParam("controler", "lastIteration"));
	}

	@Override
	public void notifyStartup(StartupEvent event) {

		if (GlobalTESFParameters.currentYear == 0) {
			Random random = new Random(GlobalTESFParameters.tesfSeed);
			for (Person person : event.getControler().getScenario().getPopulation().getPersons().values()) {
				if (VehicleInitializer.hasCarLeg(person.getSelectedPlan())) {
					vehicleExpiryYear.set(person.getId(), random.nextInt(8));
				}
			}
		}

	}
	
	// this is an approximation of statistics of canton Zurich of year 2004.
	// http://www.pxweb.bfs.admin.ch/dialog/statfile.asp?lang=1
	public static int getVehicleExpiryInYears(){
		
		double rand=random.nextDouble();
		double[] intervals=new double[66];
		intervals[0]=1;
		intervals[1]=0.919819080721826;
		intervals[2]=0.838438290737634;
		intervals[3]=0.757057500753442;
		intervals[4]=0.676876581475269;
		intervals[5]=0.599042592129834;
		intervals[6]=0.524600824100748;
		intervals[7]=0.45445316274043;
		intervals[8]=0.389326497320574;
		intervals[9]=0.329752928896889;
		intervals[10]=0.276062420835735;
		intervals[11]=0.228387438895019;
		intervals[12]=0.186678159732814;
		intervals[13]=0.150726085388678;
		intervals[14]=0.120193446443482;
		intervals[15]=0.0946456263474996;
		intervals[16]=0.0735839724198346;
		intervals[17]=0.0564767221336193;
		intervals[18]=0.0427862931264283;
		intervals[19]=0.0319917811044925;
		intervals[20]=0.0236061051322574;
		intervals[21]=0.0171877721800968;
		intervals[22]=0.0123476591073171;
		intervals[23]=0.00875150852258835;
		intervals[24]=0.00611900280385394;
		intervals[25]=0.00422033114982871;
		intervals[26]=0.00287112132391909;
		intervals[27]=0.00192649899912706;
		intervals[28]=0.00127489162237392;
		intervals[29]=0.000832035297264298;
		intervals[30]=0.000535491544235845;
		intervals[31]=0.00033984878878964;
		intervals[32]=0.000212677866623045;
		intervals[33]=0.000131233523721213;
		intervals[34]=7.98429962623437E-05;
		intervals[35]=4.78942169442118E-05;
		intervals[36]=2.83249525381302E-05;
		intervals[37]=1.65151193208399E-05;
		intervals[38]=9.49309946245097E-06;
		intervals[39]=5.3794302922434E-06;
		intervals[40]=3.00507512165146E-06;
		intervals[41]=1.65483498678601E-06;
		intervals[42]=8.98306401478268E-07;
		intervals[43]=4.80679307908564E-07;
		intervals[44]=2.53535461445369E-07;
		intervals[45]=1.31815371160488E-07;
		intervals[46]=6.75506646653858E-08;
		intervals[47]=3.41210202768106E-08;
		intervals[48]=1.6987762774196E-08;
		intervals[49]=8.33614984480352E-09;
		intervals[50]=4.03184372216778E-09;
		intervals[51]=1.92196169545876E-09;
		intervals[52]=9.02989978179278E-10;
		intervals[53]=4.18131570831485E-10;
		intervals[54]=1.90822546277291E-10;
		intervals[55]=8.58278571504982E-11;
		intervals[56]=3.80455647958537E-11;
		intervals[57]=1.6620761496485E-11;
		intervals[58]=7.15593624361417E-12;
		intervals[59]=3.03635101568066E-12;
		intervals[60]=1.26968416732224E-12;
		intervals[61]=5.23232749598117E-13;
		intervals[62]=2.12513852932308E-13;
		intervals[63]=8.50800094525314E-14;
		intervals[64]=3.35107258237542E-14;
		intervals[65]=0;

		for (int i=0;i<65;i++){
			if (intervals[i]>=rand && intervals[i+1]<=rand){
				return i;
			}
		}
		
		DebugLib.stopSystemAndReportInconsistency();
		
		return -1;
	}
	
	public static void main(String[] args) {
		
	}
}
