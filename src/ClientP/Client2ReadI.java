package ClientP;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public class Client2ReadI extends Thread {
	private final int BUFFER_SIZE = 65535;
	private BufferedInputStream in;
	private BufferedOutputStream out;
	private Client2HTTPSession connection;
	private Client2Server server;
	private String line;
	private int nRead;
	private String answerHASH;
	private boolean found = false;
	private String answerEncoding;
	private final String HostName;
	private final String UrlHash;
	private final String uri;
	private String RealSize = "";
	private boolean RealS = false;
	private String answerSentSize;
	private String Changes;
	private final String FileHash;
	private boolean GzipS = false;
	private boolean end = false;
	private boolean contains = false;
	private final String Root;
	private String Vars;
	private final String varsHashs;
	private int nChars = 0;
	// private String Charset = "ISO-8859-1";
	private final String Charset = "UTF-8";
	private String ansHASH;

	public Client2ReadI(Client2Server server, Client2HTTPSession connection,
			BufferedInputStream l_in, BufferedOutputStream l_out,
			Client2ClientInputStream in2) {
		in = l_in;
		out = l_out;
		this.connection = connection;
		this.server = server;
		setPriority(Thread.MIN_PRIORITY);
		found = in2.ffound;
		HostName = in2.getRemoteHostName();
		UrlHash = in2.getURLH();
		uri = in2.url;
		FileHash = in2.fileHash;
		Root = in2.Root;
		Vars = in2.Vars;
		varsHashs = in2.varsHashs;
		start();
	}

	@Override
	public void run() {
		read();

		server = null;
		connection = null;
		in = null;
		out = null;

	}

	byte[] Entrada = null;
	ByteArrayOutputStream entr = new ByteArrayOutputStream();
	boolean ent = true;

	public int leetodo() throws IOException {
		int p = 0;
		byte[] buffin = new byte[65535];
		p = in.read(buffin);
		if (p != -1) {
			if (p > 0) {
				entr.write(buffin, 0, p);
				if (p > 3) {
					if (buffin[p - 1] == 10 && buffin[p - 2] == 13
							&& buffin[p - 3] == 10 && buffin[p - 4] == 13) {
						cab = false;
					}
				}
			}
		} else {
			// System.out.print("nada");
			ent = false;
			// return -1;
		}
		// ent = false;
		if (p > 0) {
			Entrada = new byte[entr.size()];
			Entrada = entr.toByteArray();
		}
		return p;
	}

	int lindex = 0;
	boolean cab = true;

	public int getLine() throws IOException {

		boolean e = false;
		int c;
		line = "";
		nRead = 0;
		if (Entrada != null) {
			for (int i = lindex; i < Entrada.length; i++) {
				c = Entrada[i];
				if (c != '\n') {
					line += (char) c;
					nRead++;
				} else {
					line += (char) c;
					nRead++;
					lindex = i + 1;
					e = true;
					break;
				}
			}
		}
		// System.out.println("e:" + !e + " " + ent + " " + cab + " "
		// + in.available() + " " + line.length());
		if (!e && (ent || cab)) {
			int pp = leetodo();
			if (pp == -1) {
				line = "";
				return -1;
			} else if (pp == 0) {
				ent = true;
			}
			return getLine();
		}
		// System.out.println(ent + " " + line.length() + " " + line);
		// System.out.print(">" + line.length() + " " + line + "<");
		return line.length();
	}

	public byte[] restt() {
		if (Entrada == null || Entrada.length - lindex <= 0) {
			return null;
		}
		byte[] k = new byte[Entrada.length - lindex];
		for (int t = 0; t < Entrada.length - lindex; t++) {
			k[t] = Entrada[lindex + t];
		}
		lindex += Entrada.length - lindex;
		Entrada = null;
		lindex = 0;
		entr = new ByteArrayOutputStream();
		ent = true;
		cab = true;
		return k;
	}

	public int getLine2() throws IOException {
		int c = 0;
		line = "";
		nRead = 0;
		while (c != '\n') {
			c = in.read();
			if (c != -1) {
				line += (char) c;
				nRead++;
			} else {
				end = true;
				return -1;
			}
		}
		System.out.print(">" + line.length() + " " + line + "<");
		return nRead;
	}

	private void SendDataDirect2() throws IOException {
		// Entrada = null;
		// entr = new ByteArrayOutputStream();
		// cab = true;
		// lindex = 0;
		while (true) {
			nChars = getLine();
			if (line.contains("200")) {
				contains = true;
				// sentLine(n);
				break;
			} else {
				if (nChars == -1) {
					break;
				} else {
					// System.out.print("sdd2,");
					sendLine(nChars);
				}
			}
		}
	}

	private void read() {
		if (connection.isTunnel()) {
			try {
				SendDataDirect();
			} catch (IOException e) {
			}
		} else {
			boolean formated = false;
			try {
				nChars = getLine();
			} catch (IOException e1) {
			}
			try {
				// looking for the code.
				while (!end) {
					// System.out.print(in.toString().substring(
					// in.toString().indexOf("@"))
					// + " ");
					cab = true;
					// System.out.print("l:" + nChars + " " + line);
					if (line.contains("200") || contains) {
						while (nChars != -1 && nChars > 2) {
							// System.out.print("l:" + line);
							if (line.toUpperCase().contains("CONTENT-LENGTH")) {
								RealSize = line.substring(
										line.indexOf(" ") + 1,
										line.indexOf("\r\n"));
								// if(this.HostName.equals("localhost")){
								// System.out.println("realsize: ["+RealSize+"] from "+HostName);
								// }
								// System.out.print("prs,");
								sendLine(nChars);
								RealS = true;
							} else if (line.contains("X-protocolX")) {
								formated = true;
								// System.out.println(line);
								String parameters = line.substring(
										line.indexOf(":") + 1,
										line.indexOf("\r\n"));
								String[] param = parameters.split(",");
								// System.out.println(parameters + " ->" + uri);
								answerHASH = param[0];
								// System.out.println(answerHASH);
								String[] a = answerHASH.split("-");
								RealSize = a[1];
								ansHASH = a[0];
								answerEncoding = param[1];
								if (answerEncoding.equals("gzip")) {
									GzipS = true;
								} else {
									GzipS = false;
								}
								answerSentSize = param[2];
								Changes = param[3];
							} else {
								// normal headers
								// System.out.print("normal,");
								sendLine(nChars);
							}
							nChars = getLine();
						}
						// System.out.print("last1,");
						sendLine(nChars);
						cab = false;
						if (formated) {
							// look for file already there
							if (Changes.equals("1") && ansHASH.equals("0")) {
								byte[] rdata = SendDataDirect3();
								answerHASH = GetHash(rdata) + "-"
										+ rdata.length;
								// System.out.println(answerHASH);
								SaveCache(false, rdata, rdata.length);
							} else {
								byte[] answer = null;
								byte[] rdata = ReceiveData();
								if (Changes.contains("0")) {
									byte[] ldata = loadDatafromCache();
									// no changes
									answer = ldata;
								} else if (Changes.contains("1")) {

									if (GzipS) {
										byte[] ucdata = UncompressData(rdata);
										answer = ucdata;
									} else {
										answer = rdata;
									}
								} else if (Changes.contains("2")) {
									byte[] ldata = loadDatafromCache();

									if (GzipS) {
										byte[] ucdata = UncompressData(rdata);
										rdata = ucdata;
									}
									// de-comparing
									diff_match_patch comparator = new diff_match_patch();
									String patch_text = new String(rdata,
											Charset);
									String loa = new String(ldata, Charset);
									Object[] result = comparator
											.patch_apply(
													comparator
															.patch_fromText(patch_text),
													loa);
									String rebuilded = (String) result[0];
									answer = rebuilded.getBytes(Charset);
									// System.out.println("<< " + loa.length()
									// + " << " + FileHash + " << "
									// + rebuilded.length() + " << "
									// + answer.length);
								}

								if (CompareHash(answer)) {
									// send to browser
									SendToBrowser(answer, answer.length);

									// save new hash
									if (!Changes.equals("0")) {
										SaveCache(false, answer, answer.length);
									}
									end = true;
								} else {
									// System.out.println("e->"+new
									// String(ldata)+" <-");
									delete(HostName + "/" + this.FileHash);
									delete(HostName + "/"
											+ GetHash(Root.getBytes(Charset)));
									System.out.println(GetHash(answer) + "-"
											+ answer.length);
									System.out.println(uri + " " + answerHASH
											+ "\r\n");
									// SaveCache(false,answer);
									end = true;
									break;
									// System.out.println("["+new
									// String(ldata)+"]");
								}
							}
						} else {
							// System.out.print("last1.5,");
							// sendLine(nChars);
							SendDataDirect();
						}
					} else {
						// no format
						// send answer to browser without changes
						// System.out.print("last2,");
						sendLine(nChars);
						SendDataDirect();
					}

				}// end
			} catch (IOException e) {
			}
		}// is tunel;
		try {
			if (connection.getStatus() != Client2HTTPSession.SC_CONNECTING_TO_HOST)
				connection.getLocalSocket().close();
			// If we are connecting to a new host (and this thread is
			// already running!) , the upstream
			// socket will be closed. So we get here and close our own
			// downstream socket..... and the browser
			// displays an empty page because Client
			// closes the connection..... so close the downstream socket only
			// when NOT connecting to a new host....
		} catch (IOException e_socket_close) {
		}

		// buf = null;
	}

	private byte[] SendDataDirect3() throws IOException {
		int bytes_read = 0;
		byte[] buf = new byte[BUFFER_SIZE];
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		// <-
		byte[] aux = restt();
		if (aux != null) {
			out.write(aux, 0, aux.length);
			out.flush();
			// server.addBytesRead(aux.length);
			buffer.write(aux, 0, aux.length);
		}
		// <-

		while (true) {
			bytes_read = in.read(buf);
			if (bytes_read != -1) {
				out.write(buf, 0, bytes_read);
				out.flush();
				// server.addBytesRead(bytes_read);
				buffer.write(buf, 0, bytes_read);
			} else
				break;
		}
		byte[] kk = new byte[buffer.size()];
		kk = buffer.toByteArray();
		return kk;
	}

	private String GetHash(byte[] r) {
		String s = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.reset();
			md.update(r);
			// s = Base64.encodeToString(md.digest(), true);
			BigInteger bigInt = new BigInteger(1, md.digest());
			s = bigInt.toString(16);
			while (s.length() < 32) {
				s = "0" + s;
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return s;
	}

	private boolean CompareHash(byte[] answer) {
		String an = GetHash(answer) + "-" + answer.length;
		// System.out.println("cp "+answerHASH+" - "+an);
		return answerHASH.equals(an);
	}

	private void sendLine(int nChars) throws IOException {
		out.write(line.getBytes(Charset));
		out.flush();
		// server.addBytesRead(nChars);
		// System.out.print(line + ";");
	}

	private byte[] loadDatafromCache() throws IOException {
		return open(HostName + "/" + this.FileHash);
	}

	private byte[] open(String file) throws IOException {
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			int Size = -1;
			String[] a = file.split("-");
			if (!(a[1].equals(""))) {
				Size = Integer.parseInt(a[1]);
			}
			if (Size != -1) {
				byte[] loaded = new byte[Size];
				in.read(loaded, 0, Size);
				found = true;
				return loaded;
			} else {
				int c;
				byte[] buff = new byte[BUFFER_SIZE * 100];
				c = in.read(buff);
				byte[] loaded = new byte[c];
				for (int i = 0; i < c; i++) {
					loaded[i] = buff[i];
				}
				found = true;
				return loaded;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			found = false;
			// e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			found = false;
			// e.printStackTrace();
		} finally {
			if (in != null) {
				in.close();
			}
		}
		return null;
	}

	private byte[] open2(String file) {
		int Size = -1;
		String[] a = file.split("-");
		if (!(a[1].equals(""))) {
			Size = Integer.parseInt(a[1]);
		}
		byte[] bytes = new byte[Size];
		char[] opened = new char[Size];
		int r = 0;
		try {
			FileReader fr = new FileReader(file);
			BufferedReader inr = new BufferedReader(fr);
			r = inr.read(opened);
		} catch (FileNotFoundException e) {
			System.out.println("--> URLHash:" + this.UrlHash + "\r\n"
					+ "-->FileHash:" + this.FileHash + "\r\n" + "-AnswerHash:"
					+ this.answerHASH + "\r\n");
			// e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// if (r==(Size)){
		try {
			bytes = (new String(opened)).getBytes(Charset);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// }
		// System.out.println(">> "+bytes.length +" "+FileHash);
		// System.out.println(r +" >> "+Size+" >>"+(new
		// String(opened)).length());
		/*
		 * System.out.println("--> URLHash:"+this.UrlHash+"\r\n"+
		 * "-->FileHash:"+this.FileHash+"\r\n"+
		 * "-AnswerHash:"+this.answerHASH+"\r\n");
		 */
		return bytes;
	}

	private void SaveCache(boolean bo, byte[] rdata, int size) {
		// create directory
		@SuppressWarnings("unused")
		boolean dir = (new File(HostName)).mkdir();
		if (Vars.equals("")) {
			Vars = "/";
		}
		String fFileName = null;
		try {
			fFileName = HostName + "/" + GetHash(Root.getBytes(Charset));
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		boolean ins = false;
		String text = "";
		String[] pairs = varsHashs.split("\t");
		// System.out.println(varsHashs.length()+" "+pairs.length);
		int i = 0;
		if (pairs.length > 0) {
			while (i < pairs.length - 1) {
				String[] pair = pairs[i].split(" ");
				if (pair[0].equals(Vars) && false) {
					delete(HostName + "/" + pair[1]);
					pair[1] = answerHASH;
					ins = true;
				}
				text = text + (pair[0] + " " + pair[1] + "\t");
				i += 1;
			}
		}
		if (!ins) {
			text = (Vars + " " + answerHASH + "\t") + varsHashs;
		}
		save(fFileName, text);
		// write file with aHASH
		// fFileName = HostName+"/"+answerHASH;
		// try {
		// text = new String(rdata,0,size,Charset);
		// } catch (UnsupportedEncodingException e) {
		// TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		save2(rdata, size);
		// }
	}

	private void delete(String file) {
		File f1 = new File(file);
		boolean success = f1.delete();
		if (!success) {
			// System.out.println("Deletion failed.");
		} else {
			// System.out.println("File deleted.");
		}

	}

	private void save2(byte[] sdata, int size) {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(HostName + "/" + answerHASH);
			out.write(sdata, 0, size);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void save(String fFileName, String text) {

		try {
			// Create file
			FileWriter fstream = new FileWriter(fFileName);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(text);
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

	private void SendToBrowser(byte[] ucdata, int size) throws IOException {
		out.write(ucdata, 0, size);
		out.flush();
	}

	public static byte[] compressData(byte[] tocomp) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream(tocomp.length);
		GZIPOutputStream gos = new GZIPOutputStream(os);
		gos.write(tocomp);
		gos.close();
		byte[] compressed = os.toByteArray();
		os.close();
		return compressed;
	}

	private byte[] UncompressData(byte[] touncomp) throws IOException {
		final int BUFFER_SIZE = 65535;
		// byte[] touncomp =
		// org.apache.commons.codec.binary.Base64.decodeBase64(touncomp1);
		ByteArrayInputStream is = new ByteArrayInputStream(touncomp);
		GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
		StringBuilder string = new StringBuilder();
		byte[] data = new byte[BUFFER_SIZE];
		int bytesRead;
		while ((bytesRead = gis.read(data)) != -1) {
			string.append(new String(data, 0, bytesRead, Charset));
		}
		gis.close();
		is.close();
		return string.toString().getBytes(Charset);
	}

	private byte[] UncompressData2(byte[] touncomp) {
		InputStream in = new InflaterInputStream(new ByteArrayInputStream(
				touncomp));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[8192];
			int len;
			while ((len = in.read(buffer)) > 0) {
				baos.write(buffer, 0, len);
			}
			baos.flush();
			baos.close();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		return baos.toByteArray();
	}

	private byte[] ReceiveData() throws IOException {
		byte[] rr = null;
		int br = 0;
		int size;
		int r = 0;
		boolean re = false;
		if (GzipS) {
			size = Integer.parseInt(answerSentSize);
		} else if (RealS) {
			size = Integer.parseInt(RealSize);
		} else {
			size = 8 * 1024 * 10;
			re = true;
		}
		rr = new byte[size];
		// <-
		byte[] aux = restt();
		if (aux != null) {
			for (int q = 0; q < aux.length; q++) {
				rr[q] = aux[q];
			}
			br += aux.length;
		}
		if (br < size) { // <-
			byte[] buf = new byte[size];
			while (r != -1) {
				r = in.read(buf);
				// System.out.println("~" + size + " " + br + " " + r);
				if (r != -1) {
					for (int i = 0; i < r; i++) {
						rr[i + br] = buf[i];
					}
					br += r;
					if (br == size) {
						break;
					}
				} else {
					break;
				}

			}
		}// <-
		if (re) {
			RealSize = br + "";
			byte[] rr3 = new byte[br];
			for (int i = 0; i < br; i++) {
				rr3[i] = rr[i];
			}
			return rr3;
		}
		return rr;

	}

	private void SendDataDirect() throws IOException {
		int bytes_read = 0;
		int count = 0;
		// String buffer ="";

		if (RealS) {
			int rs = Integer.parseInt(RealSize.trim());
			byte[] buf = new byte[rs];
			count = 0;
			// <-
			byte[] aux = restt();
			if (aux != null) {
				out.write(aux, 0, aux.length);
				out.flush();
				count += aux.length;
				// server.addBytesRead(aux.length);
			}
			if (count < rs) { // <-
				while (true) {
					bytes_read = in.read(buf, count, rs - count);

					if (bytes_read != -1) {
						if (bytes_read != 0) {
							out.write(buf, count, bytes_read);
							out.flush();
							count += bytes_read;
							// server.addBytesRead(bytes_read);
							// System.out.println("enviado->" + count + " de "
							// + RealSize + " " + bytes_read);
							if (count == rs) {
								end = false;
								break;
							}
							// else if (count == rs) {
							// break;
							// }
							else if (count > rs) {
								// System.out.println("fin1->" + count + " de "
								// + RealSize + " " + bytes_read);
								end = false;
								SendDataDirect2();
								// System.out.println("leido "+RealS+" :"+count+" ");
								break;
							}
						}

					} else {
						end = true;
						// System.out.println("fin2->" + bytes_read);
						break;
					}
				}
			}// <-
		} else {
			byte[] buf = new byte[BUFFER_SIZE];
			// <-
			byte[] aux = restt();
			if (aux != null) {
				out.write(aux, 0, aux.length);
				out.flush();
				// server.addBytesRead(aux.length);
				count += aux.length;
			}
			// <-
			while (true) {
				bytes_read = in.read(buf);
				if (bytes_read != -1) {
					out.write(buf, 0, bytes_read);
					out.flush();
					// server.addBytesRead(bytes_read);
					count += bytes_read;
				} else {
					// System.out.println("fin3->" + count + " de " + RealSize
					// + " " + bytes_read);
					end = true;
					break;
				}
			}
		}

	}

	/**
	 * stop the thread by closing the socket
	 */
	public void close() {
		try {
			in.close();
		} catch (Exception e) {
		}
	}
}
