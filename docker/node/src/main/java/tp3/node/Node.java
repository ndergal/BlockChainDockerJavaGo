package tp3.node;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import tp3.block.Block;
import tp3.block.BlockChain;

public class Node {

	public enum State {
		INIT, MINING, BLOCK, RESP;
	}

	private BlockChain blocks;
	private DatagramSocket serverSocket;
	private final Peers<InetAddress> peers = new Peers<>();

	private static final String BASE_URI;
	private static final int TIMEOUTMS;
	private static final int INIT_PORT;
	private static final int PORT;
	private static final String BROADCAST;
	private static final int buffSIZE;

	static {
		BASE_URI = "http://0.0.0.0:8080/";
		TIMEOUTMS = 10000;
		INIT_PORT = 8082;
		BROADCAST = "255.255.255.255";
		buffSIZE = 32000;
		PORT = 8081;
	}

	public static class Peers<R> {
		private final Set<R> peers = new HashSet<>();
		private final Object monitor = new Object();

		public void add(R peer) {
			synchronized (monitor) {
				peers.add(peer);
			}
		}

		public boolean contains(R peer) {
			synchronized (monitor) {
				return peers.contains(peer);
			}
		}

		public boolean remove(R peer) {
			synchronized (monitor) {
				return peers.remove(peer);
			}

		}

		public Set<R> getPeers() {
			return peers;
		}
	}

	public BlockChain getBlocks() {
		return blocks;
	}

	public Peers<InetAddress> getPeers() {
		return peers;
	}

	private Node(int PORT) throws SocketException, UnknownHostException {
		serverSocket = new DatagramSocket(PORT);
	}


	public void process() {
		new Thread(() -> {
			while (!Thread.interrupted()) {
				byte[] buf = new byte[buffSIZE];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				try {
					serverSocket.receive(packet);
					String[] data = new String(packet.getData()).split("\\|");
					State state = State.valueOf(data[0]);
					;
					switch (state) {

					case INIT:
						InetAddress adr = packet.getAddress();
						// Ajout table routage
						peers.add(adr);
						byte[] blockChain = blocks.toString().getBytes();
						DatagramPacket blockPacket = new DatagramPacket(blockChain, blockChain.length, adr, INIT_PORT);
						serverSocket.send(blockPacket);
						break;

					case MINING:
						Block block2 = blocks.mining(packet.getAddress().getHostAddress(),
								serverSocket.getLocalAddress().getHostAddress(), data[1]);
						blocks.add(block2);
						for(InetAddress address : peers.peers) {
							byte[] buffer = (State.BLOCK.name() + "|" + block2.toString()).getBytes();
							DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, address, PORT);
							try {
								serverSocket.send(sendPacket);
							} catch (IOException e) {
								System.out.println("OUPS : Error Mining...");
								return;
							}
						}
						

						break;

					case BLOCK:
						Block block = Block.constBlock(data[1]);
						if (block.getIndex() > blocks.getLastBlock().getIndex()
								&& block.getPreviousHash().equals(blocks.getLastBlock().getHash())) {
							blocks.add(block);
							peers.peers.forEach(e -> {
								byte[] buff = (State.RESP.name() + "|OK").getBytes();
								DatagramPacket packett;
								try {
									packett = new DatagramPacket(buff, buff.length, e, PORT);
									serverSocket.send(packett);
								} catch (IOException e2) {
									e2.printStackTrace();
								}

							});
						} else {
							peers.peers.forEach(e -> {
								byte[] buff = (State.RESP.name() + "|PASOK|" + block).getBytes();
								DatagramPacket packett;
								try {
									packett = new DatagramPacket(buff, buff.length, e, PORT);
									serverSocket.send(packett);
								} catch (IOException e2) {
									e2.printStackTrace();
								}

							});
							byte[] buff = (State.MINING.name() + "|" + block.getDataMessage()).getBytes();
							DatagramPacket pack = new DatagramPacket(buff, buff.length, packet.getAddress(), PORT);
							serverSocket.send(pack);
						}
						break;

					case RESP:
						if (data[1].equals("PASOK")) {
							Block receivedBlock = Block.constBlock(data[2]);
							if (receivedBlock.toString().equals(blocks.getLastBlock().toString())) {
								blocks.removeLastBlock();
							} else {
								// Gestion collision
								BigInteger hash1 = new BigInteger(1, receivedBlock.getHash().getBytes());
								BigInteger hash2 = new BigInteger(1, blocks.getLastBlock().getHash().getBytes());
								if (hash1.compareTo(hash2) > 0) {
									blocks.removeLastBlock();
									blocks.add(receivedBlock);
								}
							}
						}
						break;

					}
				} catch (NoSuchAlgorithmException | IOException e) {
					System.err.println(e);
				}
			}
		}).start();
	}

	public static Node startNode() throws IOException {
		DatagramSocket serverSocket = new DatagramSocket(INIT_PORT);

		Node newNode = new Node(PORT);
		byte[] buf = (State.INIT.name() + "|").getBytes();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(BROADCAST), PORT);
		serverSocket.send(packet);
		buf = new byte[buffSIZE];
		packet = new DatagramPacket(buf, buf.length);
		serverSocket.setSoTimeout(TIMEOUTMS);
		try {
			serverSocket.receive(packet);
			newNode.peers.add(packet.getAddress());
			newNode.blocks = BlockChain.getBlockChain(new String(packet.getData()));
		} catch (SocketTimeoutException e) {
			newNode.blocks = BlockChain.genesisBlockChain();
			return newNode;
		}
		try {
			while (true) {
				buf = new byte[buffSIZE];
				packet = new DatagramPacket(buf, buf.length);
				serverSocket.receive(packet);
				newNode.peers.add(packet.getAddress());
			}
		} catch (SocketTimeoutException e) {
			return newNode;
		}
	}
	//
	// public static void main(String[] args) throws IOException {
	// final HttpServer server = startAll();
	// }

}
