public class Launcher {
	public static void main(String[] args) throws Exception {
		// get the max heap size
		java.lang.management.OperatingSystemMXBean mxbean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
		com.sun.management.OperatingSystemMXBean sunmxbean = (com.sun.management.OperatingSystemMXBean) mxbean;
		long freePhysicalMemory = sunmxbean.getFreePhysicalMemorySize();
		long physicalMemory = sunmxbean.getTotalPhysicalMemorySize();
    	double cap = (double)physicalMemory * 0.3;
    	double mb = Math.round(Math.min(cap, freePhysicalMemory)/Math.pow(10, 6));

    	String opt = "-Xmx" + Double.toString(mb) + "m ";
    	ProcessBuilder pb = new ProcessBuilder("java", "-jar", opt, "fastlsu.jar");
    	pb.start();
    	
    	// FastLSU.launch(FastLSU.class, opt);
	}
}