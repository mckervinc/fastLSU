/*
 * Limiting the amount of RAM used to be 1/8 *
 * available RAM
*/
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
  public ArrayList<Double> solver() throws IOException {
    
    /*
     * Find the max array size - if its less, perform in memory parallel
     * procedure - else read from file
    */
    if (mtg <= limit()) {
      ExecutorService executor = Executors.newFixedThreadPool(numCores);
      List<Future<PValues>> list = new ArrayList<Future<PValues>>();
      PValues[] arr = load();
      for (int i = 0; i < numCores; i++)
        list.add(executor.submit(new WorkerThread(arr[i], mtg, alpha)));

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
      return finished(union);
    }
    else {
      FastBH fbh = new FastBH(filename, alpha, mtg);
      double[] condplusval = fbh.calculate();
      printFromFile(condplusval[0], condplusval[1]);
      return new ArrayList<Double>();
    }
  }

  /*****************************************************************************/
  /*                            Reading Functions                              */
  /*****************************************************************************/
  private PValues[] load() throws IOException {
    // local variables
    PValues[] result = new PValues[numCores];
    PValues elem = new PValues(chunks[0][1] - chunks[0][0] + 1);
    int pointer = 0, coreCount = 0, size = elem.size();
    double rci = 0.0, sum = 0.0;

    // read from file
    BufferedReader br = new BufferedReader(new FileReader(filename));
    String line;

    while ((line = br.readLine()) != null) {
      ArrayList<String> values = splitter(line);
      for (int i = 0; i < values.size(); i++) {
        if (pointer == size) {
          elem.prime = rci;
          result[coreCount] = elem;
          sum += rci;
          rci = 0.0;
          coreCount++;
          pointer = 0;
          elem = new PValues(chunks[coreCount][1] - chunks[coreCount][0] + 1);
          size = elem.size();
        }

        double p = Double.parseDouble(values.get(i));
        if (p < alpha) rci++;
        elem.array[pointer] = p;
        pointer++;
      }

      if (pointer == size) {
        elem.prime = rci;
        result[coreCount] = elem;
        pointer = 0;
        sum += rci;
        rci = 0.0;
        coreCount++;
        if (coreCount == numCores) break;
        elem = new PValues(chunks[coreCount][1] - chunks[coreCount][0] + 1);
        size = elem.size();
      }
    }

    rm.changeSum(0, sum);
    return result;
  }

  /*****************************************************************************/
  /*                           Printing Functions                              */
  /*****************************************************************************/

  public ArrayList<Double> finished(PValues[] list) {
    // printTable();
    ArrayList<Double> data = new ArrayList<Double>();
    double count = 0.0;
    for (int i = 0; i < list.length; i++) {
      for (int j = 0; j < list[i].array.length; j++) {
        if (count == mprime) {
          // check(count);
          return data;
        }

        double p = list[i].array[j];
        if (p != -1) {
          // System.out.printf((count%5==4) ? "%.3e\n" : "%.3e ", p);
          count++;
          data.add(p);
        }
      }
    }
    // check(count);
    return data;
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

  public void printFromFile(double condition, double count) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    String line;
    double c = 0.0;

    while((line=br.readLine()) != null) {
      ArrayList<String> values = splitter(line);
      for (int i = 0; i < values.size(); i++) {
        if (c == count) {
          check(count);
          return;
        }

        double p = Double.parseDouble(values.get(i));
        if (p < condition) {
          System.out.printf((c%5==4) ? "%.11f\n" : "%.11f ", p);
          c++;
        } 
      }
    }
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

  /*
   * By adding these three values, one gets approximately the maxheapsize set
   * by the JVM. Then, it is multiplied by 2/3 (experimental fraction), then
   * divided by 8 (because we are dealing with doubles) - this simplifies to 
   * dividing by 12. The 0.01 is accounting for approximation error.
  */
  private double limit() {
    long max = Runtime.getRuntime().maxMemory();
    long total = Runtime.getRuntime().totalMemory();
    long free = Runtime.getRuntime().freeMemory();
    long size = (max + total + free)/12;
    return size - 0.01 * size;
  }

  /*****************************************************************************/
  /*                           Helper Functions                                */
  /*****************************************************************************/

  private void check(double count) {
    System.out.print((count%5 == 0) ? "" : "\n");
  }

  private ArrayList<String> splitter(String line) {
    ArrayList<String> arr = new ArrayList<String>(5);
    boolean first = true;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      switch(c) {
        case '\t':
        case ' ':
          if (first) {
            arr.add(sb.toString());
            sb = new StringBuilder();
          }
          first = false;
          break;
        default:
          first = true;
          sb.append(c);
          break;
      }
    }
    arr.add(sb.toString());
    return arr;
  }
}