/*
 * Performs fastLSU in parallel over the read data.
*/
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
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

  private int numCores, idx, format;
  private double[][] chunks;
  private double alpha, mtg, mprime, bigSize, fileSize;
  private String filename;
  private PrintWriter tempFile;
  private BufferedReader buf;
  private ArrayList<String> prevLine;
  public ResourceMonitor rm;

  public FastBHConcurrent(double mtg, double alpha, String filename) {
    // Find the number of cores available to the machine
    numCores = Runtime.getRuntime().availableProcessors();
    this.mtg = mtg;
    bigSize = mtg;
    this.alpha = alpha;
    this.filename = filename;
    rm = new ResourceMonitor(numCores);
    fileSize = 0.0;
    chunks = new double[numCores][2];
    split(mtg);
    format = 0;
  }

  /*****************************************************************************/
  /*                            BH Calculations                                */
  /*****************************************************************************/
  public final ArrayList<Double> solver(PValues[] arr) throws IOException {
    
    // start each thread with the given data
    ExecutorService executor = Executors.newFixedThreadPool(numCores);
    List<Future<PValues>> list = new ArrayList<Future<PValues>>();
    for (int i = 0; i < numCores; i++)
      list.add(executor.submit(new WorkerThread(arr[i], mtg, alpha, "pool-1-thread-" + str(i+1), rm)));

    // get the result
    PValues[] union = new PValues[numCores];
    int i = 0;
    mprime = 0.0;
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

  public final ArrayList<Double> solverInMem(PValues[] arr) throws IOException {
    // get current number of tests
    double tests = 0.0;
    for (int i = 0; i < arr.length; i++)
      tests += arr[i].array.length;

    // start the processing using algorithm 1
    // start each thread with the given data
    ExecutorService executor = Executors.newFixedThreadPool(arr.length);
    List<Future<PValues>> list = new ArrayList<Future<PValues>>();
    for (int i = 0; i < arr.length; i++)
      list.add(executor.submit(new MemoryThread(arr[i], mtg, alpha)));

    // get the result
    PValues[] union = new PValues[arr.length];
    int i = 0;
    mprime = 0.0;
    for (Future<PValues> future : list) {
      try {
        union[i] = future.get();
        mprime += union[i].prime;
        i++;
      }
      catch (Exception e) {e.printStackTrace();break;}
    }
    executor.shutdown();

    // append the result to file
    fileSize += mprime;
    append(union);
    bigSize -= tests;
    if (bigSize == 0.0) {
      buf.close();
      tempFile.close();
      arr = null;
      union = null;
      buf = new BufferedReader(new FileReader("tmp.txt"));
      chunks[0][0] = 0;
      chunks[0][1] = fileSize-1;
      double lim = limit();

      // if fileSize <= limit, perform algorithm 1 and return the results
      if (fileSize <= lim) {
        arr = reader((new PValues[1]), 0, 0, 1);
        buf.close();
        (new File("tmp.txt")).delete();
        return alg1(arr, fileSize);
      }
      else {
        arr = skipper(fileSize, lim);
        ArrayList<Double> result = alg2(arr, fileSize, lim);
        (new File("tmp.txt")).delete();
        return result;
      }
    }
    return null;
  }

  private ArrayList<Double> alg1(PValues[] arr, double m) {
    double condition = arr[0].prime * alpha/m;
    double rci = m;

    while (true) {
      double rciprime = kmax(arr, condition);
      if (rci == rciprime) {
        mprime = rciprime;
        break;
      }
      condition = rciprime * alpha/m;
      rci = rciprime;
    }

    return finished(arr);
  }

  private ArrayList<Double> alg2(PValues[] arr, double fs, double lim)
  throws IOException {
    double condition = alpha * fs/mtg;
    double rci = fs;

    while(true) {
      double rciprime = kmax2(arr, condition, fs, lim);
      if (rciprime == rci) {
        if (rciprime <= lim) return fill(condition, rciprime);
        else {
          writeToFile(condition, rciprime);
          return null;
        }
      }
      condition = rciprime * alpha/mtg;
      rci = rciprime;
    }
  }

  private double kmax(PValues[] arr, double condition) {
    double count = 0.0;
    for (int i = 0; i < arr.length; i++) {
      for (int j = 0; j < arr[i].array.length; j++) {
        double p = arr[i].array[j];
        if (p != -1) {
          if (p < condition) count++;
          else arr[i].array[j] = -1;
        }
      }
    }
    return count;
  }

  private double kmax2(PValues[] arr, double condition, double fileSize, double lim)
  throws IOException {
    // Read the first things not in the PValues array
    String line;
    double fs = fileSize, count = 0.0;
    buf = new BufferedReader(new FileReader("tmp.txt"));
    while((line=buf.readLine()) != null) {
      ArrayList<String> values = splitter(line);
      for (int i = 0; i < values.size(); i++) {
        if (fs > lim) {
          if (Double.parseDouble(values.get(i)) < condition) count++;
          fs--;
        }
        else break;
      }

      if (fs == lim) break;
    }
    buf.close();

    return count + kmax(arr, condition);
  }

  /*****************************************************************************/
  /*                            Reading Functions                              */
  /*****************************************************************************/

  // default load, when the entire thing fits in memory
  public final PValues[] load() throws IOException {
    buf = new BufferedReader(new FileReader(filename));
    PValues[] result = reader((new PValues[numCores]), 0, 0, numCores);
    buf.close();
    return result;
  }

  // this is used when there isn't enough memory
  public final PValues[] loadInMem() throws IOException {
    // split the size into appropriate cores
    double lim = limit();
    int allCores = numCores;
    if (bigSize > lim) split(lim);
    else if (bigSize < numCores) {
      allCores = 1;
      chunks[0][0] = 0;
      chunks[0][1] = bigSize-1;
    }
    else split(bigSize);

    // lets do the loading - if the prevLine is null, we are at the beginning
    if (buf == null) {
      buf = new BufferedReader(new FileReader(filename));
      return reader((new PValues[allCores]), 0, 0, numCores); 
    }

    // if the prevLine != null, finish processing said line
    if (prevLine != null) {
      PValues[] result = new PValues[allCores];
      PValues elem = new PValues(chunks[0][1] - chunks[0][0] + 1);
      int size = elem.size(), pointer = 0, coreCount = 0;
      double sum = 0.0, rci = 0.0;

      ArrayList<String> values = prevLine;
      for (int i = idx; i < values.size(); i++) {
        if (pointer == size) {
          elem.prime = rci;
          result[coreCount] = elem;
          sum += rci;
          rci = 0.0;
          coreCount++;
          if (coreCount == allCores) {
            prevLine = values;
            idx = i;
            rm.changeSum(0, sum);
            return result;
          }
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
        if (coreCount == allCores) {
          prevLine = null;
          idx = 0;
          rm.changeSum(0, sum);
          return result;
        }
        else return reader(result, coreCount, 0, numCores);
      }
      else return reader(result, coreCount, pointer, allCores);
    }
    
    // continue reading in
    return reader((new PValues[allCores]), 0, 0, allCores);
  }

  // go through the file, checking for values less than alpha and return the tests
  private final PValues[] reader(PValues[] result, int coreCount, int pointer, int allCores)
  throws IOException {
    // local variables
    PValues elem = new PValues(chunks[coreCount][1] - chunks[coreCount][0] + 1);
    double rci = 0.0, sum = 0.0, p = 0.0, oldrci = 0.0;

    String line;
    while((line=buf.readLine()) != null) {
      ArrayList<String> values = splitter(line);
      for (int i = 0; i < values.size(); i++) {
        try {
          p = Double.parseDouble(values.get(i));
          oldrci = rci;
          if (p < alpha) rci++;
          elem.array[pointer] = p;
          pointer++;
        }
        catch (ArrayIndexOutOfBoundsException e) {
          if (rci != oldrci) {
            elem.prime = oldrci;
            sum += oldrci;
            rci = 1.0;
          }
          else {
            elem.prime = rci;
            sum += rci;
            rci = 0.0;
          }
          result[coreCount] = elem;
          oldrci = rci;
          coreCount++;
          if (coreCount == allCores) {
            prevLine = values;
            idx = i;
            rm.changeSum(0, sum);
            return result;
          }
          pointer = 1;
          elem = new PValues(chunks[coreCount][1] - chunks[coreCount][0] + 1);
          elem.array[0] = p;
        }
      }
    }
  
    elem.prime = rci;
    result[coreCount] = elem;
    sum += rci;
    prevLine = null;
    idx = 0;
    rm.changeSum(0, sum);
    return result;
  }

  // skips the first fileSize - lim values, returns the rest in memory
  private final PValues[] skipper(double fileSize, double lim) throws IOException {
    // skip the first few numbers, then read limit() sized numbers
    // until the end of "tmp.txt" is reached
    PValues result = new PValues(lim);
    double fs = fileSize;
    int pointer = 0;
    String line;
    while((line=buf.readLine()) != null) {
      ArrayList<String> values = splitter(line);
      for (int i = 0; i < values.size(); i++) {
        if (fs == lim) {
          double p = Double.parseDouble(values.get(i));
          result.array[pointer] = p;
          pointer++;   
        }
        else fs--;
      }
      if (pointer == lim) break;
    }
    buf.close();
    return new PValues[]{result};
  }

  /*****************************************************************************/
  /*                           Printing Functions                              */
  /*****************************************************************************/

  // if fitsInMem, return simple ArrayList
  private ArrayList<Double> finished(PValues[] list) {
    ArrayList<Double> data = new ArrayList<Double>();
    double count = 0.0;
    for (int i = 0; i < list.length; i++) {
      for (int j = 0; j < list[i].array.length; j++) {
        if (count == mprime) return data;
        
        double p = list[i].array[j];
        if (p != -1) {
          count++;
          data.add(p);
        }
      }
    }
    return data;
  }

  // fill results from alg2 into memory
  private ArrayList<Double> fill(double condition, double count) throws IOException {
    buf = new BufferedReader(new FileReader("tmp.txt"));
    String line;
    double c = 0.0;
    ArrayList<Double> data = new ArrayList<Double>();
    while((line=buf.readLine()) != null) {
      ArrayList<String> values = splitter(line);
      for (int i = 0; i < values.size(); i++) {
        if (c == count) {
          buf.close();
          return data;
        }

        double p = Double.parseDouble(values.get(i));
        if (p < condition) {
          data.add(p);
          c++;
        }
      }

      if (c == count) break;
    }

    buf.close();
    return data;
  }

  // append to temporary file
  private void append(PValues[] list) throws IOException {
    // initialize log file
    if (tempFile == null) tempFile = new PrintWriter(new BufferedWriter(new FileWriter("tmp.txt", true)));

    double count = 0.0;
    for (int i = 0; i < list.length; i++) {
      for (int j = 0; j < list[i].array.length; j++) {
        if (count == mprime) return;

        double p = list[i].array[j];
        if (p != -1) {
          tempFile.printf((format%5==4) ? "%.3e\n" : "%.3e ", p);
          count++;
          format++;
        }
      }
    }
  }

  // if, after having done everything, the result is STILL TOO BIG
  private void writeToFile(double condition, double count) throws IOException {
    // Read the first things not in the PValues array
    String line;
    double c = 0.0;
    File f = new File("tmp2.txt");
    if (f.exists()) f.delete();
    tempFile = new PrintWriter(new BufferedWriter(new FileWriter("tmp2.txt", true)));
    buf = new BufferedReader(new FileReader("tmp.txt"));

    while((line=buf.readLine()) != null) {
      ArrayList<String> values = splitter(line);
      for (int i = 0; i < values.size(); i++) {
        if (c == count) {
          tempFile.printf((c%5!=4) ? "\n" : "");
          return;
        }

        double p = Double.parseDouble(values.get(i));
        if (p < condition) {
          tempFile.printf((c%5==4) ? "%.3e\n" : "%.3e ", p);
          c++;
        }
      }

      if (c == count) {
        tempFile.printf((c%5!=4) ? "\n" : "");
        break;
      }
    }

    tempFile.close();
    buf.close();
  }

  /*****************************************************************************/
  /*                          Chunking Functions                               */
  /*****************************************************************************/

  // Split array indeces into the different chunks
  private void split(double size) {
    for (int i = 0; i < numCores; i++) {
      chunks[i][0] = Math.floor((size * i) / numCores);
      chunks[i][1] = Math.floor((size * (i + 1)) / numCores - 1);
    }
  }

  // Find the max array size that fits in memory
  private double limit() {
    double max = (double)Runtime.getRuntime().maxMemory()/10.0;
    return max - 0.1 * max;
  }

  /*****************************************************************************/
  /*                           Helper Functions                                */
  /*****************************************************************************/

  private final ArrayList<String> splitter(String line) {
    line = line.trim();
    ArrayList<String> arr = new ArrayList<String>();
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

  private String str(int x) {
    return Integer.toString(x);
  }

  public boolean fitsInMem() {
    return mtg <= limit();
  }

  public boolean isFinished() {
    return bigSize == 0.0;
  }

  public void cSum(double sum) {
    rm.changeSum(0, sum);
  }
}