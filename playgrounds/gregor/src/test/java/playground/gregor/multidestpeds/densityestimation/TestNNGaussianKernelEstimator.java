package playground.gregor.multidestpeds.densityestimation;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.multidestpeds.denistyestimation.DensityEstimatorFactory;
import playground.gregor.multidestpeds.denistyestimation.NNGaussianKernelEstimator;
import playground.gregor.sim2d_v2.events.DoubleValueStringKeyAtCoordinateEvent;
import playground.gregor.sim2d_v2.events.DoubleValueStringKeyAtCoordinateEventHandler;
import playground.gregor.sim2d_v2.events.XYZAzimuthEventImpl;
import playground.gregor.sim2d_v2.simulation.floor.EnvironmentDistances;
import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

public class TestNNGaussianKernelEstimator implements DoubleValueStringKeyAtCoordinateEventHandler{


	//expected densities from original matlab script
	double [][] rDens = {{0.05531004843828294, 0.06461962109665516, 0.07818174931367144, 0.07940989493141327, 0.04855442109139557, 0.01591438841636447, 0.003110739920578988, 0.000503371428263735, 8.481585694497269e-05, 1.335242807820705e-05}
	,{ 0.06543819134019391, 0.08801153359767838, 0.1307836063079522, 0.1503473335863431, 0.09545531509715531, 0.03065382472994779, 0.005453278579206217, 0.0007395607488368751, 0.000106170510915963, 1.503236134001546e-05}
	, {0.06326215546491266, 0.08841289609650559, 0.1347062272420649, 0.1571796840532226, 0.1023357689937019, 0.0349531372920585, 0.007118820938880202, 0.001127645053330721, 0.0001573129181229497, 1.823013720614749e-05}
	, {0.04914594985267358, 0.06669647000390781, 0.09364804578914235, 0.1076370345926747, 0.07895430422025959, 0.03683477527346643, 0.01221246745132139, 0.002864510501949696, 0.0004251449811446466, 3.8372894925136e-05}
	, {0.03167142877406007, 0.04633377360988119, 0.06973403120400372, 0.09223574849676897, 0.09269963256715118, 0.06782350910340421, 0.03313906731044099, 0.009530514820625035, 0.001482722594578053, 0.0001215876423248983}
	, {0.0174058707140255, 0.03215876378551375, 0.06186926334479069, 0.1029428402221975, 0.1353959624899243, 0.1273016960385769, 0.0731387644980228, 0.0227805971733068, 0.003631811602216356, 0.0002917757048972522}
	, {0.007968575459948066, 0.01894814875084553, 0.04456195312741087, 0.08849046954366056, 0.1402325573887777, 0.1548379743755875, 0.1024810750088059, 0.03400827641849795, 0.005169913928676339, 0.0003933547340408347}
	, {0.002819153442708149, 0.007980797754170075, 0.02114792537018472, 0.04734357473717347, 0.08773418813224272, 0.1466630090165314, 0.177517096891645, 0.06997199524820968, 0.007251234239698689, 0.0003216486397701678}
	, {0.0007106659224428692, 0.002191002428628722, 0.006127747754595524, 0.01460298428403817, 0.03607061852466252, 0.1474301058339117, 0.2890963856351576, 0.1222910298816079, 0.01030561477607557, 0.0002239798519093682}
	, {0.0001216892116851673, 0.0003777938039361385, 0.001053091359012652, 0.002510802938431505, 0.008449441425177593, 0.05499092875721353, 0.1189149518644221, 0.0508181535268455, 0.004141175482057633, 7.29422195609843e-05}
	, {1.409842317255956e-05, 4.083968840183646e-05, 0.0001077170923030927, 0.0002369830868908582, 0.0007095786940910198, 0.00440309936123397, 0.00948162898528585, 0.004050299807790532, 0.0003301679950195928, 5.832823764815563e-06}};
	//expected densities from original matlab script
	double [][] gDens = {{0.001395619178190051, 0.005415121517648516, 0.01655301431694234, 0.03708264101534776, 0.05358128156842683, 0.05133264327365793, 0.04120266087820896, 0.03185065521784164, 0.0213592089006798, 0.01140291288437981}
	, {0.003922270010670948, 0.01512070156343785, 0.04566846399290565, 0.09960862972338995, 0.1324263115673484, 0.1031838088169633, 0.06015447103756499, 0.03800457382248858, 0.02434081302279514, 0.01292835897002555}
	, {0.007841273928961398, 0.02857882508213661, 0.07943205254226732, 0.1592984056576887, 0.1972962457592571, 0.1393875183275484, 0.06748822838701304, 0.03566572475872995, 0.02165487212945095, 0.01142249320764994}
	, {0.01054947091966379, 0.04135208270254001, 0.08422635133145763, 0.1460291828848357, 0.16295733001029, 0.1083994667350273, 0.05008464045241951, 0.02524337642114194, 0.01499703891676663, 0.007871491427029498}
	, {0.01722492366951031, 1.796484102924092, 0.06712624726950024, 0.08780308217492318, 0.08402364562916062, 0.05274253352452433, 0.02534715923094343, 0.01356989118486045, 0.008133631535027476, 0.004251275684229865}
	, {0.0100059223343828, 0.0306508142658485, 0.110803578096632, 0.1186365949438906, 0.03890146105562237, 0.02031057197430287, 0.01048444485254355, 0.006000168174240836, 0.003581246108026685, 0.001842358316713775}
	, {0.01199583664043987, 0.03574731541220044, 0.3234873744700064, 0.3237278247016332, 0.0349590928679953, 0.008796712783276114, 0.004794957305487616, 0.00269913741360015, 0.001485616315667228, 0.0007146975922860162}
	, {0.01567746209862599, 0.02257640415423269, 0.09643335558291886, 0.09396072375238028, 0.01578666565379262, 0.006493267782324155, 0.003584044745626519, 0.001821721927631411, 0.0008468633360810564, 0.0003495428699716245}
	, {0.01905110227169247, 0.02054144596513922, 0.020580654116776, 0.01709143580512637, 0.0112645403525741, 0.006937023314146843, 0.003772943493584343, 0.001805358706065355, 0.0007606137941681092, 0.0002811693086723225}
	, {0.02038594386748875, 0.02183626946653308, 0.02039592524414193, 0.01660961680526094, 0.01179218063739108, 0.007301584322586517, 0.003944609175720638, 0.001860208422667049, 0.0007658709626231858, 0.000275181852122656}
	, {0.01903355538464739, 0.02038497009650405, 0.01903400998339141, 0.01549468599294137, 0.01099695602375629, 0.006804752229635568, 0.003671334302506947, 0.001727146581507601, 0.0007084938370617018, 0.0002534109969669343}};

	double [][] acutualRDens = new double[this.rDens.length][this.rDens[0].length];
	double [][] acutualGDens = new double[this.rDens.length][this.rDens[0].length];

	private final int offsetX = 3;
	private final int offsetY = 6;

	private final double epsilon = 1e-15;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testNNGaussianKernelEstimator() {

		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		QuadTree<EnvironmentDistances> tree = createQuadTree();
		StaticEnvironmentDistancesField sedf = new StaticEnvironmentDistancesField(tree, -1, -1);
		sc.addScenarioElement(sedf);

		EventsManager events = EventsUtils.createEventsManager();
		NNGaussianKernelEstimator est = new DensityEstimatorFactory(events, sc).createDensityEstimator();
		events.addHandler(est);
		events.addHandler(this);

		createEvents(events,sc);

		for (int x = 0; x < this.acutualRDens.length; x++) {
			for (int y = 0; y < this.acutualRDens[0].length; y++) {
				double actualR = this.acutualRDens[x][y];
				double expectedR = this.rDens[x][y];
				Assert.assertEquals(expectedR, actualR,this.epsilon);

				double actualG = this.acutualGDens[x][y];
				double expectedG = this.gDens[x][y];
				Assert.assertEquals(expectedG, actualG,this.epsilon);
			}


		}



	}

	private void createEvents(EventsManager events, Scenario sc) {
		double [] rx = {-3.000,-1.500,-1.000,2.000,3.000,5.000};
		double [] ry = {-6.000, -3.000, -5.000, -2.500, -1.000,0.};
		double [] gx = {-2.000,-1.000, 0., 1.000, 3.000, 6.000};
		double [] gy = {0., -2.000, -3.000,-5.000,-3.500,-5.000};
		for (int i = 0; i < rx.length; i++) {
			Coordinate cr = new Coordinate(rx[i],ry[i]);
			XYZAzimuthEventImpl er = new XYZAzimuthEventImpl(sc.createId("r" + Integer.toString(i)),cr, 0., 0.);
			events.processEvent(er);

			Coordinate cg = new Coordinate(gx[i],gy[i]);
			XYZAzimuthEventImpl eg = new XYZAzimuthEventImpl(sc.createId("g" + Integer.toString(i)),cg, 0., 0.);
			events.processEvent(eg);
		}

		//Density estimation is only performed after a time increment
		XYZAzimuthEventImpl dummy = new XYZAzimuthEventImpl(sc.createId("dummy"),new Coordinate(Double.NaN,Double.NaN), 0., 1.);
		events.processEvent(dummy);
	}

	private QuadTree<EnvironmentDistances> createQuadTree() {
		QuadTree<EnvironmentDistances> ret = new QuadTree<EnvironmentDistances>(-3.500,-6.500,7.500,3.500);
		double [] x = {0., .650, .650, 0., 3.620, 4.230, 4.230, 3.620, .900, 1.200, 3.000, 3.300, .900, 1.200, 3.000, 3.300};
		double [] y = {0., 0., .660, .660, 0., 0., .600, .600, -4.110, -4.110, -4.110, -4.110, -5.000, -5.000, -5.000, -5.000};
		for (int i = 0; i < x.length; i++) {
			Coordinate c = new Coordinate(x[i],y[i]);
			EnvironmentDistances e = new EnvironmentDistances(c);
			e.addEnvironmentDistanceLocation(c);
			ret.put(x[i], y[i], e);
		}

		return ret;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEvent(DoubleValueStringKeyAtCoordinateEvent e) {
		System.out.println(e);
		int idxX = (int) (e.getCoordinate().x+this.offsetX);
		int idxY = (int) (e.getCoordinate().y+this.offsetY);
		if (e.getKey().equals("r")) {
			this.acutualRDens[idxX][idxY] = e.getValue();
		} else if (e.getKey().equals("g")) {
			this.acutualGDens[idxX][idxY] = e.getValue();
		} else {
			throw new RuntimeException("Unknown key:" + e.getKey());
		}
	}
}
