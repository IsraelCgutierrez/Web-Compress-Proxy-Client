package ClientP;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

public class Client2Server implements Runnable {

	private final String VERSION = "0.4.80b1";
	private final String HTTP_VERSION = "HTTP/1.1";

	private final String MAIN_LOGFILE = "client.log";
	private final String DATA_FILE = "client.data";
	private final String SERVER_PROPERTIES_FILE = "client.properties";

	// private String httpUserAgent =
	// "Mozilla/4.0 (compatible; MSIE 5.0; WindowsNT 5.1)";
	private ServerSocket listen;
	private BufferedWriter logFile;
	private BufferedWriter accessLogFile;
	private Properties clientproperties = null;

	private volatile long bytesread;
	private volatile long byteswritten;
	private volatile int numconnections;

	private boolean enable_cookies_by_default = true;
	private WildcardDictionary dic = new WildcardDictionary();
	private Vector<OnURLAction> urlactions = new Vector<OnURLAction>();

	public final int DEFAULT_SERVER_PORT = 8088;
	public final String WEB_CONFIG_FILE = "admin/jp2-config";

	public int port = DEFAULT_SERVER_PORT;
	public InetAddress proxy;
	public int proxy_port = 0;

	public boolean fatalError;
	private String errorMessage;
	private boolean serverRunning = false;

	public boolean useProxy = false;
	public boolean debug = false;
	// public boolean log_access = true;
	// public String log_access_filename = "access.log";
	public int MatchDistance = 1000;
	public float MatchThreshold = (float) 0.8;

	void init() {
		// create new BufferedWriter instance for logging to file
		try {
			logFile = new BufferedWriter(new FileWriter(MAIN_LOGFILE, true));
		} catch (Exception e_logfile) {
			setErrorMsg("Unable to open the main log file.");
			if (logFile == null)
				setErrorMsg("Proxy client need write permission for the file "
						+ MAIN_LOGFILE);
			errorMessage += " " + e_logfile.getMessage();
		}
		writeLog("Proxy Client server startup...");

		// restore settings from file. If this fails, default settings will be
		// used
		restoreSettings();

		// create now server socket
		try {
			listen = new ServerSocket(port);
		} catch (BindException e_bind_socket) {
			setErrorMsg("The socket " + port
					+ " is already in use (Another proxy running?) "
					+ e_bind_socket.getMessage());
		} catch (IOException e_io_socket) {
			setErrorMsg("IO Exception occured while creating server socket on port "
					+ port + ". " + e_io_socket.getMessage());
		}

		if (fatalError) {
			writeLog(errorMessage);
			return;
		}

	}

	public Client2Server() {
		init();
	}

	public Client2Server(boolean b) {
		System.out.println("Starting...");
		init();
	}

	/**
	 * calls init(), sets up the server port and starts for each connection new
	 * ClientConnection
	 */
	void serve() {

		serverRunning = true;
		writeLog("Server running on port " + this.port);
		try {
			while (serverRunning) {
				Socket client = listen.accept();
				new Client2HTTPSession(this, client);
			}
		} catch (Exception e) {
			e.printStackTrace();
			writeLog("Exception in ClientServer.serve(): " + e.toString());
		}
	}

	@Override
	public void run() {
		serve();
	}

	public void setErrorMsg(String a) {
		fatalError = true;
		errorMessage = a;
	}

	/**
	 * Tests what method is used with the reqest
	 * 
	 * @return -1 if the server doesn't support the method
	 */
	public int getHttpMethod(String d) {
		if (startsWith(d, "GET") || startsWith(d, "HEAD"))
			return 0;
		if (startsWith(d, "POST") || startsWith(d, "PUT"))
			return 1;
		if (startsWith(d, "CONNECT"))
			return 2;
		if (startsWith(d, "OPTIONS"))
			return 3;

		return -1;/*
				 * No match...
				 * 
				 * Following methods are not implemented: ||
				 * startsWith(d,"TRACE")
				 */
	}

	public boolean startsWith(String a, String what) {
		int l = what.length();
		int l2 = a.length();
		return l2 >= l ? a.substring(0, l).equals(what) : false;
	}

	/**
	 * @return the Server response-header field
	 */
	public String getServerIdentification() {
		return "Client/" + getServerVersion();
	}

	public String getServerVersion() {
		return VERSION;
	}

	/**
	 * saves all settings with a ObjectOutputStream into a file
	 * 
	 */
	public void saveSettings() {

		Boolean propertiesFileSaved = false;
		Boolean objectFileSaved = false;

		if (clientproperties == null)
			return;

		clientproperties.setProperty("client.http-proxy",
				new Boolean(useProxy).toString());
		clientproperties.setProperty("client.http-proxy.hostname",
				proxy.getHostAddress());
		clientproperties.setProperty("client.http-proxy.port", new Integer(
				proxy_port).toString());
		// serverproperties.setProperty("client.filter.http.useragent",
		// httpUserAgent);
		clientproperties.setProperty("client.enable-cookies-by-default",
				new Boolean(enable_cookies_by_default).toString());
		clientproperties.setProperty("client.debug-logging",
				new Boolean(debug).toString());
		clientproperties.setProperty("client.port",
				new Integer(port).toString());
		// serverproperties.setProperty("client.access.log", new Boolean(
		// log_access).toString());
		// serverproperties.setProperty("client.access.log.filename",
		// log_access_filename);
		clientproperties
				.setProperty("client.MatchDistance", MatchDistance + "");
		clientproperties.setProperty("client.MatchThreshold", MatchThreshold
				+ "");

		try {
			clientproperties.store(
					new FileOutputStream(SERVER_PROPERTIES_FILE),
					"Client Proxy properties");
			propertiesFileSaved = true;
		} catch (IOException IOExceptProperties) {
			writeLog("storeServerProperties(): "
					+ IOExceptProperties.getMessage());
		}

		try {
			ObjectOutputStream file = new ObjectOutputStream(
					new FileOutputStream(DATA_FILE));
			file.writeObject(dic);
			file.writeObject(urlactions);
			file.close();
			objectFileSaved = true;
		} catch (IOException IOExceptObjectStream) {
			writeLog("storeServerProperties(): "
					+ IOExceptObjectStream.getMessage());
		}

		if (objectFileSaved && propertiesFileSaved)
			writeLog("Configuration saved successfully");
		else
			writeLog("Failure during saving server properties or object stream");

	}

	/**
	 * restores all options from the configuration file
	 * 
	 */
	public void restoreSettings() {
		Boolean propertiesFileLoaded = false;
		Boolean objectFileLoaded = false;

		if (clientproperties == null) {
			clientproperties = new Properties();
			try {
				clientproperties.load(new DataInputStream(new FileInputStream(
						SERVER_PROPERTIES_FILE)));
				propertiesFileLoaded = true;
			} catch (IOException e) {
				writeLog("getServerProperties(): " + e.getMessage());

			}
		}

		useProxy = new Boolean(clientproperties.getProperty(
				"client.http-proxy", "false")).booleanValue();
		try {
			proxy = InetAddress.getByName(clientproperties.getProperty(
					"client.http-proxy.hostname", "127.0.0.1"));
		} catch (UnknownHostException e) {
		}
		proxy_port = new Integer(clientproperties.getProperty(
				"client.http-proxy.port", "8080")).intValue();
		// httpUserAgent = serverproperties.getProperty(
		// "client.filter.http.useragent",
		// "Mozilla/4.0 (compatible; MSIE 4.0; WindowsNT 5.0)");
		enable_cookies_by_default = new Boolean(clientproperties.getProperty(
				"client.enable-cookies-by-default", "true")).booleanValue();
		debug = new Boolean(clientproperties.getProperty(
				"client.debug-logging", "false")).booleanValue();
		port = new Integer(clientproperties.getProperty("client.port", "8088"))
				.intValue();
		// log_access = new Boolean(serverproperties.getProperty(
		// "client.access.log", "true")).booleanValue();
		// log_access_filename = serverproperties.getProperty(
		// "client.access.log.filename", "access.log");
		MatchDistance = new Integer(clientproperties.getProperty(
				"client.MatchDistance", "1000"));
		MatchThreshold = new Float(clientproperties.getProperty(
				"client.MatchThreshold", "0.8"));

		try {

			// accessLogFile = new BufferedWriter(new FileWriter(
			// log_access_filename, true));
			// Restore the WildcardDioctionary and the URLActions with the
			// ObjectInputStream (settings.dat)...
			ObjectInputStream objInputStream;
			File file = new File(DATA_FILE);
			if (file.exists()) {
				objInputStream = new ObjectInputStream(
						new FileInputStream(file));
				dic = (WildcardDictionary) objInputStream.readObject();
				urlactions = (Vector<OnURLAction>) objInputStream.readObject();
				objInputStream.close();
				// loading successful
				objectFileLoaded = true;
			}

		} catch (Exception exceptObjectInput) {
			setErrorMsg("restoreSettings(): " + exceptObjectInput.getMessage());
		}

		if (!objectFileLoaded || !propertiesFileLoaded) {
			writeLog("Error occured during configuration read, trying to save configuration...");
			saveSettings();
		}

	}

	/**
	 * @return the HTTP version
	 */
	public String getHttpVersion() {
		return HTTP_VERSION;
	}

	/**
	 * the User-Agent header field
	 * 
	 * @return User-Agent String
	 */
	// public String getUserAgent() {
	// return httpUserAgent;
	// }
	//
	// public void setUserAgent(String ua) {
	// httpUserAgent = ua;
	// }

	/**
	 * writes into the server log file and adds a new line
	 * 
	 */
	public void writeLog(String s) {
		writeLog(s, true);
	}

	/**
	 * writes to the server log file
	 * 
	 */
	public void writeLog(String s, boolean new_line) {
		try {
			s = new Date().toString() + " " + s;
			logFile.write(s, 0, s.length());
			if (new_line)
				logFile.newLine();
			logFile.flush();
			if (debug)
				System.out.println(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void closeLog() {
		try {
			writeLog("Server shutdown.");
			logFile.flush();
			logFile.close();
			accessLogFile.close();
		} catch (Exception e) {
		}
	}

	// public void addBytesRead(long read) {
	// bytesread += read;
	// }

	/**
	 * Functions for the statistics: How many connections Bytes read/written
	 * 
	 */
	// public void addBytesWritten(int written) {
	// byteswritten += written;
	// }

	public int getServerConnections() {
		return numconnections;
	}

	// public long getBytesRead() {
	// return bytesread;
	// }

	public long getBytesWritten() {
		return byteswritten;
	}

	public void increaseNumConnections() {
		numconnections++;
	}

	public void decreaseNumConnections() {
		numconnections--;
	}

	public String getGMTString() {
		return new Date().toString();
	}

	public Client2URLMatch findMatch(String url) {
		return (Client2URLMatch) dic.get(url);
	}

	public WildcardDictionary getWildcardDictionary() {
		return dic;
	}

	public Vector<OnURLAction> getURLActions() {
		return urlactions;
	}

	public boolean enableCookiesByDefault() {
		return this.enable_cookies_by_default;
	}

	public void enableCookiesByDefault(boolean a) {
		enable_cookies_by_default = a;
	}

	public void resetStat() {
		bytesread = 0;
		byteswritten = 0;
	}

	public void logAccess(String s) {
		try {
			accessLogFile
					.write("[" + new Date().toString() + "] " + s + "\r\n");
			accessLogFile.flush();
		} catch (Exception e) {
			writeLog("ClientServer.access(String): " + e.getMessage());
		}
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void shutdownServer() {
		closeLog();
		System.exit(0);
	}

}