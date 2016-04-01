/******************************************************************************
 * numCores instancces of this class are instantiated. Overall, this file
 * does the algorithm - it performs linear scans over the entire chunk,
 * returns a new chunk that only contains significant p-values
 ******************************************************************************/
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Callable;

public class WorkerThread implements Callable<PValues> {
  private double start_index, mci, max, m, alpha;
  private String filename, threadName;
  private static ResourceMonitor rm = FastBHConcurrent.rm;

  // constructor
  public WorkerThread(double[] indices, double[] val, String filename) {
    start_index = indices[0];
    mci = indices[1] - start_index + 1.0;
    max = val[0];
    m = val[1];
    alpha = val[2];
    this.filename = filename;
  }

  /*****************************************************************************/
  /*                            Main Functions                                 */
  /*****************************************************************************/

  /*
    The call() function is part of the Callable class. Gets called automatically
    while in parallel. This will ultimately perform the algorithm
  */
  public PValues call() throws Exception {
    threadName = Thread.currentThread().getName();
    
    // pass 1 - find r0 (p-values less than alpha)
    try {
      BufferedReader br = new BufferedReader(new FileReader(filename));
      String line;
      int k = 0;
      int skip = (int) (start_index/5.0);
      while ((line = br.readLine()) != null) {
        String[] values = line.split(" ");
        start_index -= values.length;
        if (k == skip) {
          start_index += values.length;
          if (mci <= max) return calculate(new PValues(mci), br, line, mci);
          else            return calculate(new PValues(max), br, line, max);
        }
        k++;
      }
    }
    catch (Exception e) {e.printStackTrace();}

    // this should be an error
    System.out.println(threadName);
    return null;  
  }

  /*
    Main function: calculates the number of significant p-values in the chunk
    of size mci.
  */
  private PValues calculate(PValues list, BufferedReader br, String line, double end) {
    int pointer = 0;
    double rci = 0.0;
    PValues result = new PValues(0.0);
    String[] values;
    boolean f = false;

    // read the data in, and perform the calculation
    try {
      do {
        values = line.split(" ");
        for (int i = (int)start_index; i < values.length; i++) {
          // base cases
          if (pointer == end) {
            result = result.concatenate(kmax(rci, list));
            if (end == mci) {
              f = true;
              break;
            }
            if (end + max <= mci) {
              list = new PValues(max);
              end += max;
            }
            else {
              list = new PValues(mci - end);
              end = mci;
            }

            // reset some parameters
            rci = 0.0;
            f = false;
          }

          // calculate rci && fill object (array)
          double n = Double.parseDouble(values[i]);
          if (n < alpha) rci++;
          list.array[pointer] = n;
          pointer++;
        }
        start_index = 0;

        // make sure if you reach the end that the final thing is calculated
        if (!f && pointer == end) {
          notifier(0, rci);
          PValues c = kmax(rci, list);
          result = result.concatenate(c);
        }
      } while (pointer < end && (line = br.readLine()) != null);
      br.close();
    }
    catch (Exception e) {e.printStackTrace();}
    return result;
  }

  /*****************************************************************************/
  /*                           Signal Processing                               */
  /*****************************************************************************/

  private double cumSum(int step, double rci) {
    double sum = rci;
    for (Iterator<String> it = rm.iterator(); it.hasNext();) {
      String x = it.next();
      if (!x.equals(threadName)) {
        try {
          while (rm.getStep(x) != step) {
            if (rm.sumStep() == step) {
              notifier(step, rci);
              return rm.sum();
            }
          }
          sum += rm.getValue(x);
        }
        catch (NullPointerException e) {}
      }
    }
    notifier(step, rci);
    if (rm.sumStep() < step) rm.changeSum(step, sum);
    return sum;
  }

  private void notifier(int step, double rci) {
    rm.update(threadName, step, rci);
  }

  /*****************************************************************************/
  /*                            BH Calculations                                */
  /*****************************************************************************/

  //Performs the algorithm. Once the kth step == k+1th step, break  
  private PValues kmax(double rci, PValues list) {
    
    double sum1 = cumSum(0, rci);
    double condition = sum1 * alpha/m;
    int step = 1;

    while(true) {
      double rciprime = counter(condition, list, step);
      if (rciprime == 0.0) {
        rm.remove(threadName);
        return new PValues(0);
      }
      double sum2 = cumSum(step, rciprime);
      if (sum1 == sum2 || step == m) {
        list.prime = rciprime;
        break;
      }
      condition = sum2 * alpha/m;
      sum1 = sum2;
      step++;
    }

    return list;
  }

  /*
    Goes through the array, checking if the pvalues support the condition of
    being < alpha * kmax.r/m
  */
  private double counter(double cond, PValues list, int step) {
    double count = 0.0;
    for (int i = 0; i < list.array.length; i++) {
      double p = list.array[i];
      if (p != -1 && p < cond) count++;
      else list.array[i] = -1;
    }
    notifier(step, count);
    return count;
  }
}