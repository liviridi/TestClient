package com.hpe.autoframework;

/**
 * 
 * Test Base class
 * All test class need to extends this base class.
 *
 */
public class TestBase {
	
	private static boolean forwardSet_ = false;

	public static boolean isForwardSet() {
		return forwardSet_;
	}

	public static void setForwardSet(boolean forwardSet) {
		forwardSet_ = forwardSet;
	}

	/**
	 * Set SSH port forwarding
	 * @param hostname host name to connect
	 * @param port port
	 * @param user login user
	 * @param password login password
	 * @param forwardinfo forwarding information
	 */
	public static void setForward(String hostname, int port, String user, String password, ForwardInfo forwardinfo) {
		SshClient forwardssh = new SshClient(false);
		forwardssh.connect(hostname, port, user, password);
		killForwardSsh(forwardssh, forwardinfo);
		int count = forwardinfo.size();
		for (int i = 0; i < count; i ++) {
			forwardssh.command("ssh -f -n -N -g -L " + forwardinfo.getPort(i) + ":localhost:" + forwardinfo.getForwardPort(i) + " " + forwardinfo.getForwardUser(i) + "@" + forwardinfo.getForwardHost(i));
		}
		forwardssh.close();
	}
	
	/**
	 * Unset SSH port forwarding
	 * @param hostname host name to connect
	 * @param port port
	 * @param user login user
	 * @param password login password
	 * @param forwardinfo forwarding information
	 */
	public static void closeForward(String hostname, int port, String user, String password, ForwardInfo forwardinfo) {
		SshClient forewardssh = new SshClient(false);
		forewardssh.connect(hostname, port, user, password);
		killForwardSsh(forewardssh, forwardinfo);
		forewardssh.close();
	}
	
	/**
	 * Kill port forwarding SSH
	 * @param forwardssh SSH client
	 * @param forwardinfo port forwarding info
	 */
	private static void killForwardSsh(SshClient forwardssh, ForwardInfo forwardinfo) {
		int count = forwardinfo.size();
		for (int i = 0; i < count; i ++) {
			int exitcode = forwardssh.execCommand("pkill -f 'ssh -f -n -N -g -L " + forwardinfo.getPort(i) + ":'", null);
			assert (exitcode == 0 || exitcode == 1) : "pkill ssh failed";
		}
	}
}
