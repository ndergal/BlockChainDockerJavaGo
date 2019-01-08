package tp3.block;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Predicate;

import javax.xml.bind.DatatypeConverter;

public class BlockChain {

	private final LinkedList<Block> blocks = new LinkedList<>();
	private static final Block GENESIS;
	private Block last;
	private static final Predicate<BigInteger> p;
	static {
		GENESIS = new Block(0, "0", "nobody", "nobody", "first");
		GENESIS.setHash("0");
		GENESIS.setNonce(0);
		p = b -> b.mod(new BigInteger("7")).intValue() == 0;
	}

	private void generateGenesisBlock() {
		synchronized (blocks) {
			if (blocks.isEmpty()) {
				blocks.add(GENESIS);
			}
		}
	}

	public static BlockChain genesisBlockChain() {
		BlockChain bc = new BlockChain();
		bc.generateGenesisBlock();
		return bc;
	}

	public Collection<Block> getBlocks() {
		synchronized (blocks) {
			return Collections.unmodifiableCollection(blocks);
		}
	}

	@Override
	public String toString() {
		synchronized (blocks) {
			StringBuilder sb = new StringBuilder();
			blocks.stream().forEach(b -> sb.append(b.toString()).append("IIIIIII\n"));
			return sb.substring(0, sb.length() - 8);
		}
	}

	public Block mining(String senderId, String receiverId, String message) throws NoSuchAlgorithmException {
		Block lastBlock;
		synchronized (blocks) {
			lastBlock = blocks.getLast();
		}
		int lastIndex = lastBlock.getIndex();
		String previousHash = lastBlock.getHash();
		Block newBlock = new Block(lastIndex + 1, previousHash, senderId, receiverId, message);
		int nonce = 0;
		String input;
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hashByte;
		do {
			nonce += 1;
			input = newBlock.getIndex() + previousHash + newBlock.getTimestamp() + message + nonce;
			hashByte = digest.digest(input.getBytes(StandardCharsets.UTF_8));
		} while (!isValidHash(hashByte));
		newBlock.setNonce(nonce);
		newBlock.setHash(DatatypeConverter.printHexBinary(hashByte));
		return newBlock;
	}

	private boolean isValidHash(byte[] hash) {
		return p.test(new BigInteger(1, hash));
	}

	public void add(Block block) {
		synchronized (blocks) {
			blocks.add(block);
		}
	}

	public Block remove(String previousHash) {
		synchronized (blocks) {
			Optional<Block> opt = blocks.stream().filter(b -> (b.getPreviousHash() == previousHash)).findFirst();
			if (opt.isPresent()) {
				return blocks.remove(opt.get().getIndex());
			} else {
				return null;
			}
		}
	}

	public Block getLastBlock() {
		return blocks.getLast();
	}

	public Block removeLastBlock() {
		return blocks.removeLast();
	}

	public static BlockChain getBlockChain(String strBlockChain) {
		BlockChain blockChain = new BlockChain();

		for (String bloc : strBlockChain.split("IIIIIII\n")) {

			ArrayList<String> values = new ArrayList<>();
			for (String line : bloc.split("\n")) {

				if (line.contains(":")) {
					String[] val = line.split(" : ");
					values.add(val[1]);
				}
			}
			Block block = Block.constBlock(Integer.parseInt(values.get(0)), Timestamp.valueOf(values.get(1)),
					values.get(5), values.get(6), Integer.parseInt(values.get(7)), values.get(2), values.get(3),
					values.get(4));
			blockChain.add(block);
		}
		return blockChain;
	}

	public static void main(String[] args) throws NoSuchAlgorithmException {
		BlockChain blocks = new BlockChain();
		blocks.generateGenesisBlock();
		blocks.add(blocks.mining("me", "you", "hello world !"));
		blocks.add(blocks.mining("me", "you", "hello world !"));
		blocks.add(blocks.mining("me", "you", "hello world !"));

		System.out.println(blocks);

		for (String s : blocks.toString().split("IIIIIII\n")) {
			System.out.println(s);
		}

		blocks.getBlocks().forEach(b -> System.out.println(b));
	}
}
