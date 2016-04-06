import java.io.*;

public class TimingSlow {
	
	public static void printArr(double[] res) {
		for (int i = 0; i < res.length; i++)
			System.out.printf((i%5==4) ? "%.3e\n" : "%.3e ", res[i]);
		System.out.printf((res.length%5!=0) ? "\n": "");
	}

	public static void main(String[] args) throws Exception {
		int trials = Integer.parseInt(args[0]);
		int m = Integer.parseInt(args[2]);
		int pointer = 0;

		double alpha = Double.parseDouble(args[1]);
		double[] data = new double[m];
		double time = 0;

		String line;

		for (int i = 0; i < trials; i++) {
			BufferedReader br = new BufferedReader(new FileReader((new File(args[3])).getAbsolutePath()));
			long start = System.currentTimeMillis();
			while ((line=br.readLine()) != null) {
				String[] values = line.split(" ");
				for (int j = 0; j < values.length; j++) {
					data[pointer] = Double.parseDouble(values[j]);
					pointer++;			
				}
			}
			SlowBH sbh = new SlowBH(data, alpha);
			double [] res = sbh.kmax();
			long end = System.currentTimeMillis();
			// printArr(res);
			double timeinS = (double)(end - start);
			time += timeinS;
			br.close();
			pointer = 0;
		}
		double ms = time/((double)trials);
		System.out.printf("Average run time over %d Trials: %.2f ms, or %.8f s\n", trials, ms, ms/1000.0);
	}
}