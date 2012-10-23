package gov.nrel.consumer.beans;


public class Config {


	private int scanInterval;
	private int broadcastInterval;
	private int range;
	private int numThreads;
	private String network;

	public Config(int scanInterval, int broadcastInterval, int range2, int numThreads, String network) {
		this.scanInterval = scanInterval;
		this.broadcastInterval = broadcastInterval;
		this.range = range2;
		this.numThreads = numThreads;
		this.network = network;
	}

	public String getNetwork() {
		return network;
	}
	public int getScanInterval() {
		return scanInterval;
	}

	public int getBroadcastInterval() {
		return broadcastInterval;
	}

	public int getRange() {
		return range;
	}

	public int getNumThreads() {
		return numThreads;
	}


//	public int deviceMatches(RemoteDevice t_rd) {
//		return oidMatches(t_rd, null);
//	}
//
//	public int oidMatches(RemoteDevice t_rd, ObjectIdentifier t_id) {
//		Vector<DeviceFilter> t_filters = getFilters();
//		if (t_filters == null) {
//			return getDefaultPollInterval();
//		}
//
//		for (DeviceFilter filter : t_filters) {
//			if (filter.matches(t_rd)) {
//				if (t_id == null) {
//					return filter.timeBetweenScans;
//				} else if (filter.oidFilters.isEmpty()) {
//					// if there's no oidfilters, accept all of them for this
//					// device
//					return filter.timeBetweenScans;
//				}
//				
//				for (OIDFilter oidFilter : filter.oidFilters) {
//					if (oidFilter.matches(t_id)) {
//						return oidFilter.timeBetweenScans;
//					}
//				}
//			}
//		}
//
//		return getDefaultPollInterval();
//	}	
}
