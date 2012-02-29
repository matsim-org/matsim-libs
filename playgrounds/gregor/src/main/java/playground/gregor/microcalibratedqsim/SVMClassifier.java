package playground.gregor.microcalibratedqsim;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;


/**
 * wrapper for the java-ml classifier 
 * @author laemmel
 *
 */
public class SVMClassifier implements LinkVelocityClassifier {

	private static final Logger log = Logger.getLogger(SVMClassifier.class);

	private static final int BUILD_INTERVAL = 150;	

//		MultilayerPerceptron mlp = new MultilayerPerceptron();
	//	Bagging bagging = new Bagging();
//	Classifier classifier = new LibSVM();
//		Classifier classifier = new KNearestNeighbors(5);
//		Classifier classifier = new WekaClassifier(this.mlp);
//	Classifier classifier = new RandomForest(50);

//	Dataset data = new DefaultDataset();

	private final boolean caliMode = true;

	private Id[] ids;

	@Override
	public double getEstimatedVelocity(double[] onLink) {
//		Instance instance = new DenseInstance(onLink);
//		double ret = (Double) this.classifier.classify(instance);
//		return ret;
		return 0;
	}

	@Override
	public void addInstance(double[] input, double output) {
//		Instance instance = new DenseInstance(input, output);
//		this.data.add(instance);
////		if (this.data.size() % BUILD_INTERVAL == 0) {
////			build();
////		}
	}

	@Override
	public void build() {
//		log.info("starting svm training");
//		log.info("data size: " + this.data.size());
//		log.info("starting 10-fold cross-validation");
//		//		svm_parameter param = new svm_parameter();
//		//		svm_parameter param2 = ((LibSVM)this.svm).getParameters();
//		//		System.out.println("C " + param.C + " " + param2.C);
//		//		System.out.println("cache_size " + param.cache_size + " " + param2.cache_size);
//		//		System.out.println("coef0 " + param.coef0 + " " + param2.coef0);
//		//		System.out.println("degree " + param.degree + " " + param2.degree);
//		//		System.out.println("eps " + param.eps + " " + param2.eps);
//		//		System.out.println("gamma " + param.gamma + " " + param2.gamma);
//		//		System.out.println("kernel_type " + param.kernel_type + " " + param2.kernel_type);
//		//		System.out.println("nr_weight " + param.nr_weight + " " + param2.nr_weight);
//		//		System.out.println("nu " + param.nu + " " + param2.nu);
//		//		System.out.println("p " + param.p + " " + param2.p);
//		//		System.out.println("shrinking " + param.shrinking + " " + param2.shrinking);
//		//		System.out.println("svm_type " + param.svm_type + " " + param2.svm_type);
//
//		//		param2.svm_type = 0;
//		//		param2.kernel_type = 0;
//		//		param2.degree = 3;
//		//		param2.gamma = .25;
//		//		param2.coef0 = 0.1;
//		//		param2.nu = .5;
//		//		param2.eps = 0.001;
//		//		param2.cache_size = 100;
//		//		param2.shrinking = 1;
//		//		param2.probability = 0;
//		////		param2.weight = {1.,1.,1.,1.};
//		//		((LibSVM)this.svm).setParameters(param2);
//		Collections.shuffle(this.data);
//		List<Dataset> sets = new ArrayList<Dataset>();
//		int from = 0; int incr = this.data.size()/10;
//		for (int i = 0; i < 9; i++) {
//			Dataset tmpData = new DefaultDataset();
//			tmpData.addAll(this.data.subList(from, from+incr-1));
//			sets.add(tmpData);
//			from += incr;
//		}
//		Dataset tmpData = new DefaultDataset();
//		tmpData.addAll(this.data.subList(from, this.data.size()-1));		
//		sets.add(tmpData);
//
//		double relErr = 0; double meanerr = 0; double avgDiff = 0;
//		for (int i = 0; i < 10; i++) {
//			log.info("fold " + i);
//			Dataset cali = new DefaultDataset();
//			for (int j = 0; j < 10; j++) {
//				if (j == i) {
//					continue;
//				}
//				cali.addAll(sets.get(j));
//			}
//			Dataset vali = new DefaultDataset();
//			vali.addAll(sets.get(i));
//			this.classifier.buildClassifier(cali);
//			for (Instance inst : vali) {
//				double pred = (Double) this.classifier.classify(inst);
//				double real = (Double) inst.classValue();
//				double diff = (pred-real);
//				avgDiff += diff;
//				meanerr += Math.abs(diff);
//				relErr += Math.abs(diff/real);
////				if (i == 9){
//					log.info(real + "\t" + pred + "\t" + diff);
////				}
//			}
//		}
//
//
//
//		double absRel = relErr/this.data.size();
//		log.info("finished 10-fold cross-validation");
//		Id id = getIds()[getIds().length-1];
//		log.info("link:" + id + "  abs rel err:" + (int)(100*absRel+0.5) + "%    mean abs error:" + meanerr/this.data.size() + "  avg diff:" + avgDiff/this.data.size());
//		this.classifier.buildClassifier(this.data);
//		if (absRel < 0.05) {
//			this.caliMode = false;
//			this.classifier.buildClassifier(this.data);
//			
//		}
		//DEBUG

	}

	@Override
	public boolean isCalibrationMode(){
		return this.caliMode;
	}

	@Override
	public void setIds(Id[] ids) {
		this.ids = ids;
	}
	
	@Override
	public Id[] getIds() {
		return this.ids;
	}
}
