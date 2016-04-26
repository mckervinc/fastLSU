import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;

public class QV {
  public static ArrayList<Double> load(String filename) throws Exception {
    String line;
    ArrayList<Double> data = new ArrayList<Double>();
    BufferedReader br = new BufferedReader(new FileReader((new File(filename)).getAbsolutePath()));
    while((line=br.readLine()) != null) {
      String[] values = line.split(" ");
      for (int i = 0; i < values.length; i++)
        data.add(Double.parseDouble(values[i]));
    }
    return data;
  }

  public static double[] qValues(double m, ArrayList<Double> data) {
    Collections.sort(data);
    double[] res = new double[data.size()];
    double pr = data.get(data.size()-1);
    res[data.size()-1] =  pr*m/((double)data.size());

    for (int i = data.size()-2; i > -1; i--) {
      double pr_minus1 = data.get(i);
      double qr_minus1 = pr_minus1 * m / ((double) i+1);
      double qr = res[i+1];
      res[i] = (qr_minus1 < qr) ? qr_minus1 : qr;
    }
    return res;
  }

  public static void main(String[] args) throws Exception {
    // if (args.length == 0) {
    //   System.out.println("Please input a file that has the results of fastLSU!");
    //   return;
    // }

    // ArrayList<Double> data = new ArrayList<Double>();
    // String path = args[0], line = null;
    // BufferedReader br = new BufferedReader(new FileReader((new File(path)).getAbsolutePath()));
    // boolean first = true;
    // double m = 0.0;
    // while ((line=br.readLine()) != null) {
    //   if (first) {
    //     m = Double.parseDouble(line);
    //     first = false;
    //   }
    //   else {
    //     String[] values = line.split(" ");
    //     for (int i = 0; i < values.length; i++)
    //       data.add(Double.parseDouble(values[i]));
    //   }
    // }

    // Collections.sort(data);
    // double[] result = new double[data.size()];
    // double pr = data.get(data.size()-1);
    // result[data.size()-1] = pr * m/((double)data.size());

    // for (int i = data.size()-2; i > -1; i--) {
    //   double pr_minus1 = data.get(i);
    //   double qr_minus1 = pr_minus1 * m / ((double) i+1);
    //   double qr = result[i+1];
    //   if (qr_minus1 < qr) result[i] = qr_minus1;
    //   else result[i] = qr;
    // }

    // System.out.println("---P Values ---");
    // for (int i = 0; i < data.size(); i++) {
    //   System.out.printf((i%5==4) ? "%.3e\n" : "%.3e ", data.get(i));
    // }
    // if (data.size()%5!=0) System.out.println();

    // System.out.println("---Q Values ---");
    // for (int i = 0; i < result.length; i++) {
    //   System.out.printf((i%5==4) ? "%.3e\n" : "%.3e ", result[i]);
    // }
    // if (result.length%5!=0) System.out.println();

  }
}