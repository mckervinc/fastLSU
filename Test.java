import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

public class Test {
	public static void main(String[] args) throws Exception {
		double m = 2 * Math.pow(10, 7);
		double alpha = 0.1;
		String path = "../Tests/test02.txt";
		FastBHConcurrent fbhc = new FastBHConcurrent(m, alpha, (new File(path)).getAbsolutePath());
		System.out.println("---------------------");
		System.out.println("# tests: " + m + " || alpha: " + alpha);
		long start = System.currentTimeMillis();
		ArrayList<Double> data = fbhc.solver();
		long end = System.currentTimeMillis();
		System.out.println("---------------------");
		System.out.println("Time: " + (end - start)/1000.0 + " second(s)");
		System.out.println("---------------------");

		PrintWriter pw = new PrintWriter((new File("../Results/test02r.txt")).getAbsolutePath(), "UTF-8");
		pw.println(m);
		for (int i = 0; i < data.size(); i++) {
			double p = data.get(i);
			pw.printf((i%5==4) ? "%.15f\n" : "%.15f ", p);
		}
		if (data.size()%5!=0) pw.println();
		pw.close();
	}
}