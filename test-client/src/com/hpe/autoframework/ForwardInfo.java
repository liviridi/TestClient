package com.hpe.autoframework;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Class stores SSH porting forwarding information
 *
 */
public class ForwardInfo {
	protected class Info {
		String forwardHost_;
		int port_;
		int forwardPort_;
		String forwardUser_;
		
		Info(int port, String forewardHost, int forewardPort, String forwardUser) {
			port_ = port;
			forwardHost_ = forewardHost;
			forwardPort_ = forewardPort;
			forwardUser_ = forwardUser;
		}
	}
	
	/**
	 * Info array
	 */
	protected List<Info> info_ = new ArrayList<Info>();
	
	/**
	 * Constructor
	 */
	public ForwardInfo() {
	}

	/**
	 * Constructor
	 * @param port port
	 * @param forwardHost forwarding host name
	 * @param forwardPort forwarding port
	 */
	public ForwardInfo(int port, String forwardHost, int forwardPort, String forwardUser) {
		info_.add(new Info(port, forwardHost, forwardPort, forwardUser));
	}
	
	/**
	 * Add forwarding info
	 * @param port port
	 * @param forwardHost forwarding host name
	 * @param forwardPort forwarding port
	 */
	public void addInfo(int port, String forwardHost, int forwardPort, String forwardUser) {
		info_.add(new Info(port, forwardHost, forwardPort, forwardUser));
	}
	
	/**
	 * Get number of info
	 * @return number of info
	 */
	public int size() {
		return info_.size();
	}
	
	/**
	 * Get port
	 * @param index index of info
	 * @return port
	 */
	public int getPort(int index) {
		Info info = info_.get(index);
		if (info == null)
			return -1;
		return info.port_;
	}

	/**
	 * Get forwarding host name
	 * @param index index of info
	 * @return forwarding host name
	 */
	public String getForwardHost(int index) {
		Info info = info_.get(index);
		if (info == null)
			return null;
		return info.forwardHost_;
	}
	
	/**
	 * Get forwarding port
	 * @param index index of info
	 * @return forwarding port
	 */
	public int getForwardPort(int index) {
		Info info = info_.get(index);
		if (info == null)
			return -1;
		return info.forwardPort_;
	}
	
	/**
	 * Get forwarding user name
	 * @param index index of info
	 * @return forwarding user name
	 */
	public String getForwardUser(int index) {
		Info info = info_.get(index);
		if (info == null)
			return null;
		return info.forwardUser_;
	}

}
