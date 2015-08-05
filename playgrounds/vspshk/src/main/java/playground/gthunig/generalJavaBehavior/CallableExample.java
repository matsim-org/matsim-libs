package playground.gthunig.generalJavaBehavior;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CallableExample {

  public static class WordLengthCallable
        implements Callable<Integer> {
    private String word;
    public WordLengthCallable(String word) {
    	this.word = word;
    }
    public Integer call() {
    	try {
			Thread.sleep(10 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	return Integer.valueOf(word.length());
    }
  }

  public static void main(String args[]) throws Exception {
	ArrayList<String> input = new ArrayList<String>(Arrays.asList("Tick", "Trick", "Track"));
	  
    ExecutorService pool = Executors.newFixedThreadPool(3);
    Set<Future<Integer>> set = new HashSet<Future<Integer>>();
    for (String word: input) {
    	Callable<Integer> callable = new WordLengthCallable(word);
    	Future<Integer> future = pool.submit(callable);
    	set.add(future);
    }
    int sum = 0;
    for (Future<Integer> future : set) {
    	System.out.println(future.isDone());
    	sum += future.get();
    }
    System.out.printf("The sum of lengths is %s%n", sum);
    System.exit(sum);
  }
}