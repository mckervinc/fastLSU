/*
 * Need to make this faster.
*/
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class FastBH {
  private String filename;
  private double alpha, m;

  public FastBH(String filename, double alpha, double m) {
    this.filename = filename;
    this.alpha = alpha;
    this.m = m;
  }
  
  public double[] calculate() throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    String line;
    double rci = 0.0;
    while ((line=br.readLine()) != null) {
      ArrayList<String> values = splitter(line);
      for (int i = 0; i < values.size(); i++) {
        double p = Double.parseDouble(values.get(i));
        if (p < alpha) rci++;
      }
    }

    br.close();
    return kmax(rci);
  }

  private double[] kmax(double rci) throws IOException {
    double condition = rci * alpha/m;

    while (true) {
      BufferedReader br = new BufferedReader(new FileReader(filename));
      String line;
      double rciprime = 0.0;

      while((line=br.readLine()) != null) {
        ArrayList<String> values = splitter(line);
        for (int i = 0; i < values.size(); i++) {
          double p = Double.parseDouble(values.get(i));
          if (p < condition) rciprime++;
        }
      }
      br.close();

      if (rciprime == rci) return new double[]{condition, rciprime};
      rci = rciprime;
      condition = rciprime * alpha/m;
    }
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