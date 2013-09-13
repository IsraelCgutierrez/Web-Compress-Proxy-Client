package ClientP;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;

public class Client2ServerInputStream extends BufferedInputStream implements Client2InputStream
{
	private Client2HTTPSession connection;

	public Client2ServerInputStream(Client2Server server,
			Client2HTTPSession connection, InputStream a, boolean filter) {
		super(a);
		this.connection = connection;
	}

	public int read_f(byte[] b) throws IOException {
		return read(b);
	}
}

