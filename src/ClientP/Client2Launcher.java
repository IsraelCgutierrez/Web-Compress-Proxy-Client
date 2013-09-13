package ClientP;
public class Client2Launcher {

  static Client2Server server;

  public static void main(String[] args)
  {
		server = new Client2Server(true);
    	if (server.fatalError) {
    		System.out.println("Error: " +  server.getErrorMessage());
		}
    	else {
    		new Thread(server).start();
    	   	System.out.println("Running on port " + server.port);
    	}
  }
}