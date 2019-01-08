package tp3.node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import tp3.block.Block;
import tp3.node.Node.State;

@Path("myfile")
public class Main {

	public static Node node;
	private static final String BASE_URI;
	private static final int TIMEOUTMS;
	private static final int INIT_PORT;
	private static final int PORT;
	private static final String BROADCAST;
	private static final int SIZE;

	static {
		BASE_URI = "http://0.0.0.0:8080/";
		TIMEOUTMS = 10000;
		INIT_PORT = 8082;
		BROADCAST = "255.255.255.255";
		SIZE = 4096;
		PORT = 8081;
	}

	public static HttpServer startServer() throws IOException {
		ResourceConfig rc = new ResourceConfig().packages("tp3.node");
		return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
	}

	@Path("blockchain")
	@GET
	public String getBlockchain(@Context org.glassfish.grizzly.http.server.Request re) throws IOException {
		return node.getBlocks().toString();
	}

	@Path("get-messages")
	@GET
	public String getMessages(@Context org.glassfish.grizzly.http.server.Request re) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (Block b : node.getBlocks().getBlocks()) {
			if (b.getData().getReceiverId().equals(InetAddress.getLocalHost().getHostAddress())) {
				sb.append(b.toString());
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	@Path("send/{message}/{dest}")
	@POST
	public void sendMessage(@PathParam("message") String message, @PathParam("dest") String dest,
			@Context org.glassfish.grizzly.http.server.Request re) throws IOException {
		for (InetAddress adr : node.getPeers().getPeers()) {
			if (adr.getHostAddress().equals(dest)) {
				String finalMessage = State.MINING.name() + "|" + message;
				byte[] buf = finalMessage.getBytes();
				DatagramPacket packet = new DatagramPacket(buf, buf.length, adr, 8081);
				DatagramSocket serverSocket = new DatagramSocket();
				serverSocket.send(packet);
				return;
			}
		}
	}

	public static void main(String[] args) throws IOException {
		node = Node.startNode();
		node.process();
		HttpServer server = startServer();
	}

}
