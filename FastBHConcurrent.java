/*
 * Limiting the amount of RAM used to be 1/8 *
 * available RAM
*/
import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FastBHConcurrent{

  private int numCores;
  private double[][] chunks;
  private double alpha, mtg, mprime = 0.0;
  private String filename;
  public static ResourceMonitor rm;

  public FastBHConcurrent(double mtg, double alpha, String filename) {
    // Find the number of cores available to the machine
    numCores = Runtime.getRuntime().availableProcessors();

    // The total size of the input across all potential cores
    this.mtg = mtg;

    // alpha value
    this.alpha = alpha;

    // file name
    this.filename = filename;

    // status flags
    rm = new ResourceMonitor(numCores);

    // given the numCores, split the array into cores based on chunks
    // the second part of the array is the lo and hi index
    chunks = new double[numCores][2];
    split(mtg);
  }

  /*****************************************************************************/
  /*                            BH Calculations                                */
  /*****************************************************************************/
  public void solver() {
    // Find the max number of doubles to be used in a given sequence
    // If the input is greater than the max, then further split up
    // each chunk into smaller, arbitrary sized chunks. The max is the
    // largest the particular machine can handle
    double max = limit();
    
    ExecutorService executor = Executors.newFixedThreadPool(numCores);
    List<Future<PValues>> list = new ArrayList<Future<PValues>>();
    double[] d = new double[]{max/(double)numCores, mtg, alpha};
    for (int i = 0; i < numCores; i++)
      list.add(executor.submit(new WorkerThread(chunks[i], d, filename)));

    // get the result
    PValues[] union = new PValues[numCores];
    int i = 0;
    for (Future<PValues> future : list) {
      try {
        union[i] = future.get();
        mprime += union[i].prime;
        i++;
      }
      catch (Exception e) {e.printStackTrace();break;}
    }
    executor.shutdown();

    // return value
    finished(union);
  }

  /*****************************************************************************/
  /*                           Helper Functions                                */
  /*****************************************************************************/

  public void finished(PValues[] list) {
    printTable();
    double count = 0.0;
    for (int i = 0; i < list.length; i++) {
      for (int j = 0; j < list[i].array.length; j++) {
        if (count == mprime) {
          check(count);
          return;
        }

        double p = list[i].array[j];
        if (p != -1) {
          System.out.printf((count%5==4) ? "%.11f\n" : "%.11f ", p);
          count++;
        }
      }
    }
    check(count);
  }

  public void printTable() {
    Iterator<String> it = rm.iterator();
    System.out.println("--------------------------------------------");
    while (it.hasNext()) {
      String key = it.next();
      double val = rm.getValue(key);
      int step = rm.getStep(key) + 1;
      System.out.printf("%s: || Step: %d || value: %f\n", key, step, val);
    }
    System.out.println("--------------------------------------------");
  }

  // helper function for combining the results of the parallel
  private void unionLSU(PValues[] list) {
    printTable();
    double r = mprime;
    while (true) {
      double rprime = kmaxR(list, r);
      if (r == rprime) {
        double count = 0.0;
        for (int i = 0; i < list.length; i++) {
          for (int j = 0; j < list[i].array.length; j++) {
            // base case
            if (count == r) {
              check(count);
              return;
            }

            // print the results
            double p = list[i].array[j];
            if (p != -1) {
              System.out.printf((count%5==4) ? "%.11f\n" : "%.11f ", p);
              count++;
            }
          }
        }
        check(r);
        break;
      }
      r = rprime;
    }
  }

  private double kmaxR(PValues[] list, double r) {
    double count = 0;
    double cond = alpha * r/mprime;
    for (int i = 0; i < list.length; i++) {
      for (int j = 0; j < list[i].array.length; j++) {
        double p = list[i].array[j];
        if (p != -1 && p < cond) count++;
        else list[i].array[j] = -1;
      }
    }
    return count;
  }

  /*****************************************************************************/
  /*                          Chunking Functions                               */
  /*****************************************************************************/

  // Split array indeces into the different chunks
  private void split(double size) {
    for (int i = 0; i < numCores; i++) {
      chunks[i][0] = (size * i) / numCores;
      chunks[i][1] = (size * (i + 1)) / numCores - 1;
    }
  }

  // returns max number of doubles in a given chunk
  private double limit() {
    long memorySize = ((com.sun.management.OperatingSystemMXBean)
        ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
    return Math.ceil((memorySize/8.0 - 12.0) / 8.0);
  }

  /*****************************************************************************/
  /*                           Helper Functions                                */
  /*****************************************************************************/

  private void check(double count) {
    System.out.print((count%5 == 0) ? "" : "\n");
  }
}