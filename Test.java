import java.io.File;

public class Test {
	public static void main(String[] args) {
		double m = Math.pow(10, 7);
		double alpha = 0.1;
		String path = "../Tests/test01.txt";
		FastBHConcurrent fbhc = new FastBHConcurrent(m, alpha, (new File(path)).getAbsolutePath());
		System.out.println("--------------------------------------------");
		System.out.println("# tests: " + m + " || alpha: " + alpha);
		long start = System.currentTimeMillis();
		fbhc.solver();
		long end = System.currentTimeMillis();
		System.out.println("---------------------");
		System.out.println("Time: " + (end - start)/1000.0 + " second(s)");
		System.out.println("---------------------");
	}
}