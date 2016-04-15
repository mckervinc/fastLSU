# fastLSU
Note that this is a work-in-progress. When this is finished, this will
allow researchers to perform the fastLSU program in parallel on individual machines.
See the paper in this repository for more information on the algorithm itself.

The main program begins with FastBHConcurrent.java - from there a ResourceMonitor
monitors the number of p-values in each instance of the WorkerThread. The threads
then return PValues, which hold the final result.

--Simplified WorkerThread to prevent parallel disk reads on the same file (this
caused too many seeks on a given hard-drive)
--Added small performance tweaks
--Must alter FastBH for low-memory machines and/or tests on the order of 10^9.