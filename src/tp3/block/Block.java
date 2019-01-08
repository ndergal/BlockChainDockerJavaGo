package tp3.block;

import java.sql.Timestamp;
import java.util.ArrayList;

public class Block {
	private final int index;
	private final Timestamp timestamp;
	private final Data data;
	private final String previousHash;
	private String hash;
	private int nonce;

	public Block(int index, String previousHash, String senderId, String receiverId, String message) {
		this.index = index;
		this.timestamp = new Timestamp(System.currentTimeMillis());
		this.previousHash = previousHash;
		this.data = new Data(senderId, receiverId, message);
	}

	private Block(int index, Timestamp time, String previousHash, String hash, int nonce, String senderId,
			String receiverId, String message) {
		this.index = index;
		this.timestamp = time;
		this.previousHash = previousHash;
		this.data = new Data(senderId, receiverId, message);
		this.hash = hash;
		this.nonce = nonce;
	}

	public static Block constBlock(int index, Timestamp time, String previousHash, String hash, int nonce,
			String senderId, String receiverId, String message) {
		return new Block(index, time, previousHash, hash, nonce, senderId, receiverId, message);
	}

	public static Block constBlock(String bloc) {

		ArrayList<String> values = new ArrayList<>();
		for (String line : bloc.split("\n")) {
			if (line.contains(":")) {
				String[] val = line.split(" : ");
				values.add(val[1]);
			}
		}
		return new Block(Integer.parseInt(values.get(0)), Timestamp.valueOf(values.get(1)), values.get(5),
				values.get(6), Integer.parseInt(values.get(7)), values.get(2), values.get(3), values.get(4));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("index : " + index + "\n");
		sb.append("timestamp : " + timestamp + "\n");
		sb.append(data.toString());
		sb.append("previousHash : " + previousHash + "\n");
		sb.append("hash : " + hash + "\n");
		sb.append("nonce : " + nonce + "\n");
		return sb.toString();
	}

	public String getDataMessage() {
		return this.data.message;
	}

	public class Data {
		private final String senderId;
		private final String receiverId;
		private final String message;

		public Data(String senderId, String receiverId, String message) {
			this.senderId = senderId;
			this.receiverId = receiverId;
			this.message = message;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("senderId : " + senderId + "\n");
			sb.append("receiverId : " + receiverId + "\n");
			sb.append("message : " + message + "\n");
			return sb.toString();

		}

		public String getReceiverId() {
			return receiverId;
		}
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public int getIndex() {
		return index;
	}

	public String getPreviousHash() {
		return previousHash;
	}

	public Data getData() {
		return data;
	}

	public String getHash() {
		return hash;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setNonce(int nonce) {
		this.nonce = nonce;
	}

}
