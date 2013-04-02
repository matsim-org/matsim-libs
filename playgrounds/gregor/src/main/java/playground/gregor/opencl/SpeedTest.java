/* *********************************************************************** *
 * project: org.matsim.*
 * SpeedTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.opencl;

//import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
//import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
//import static java.lang.System.nanoTime;
//import static java.lang.System.out;
//
//import java.io.IOException;
//import java.nio.FloatBuffer;
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Random;
//
//import org.matsim.core.gbl.MatsimRandom;
//
//import com.jogamp.opencl.CLBuffer;
//import com.jogamp.opencl.CLCommandQueue;
//import com.jogamp.opencl.CLContext;
//import com.jogamp.opencl.CLDevice;
//import com.jogamp.opencl.CLKernel;
//import com.jogamp.opencl.CLProgram;
//
//
//public class SpeedTest {
//
//	private static final int NUM_TESTS = 10000000;
//	private final int size = 10;
//	private final List<Integer> data;
//	public SpeedTest() {
//		this.data = new ArrayList<Integer>();
//		for (int i = 0; i < 1000; i++) {
//			this.data.add(i);
//		}
//	}
//
//	public void testArrayList() {
//		MatsimRandom.reset();
//		Random r = MatsimRandom.getLocalInstance();
//		ArrayList<Integer> test = new ArrayList<Integer>(this.size);
//		for (int i = 0; i < this.size; i++){
//			int idx = r.nextInt(1000);
//			test.add(this.data.get(idx));
//		}
//
//		int [] idx0s = new int[NUM_TESTS];
//		int [] idx1s = new int[NUM_TESTS];
//		for (int i = 0; i < NUM_TESTS; i++) {
//			int idx0 = r.nextInt(this.size);
//			int idx1 = r.nextInt(this.size);
//			idx0s[i] = idx0;
//			idx1s[i] = idx1;
//		}
//
//		long start = System.currentTimeMillis();
//		for (int i = 0; i < NUM_TESTS; i++) {
//			int idx0 = idx0s[i];
//			int idx1 = idx1s[i];
//			Integer tmp = test.get(idx0);
//			test.set(idx0, test.get(idx1));
//			test.set(idx1,tmp);
//		}
//		long stop = System.currentTimeMillis();
//		System.out.println("ArrayList took:\t\t" + (stop-start));
//	}
//
//	public void testLinkedList() {
//		MatsimRandom.reset();
//		Random r = MatsimRandom.getLocalInstance();
//		LinkedList<Integer> test = new LinkedList<Integer>();
//		for (int i = 0; i < this.size; i++){
//			int idx = r.nextInt(1000);
//			test.add(this.data.get(idx));
//		}
//
//		int [] idx0s = new int[NUM_TESTS];
//		int [] idx1s = new int[NUM_TESTS];
//		for (int i = 0; i < NUM_TESTS; i++) {
//			int idx0 = r.nextInt(this.size);
//			int idx1 = r.nextInt(this.size);
//			idx0s[i] = idx0;
//			idx1s[i] = idx1;
//		}
//
//		long start = System.currentTimeMillis();
//		for (int i = 0; i < NUM_TESTS; i++) {
//			int idx0 = idx0s[i];
//			int idx1 = idx1s[i];
//			Integer tmp = test.get(idx0);
//			test.set(idx0, test.get(idx1));
//			test.set(idx1,tmp);
//		}
//		long stop = System.currentTimeMillis();
//		System.out.println("LinkedList took:\t" + (stop-start));
//	}
//
//	public void testArray() {
//		MatsimRandom.reset();
//		Random r = MatsimRandom.getLocalInstance();
//		Integer [] test = new Integer [this.size];
//		for (int i = 0; i < this.size; i++){
//			int idx = r.nextInt(1000);
//			test[i]=(this.data.get(idx));
//		}
//
//		int [] idx0s = new int[NUM_TESTS];
//		int [] idx1s = new int[NUM_TESTS];
//		for (int i = 0; i < NUM_TESTS; i++) {
//			int idx0 = r.nextInt(this.size);
//			int idx1 = r.nextInt(this.size);
//			idx0s[i] = idx0;
//			idx1s[i] = idx1;
//		}
//
//		long start = System.currentTimeMillis();
//		for (int i = 0; i < NUM_TESTS; i++) {
//			int idx0 = idx0s[i];
//			int idx1 = idx1s[i];
//			Integer tmp = test[idx0];
//			test[idx0] = test[idx1];
//			test[idx1] = tmp;
//		}
//		long stop = System.currentTimeMillis();
//		System.out.println("Array took:\t\t" + (stop-start));
//	}
//
//
//	public static void testOpenCL() throws IOException {
//		// set up (uses default CLPlatform and creates context for all devices)
//		CLContext context = CLContext.create();
//		System.out.println("created "+context);
//
//		// always make sure to release the context under all circumstances
//		// not needed for this particular sample but recommented
//		try{
//
//			// select fastest device
//			CLDevice device = context.getMaxFlopsDevice();
//			System.out.println("freq: " + device.getMaxClockFrequency());
//			System.out.println("units: " + device.getMaxComputeUnits());
////			device.get
//			System.out.println("using "+device);
//			
//			// create command queue on device.
//			CLCommandQueue queue = device.createCommandQueue();
//
//			int elementCount = 128;                                  // Length of arrays to process
//			System.out.println(device.getMaxWorkGroupSize());
//			int localWorkSize = Math.min(device.getMaxWorkGroupSize(), 128);  // Local work size dimensions
//			int globalWorkSize = roundUp(localWorkSize, elementCount);   // rounded up to the nearest multiple of the localWorkSize
//
//			// load sources, create and build program
//			CLProgram program = context.createProgram(kernel).build();
//
//			// A, B are input buffers, C is for the result
//			CLBuffer<FloatBuffer> clBufferA = context.createFloatBuffer(globalWorkSize, READ_ONLY);
//			CLBuffer<FloatBuffer> clBufferB = context.createFloatBuffer(globalWorkSize, READ_ONLY);
//			CLBuffer<FloatBuffer> clBufferC = context.createFloatBuffer(globalWorkSize, WRITE_ONLY);
//
//			out.println("used device memory: "
//					+ (clBufferA.getCLSize()+clBufferB.getCLSize()+clBufferC.getCLSize())/1000000 +"MB");
//
//			// fill input buffers with random numbers
//			// (just to have test data; seed is fixed -> results will not change between runs).
//			fillBuffer(clBufferA.getBuffer(), 12345);
//			fillBuffer(clBufferB.getBuffer(), 67890);
//
//			// get a reference to the kernel function with the name 'VectorAdd'
//			// and map the buffers to its input parameters.
//			CLKernel kernel = program.createCLKernel("VectorAdd");
//			kernel.putArgs(clBufferA, clBufferB, clBufferC).putArg(elementCount);
//
//			// asynchronous write of data to GPU device,
//			// followed by blocking read to get the computed results back.
//			long time = nanoTime();
//			for (int ii = 0; ii < 100 ; ii++) {
//				clBufferC.getBuffer().clear();
//				queue.putWriteBuffer(clBufferA, false)
//				.putWriteBuffer(clBufferB, false)
//				.put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize)
//				.putReadBuffer(clBufferC, true);
//				clBufferA.getBuffer().rewind();
//				clBufferB.getBuffer().rewind();
//			}
//			time = nanoTime() - time;
//
//			// print first few elements of the resulting buffer to the console.
//			out.println("exp(a)+exp(b)=c results snapshot: ");
//			for(int i = 0; i < 10; i++)
//				out.print(clBufferC.getBuffer().get() + ", ");
//			out.println("...; " + clBufferC.getBuffer().remaining() + " more");
//
//			out.println("computation took: "+(time/1000000)+"ms");
//
//			long time2 = nanoTime();
//			for (int i = 0; i < 100; i++ ) {
//				clBufferA.getBuffer().rewind();
//				clBufferB.getBuffer().rewind();
//				clBufferC.getBuffer().clear();
//				FloatBuffer cc = clBufferC.getBuffer();
//				while (clBufferA.getBuffer().remaining() > 0) {
//					cc.put(playground.gregor.sim2d_v4.math.Math.exp(clBufferA.getBuffer().get())+playground.gregor.sim2d_v4.math.Math.exp(clBufferB.getBuffer().get()));
//					//            	cc.put((float) (Math.exp(clBufferA.getBuffer().get())+Math.exp(clBufferB.getBuffer().get())));
//				}
//				cc.rewind();
//			}
//			time2 = nanoTime() - time2;
//			// print first few elements of the resulting buffer to the console.
//			out.println("exp(a)+exp(b)=c results snapshot: ");
//			for(int i = 0; i < 10; i++)
//				out.print(clBufferC.getBuffer().get() + ", ");
//			out.println("...; " + clBufferC.getBuffer().remaining() + " more");
//
//			out.println("computation took: "+(time2/1000000)+"ms");
//
//		}finally{
//			// cleanup all resources associated with this context.
//			context.release();
//		}
//
//
//
//	}
//
//	private static void fillBuffer(FloatBuffer buffer, int seed) {
//		Random rnd = new Random(seed);
//		while(buffer.remaining() != 0)
//			buffer.put(rnd.nextFloat());
//		buffer.rewind();
//	}
//
//	private static int roundUp(int groupSize, int globalSize) {
//		int r = globalSize % groupSize;
//		if (r == 0) {
//			return globalSize;
//		} else {
//			return globalSize + groupSize - r;
//		}
//	}
//
//	//    // OpenCL Kernel Function for element by element vector addition
//	private static String kernel = "kernel void VectorAdd(global const float* a, global const float* b, global float* c, int numElements) {" +
//			"        int iGID = get_global_id(0);" +
//			"        if (iGID >= numElements)  {" +
//			"            return;" +
//			"        }" +
////			" c[iGID] = exp(a[iGID])+exp(b[iGID]);"+
//			" float aa = 1 + a[iGID] / 256;" +
//			"aa *= aa; aa *= aa; aa *= aa; aa *= aa;" +
//			"aa *= aa; aa *= aa; aa *= aa; aa *= aa;" +
//			"float bb = 1 + b[iGID] / 256;" +
//			"bb *= bb; bb *= bb; bb *= bb; bb *= bb;" +
//			"bb *= bb; bb *= bb; bb *= bb; bb *= bb;" +
//			"        c[iGID] = aa + bb;" +
//			"    }";
//
//	public static void main(String [] args) throws IOException {
//		//		SpeedTest test = new SpeedTest();
//		//		for (int i = 0; i < 100; i++) {
//		//			test.testArrayList();
//		//			test.testLinkedList();
//		//			test.testArray();
//		//		}
//		SpeedTest.testOpenCL();
//	}
//
//}
