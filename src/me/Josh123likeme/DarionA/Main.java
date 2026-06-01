package me.Josh123likeme.DarionA;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Main {
	
	private static final String[] R_TYPE_INSTRUCTIONS = new String[] {"add","sub","lsl","lsr","asr","and","or","xor","not"};
	
	private static boolean verbose = false;
	private static int memorySize = -1; //words
	
	public static void main(String[] args) throws IOException {
		
		if (args.length < 1) throw new IllegalArgumentException("Expected input file");
		
		List<String> lines = Files.readAllLines(Paths.get(args[0]));
		Path outputPath = Paths.get("out.bin");
		
		for (int i = 1; i < args.length; i++) {
			
			String arg = args[i];
			
			switch (arg) {
			
			case "-o":
				outputPath = Paths.get(args[++i]);
				break;
			case "-v":
				verbose = true;
				break;
			case "-s":
				memorySize = Integer.parseInt(args[++i]);
				break;
			
			}
			
		}
		
		if (verbose) System.out.println("Assembling " + args[0] + " in verbose mode.\n\rInstruction memory size: " + memorySize);
		
		if (verbose) System.out.println("Starting assembly");
		
		byte[] bytes = assemble(lines);
		
		Files.write(outputPath, bytes);
		
		System.out.println("Output written to " + outputPath.toString() + " (" + bytes.length + " Bytes)");
		
	}
	
	private static byte[] assemble(List<String> lines) {
		
		List<Short> words = new ArrayList<Short>();
		
		//store indexes of all branch and jump instructions and the labels they point to
		HashMap<Integer, String> branchInstructions = new HashMap<Integer, String>();
		HashMap<Integer, String> jumpInstructions = new HashMap<Integer, String>();
		//store all labels and the locations they point to
		HashMap<String, Integer> labelPointers = new HashMap<String, Integer>();
		
		//assemble lines
		for (int i = 0; i < lines.size(); i++) {
			
			String line = lines.get(i).strip();
			
			//detect whitespace
			if (line.length() == 0) {
				if (verbose) System.out.println("Line " + (i + 1) + ": " + line + " >> Detected whitespace");
				continue;
			}
			
			//detect comment
			if (line.matches("//.*")) {
				if (verbose) System.out.println("Line " + (i + 1) + ": " + line + " >> Detected comment");
				continue;
			}
			
			//detect label
			if (line.matches("[a-zA-Z0-9_]+:")) {
				
				labelPointers.put(line.substring(0, line.length() - 1), words.size());
				if (verbose) System.out.println("Line " + (i + 1) + ": " + line + " >> Detected label");
				continue;
			}
			
			String mnemonic = line.split(" |,")[0];
			List<String> args = getArgs(line);
			
			if (verbose) System.out.println("Line " + (i + 1) + ": " + line + " >> Detected instruction: " + mnemonic);
			
			short word = 0;
			
			if (Arrays.asList(R_TYPE_INSTRUCTIONS).contains(mnemonic)) {
				
				int rd = Integer.parseInt(args.get(0));
				int rs = Integer.parseInt(args.get(1));
				int rt = Integer.parseInt(args.get(2));
				int func = -1;
				
				if (mnemonic.equals("add")) func = 0b0000;
				else if (mnemonic.equals("sub")) func = 0b0001;
				else if (mnemonic.equals("lsl")) func = 0b0100;
				else if (mnemonic.equals("lsr")) func = 0b0110;
				else if (mnemonic.equals("asr")) func = 0b0111;
				else if (mnemonic.equals("and")) func = 0b1000;
				else if (mnemonic.equals("or")) func = 0b1001;
				else if (mnemonic.equals("xor")) func = 0b1010;
				else if (mnemonic.equals("not")) func = 0b1011;
				else throw new IllegalStateException();
				
				if (rd < 0 || rd > 7)
					throw new IllegalArgumentException("Register rd:" + rd + " is out of range");
				if (rs < 0 || rs > 7)
					throw new IllegalArgumentException("Register rs:" + rs + " is out of range");
				if (rt < 0 || rt > 7)
					throw new IllegalArgumentException("Register rt:" + rt + " is out of range");
				
				word = (short) (0b000 << 13 | rs << 10 | rt << 7 | rd << 4 | func);
				
			}
			else if (mnemonic.equals("addi") || mnemonic.equals("subi")) {
				
				int opcode;
				int rt = Integer.parseInt(args.get(0));
				int rs = Integer.parseInt(args.get(1));
				int im = Integer.parseInt(args.get(2));
				
				if (mnemonic.equals("addi")) opcode = 0b001;
				else if (mnemonic.equals("subi")) opcode = 0b010;
				else throw new IllegalStateException();
				
				if (rt < 0 || rt > 7)
					throw new IllegalArgumentException("Register rt:" + rt + " is out of range");
				if (rs < 0 || rs > 7)
					throw new IllegalArgumentException("Register rs:" + rs + " is out of range");
				if (im < -32 || im > 31)
					throw new IllegalArgumentException("Immediate value im:" + im + " is out of range");
				
				word = (short) (opcode << 13 | rs << 10 | rt << 7 | (im & 0x7F));
				
			}
			else if (mnemonic.equals("jmp")) {
				
				String label = args.get(0);
				
				jumpInstructions.put(words.size(), label);
				
				word = 0b011 << 13;
				
			}
			else if (mnemonic.equals("bne") || mnemonic.equals("bgt")) {
				
				int opcode;
				int rt = Integer.parseInt(args.get(0));
				int rs = Integer.parseInt(args.get(1));
				String label = args.get(2);
				
				if (mnemonic.equals("bne")) opcode = 0b100;
				else if (mnemonic.equals("bgt")) opcode = 0b101;
				else throw new IllegalStateException();
				
				if (rt < 0 || rt > 7)
					throw new IllegalArgumentException("Register rt:" + rt + " is out of range");
				if (rs < 0 || rs > 7)
					throw new IllegalArgumentException("Register rs:" + rs + " is out of range");
				
				word = (short) (opcode << 13 | rs << 10 | rt << 7);
				
				branchInstructions.put(words.size(), label);
				
			}
			else if (mnemonic.equals("lw") || mnemonic.equals("sw")) {
				
				int opcode;
				int rt = Integer.parseInt(args.get(0));
				int im = Integer.parseInt(args.get(1));
				int rs = Integer.parseInt(args.get(2));
				
				if (mnemonic.equals("lw")) opcode = 0b110;
				else if (mnemonic.equals("sw")) opcode = 0b111;
				else throw new IllegalStateException();
				
				if (rt < 0 || rt > 7)
					throw new IllegalArgumentException("Register rt:" + rt + " is out of range");
				if (im < -32 || im > 31)
					throw new IllegalArgumentException("Immediate value im:" + im + " is out of range");
				if (rs < 0 || rs > 7)
					throw new IllegalArgumentException("Register rs:" + rs + " is out of range");
				
				word = (short) (opcode << 13 | rs << 10 | rt << 7 | (im & 0x7F));
				
			}
			else throw new IllegalArgumentException("Mnemonic \"" + mnemonic + "\" is not a valid opcode");
			
			words.add(word);
			
			if (verbose) System.out.println("->Word " + (words.size() - 1) + ": " + toBinary16(word));
			
		}
		
		if (verbose) System.out.println("Writing label addresses to branch and jump instructions");
		
		//add label addresses to branch and jump instructions
		for (Integer branch : branchInstructions.keySet()) {
			
			int labelPointer = labelPointers.get(branchInstructions.get(branch));
			
			int relativePointer = labelPointer - (branch + 1);
			
			if (relativePointer < -32 || relativePointer > 31)
				throw new IllegalArgumentException("The branch pointer is out of range (pointer: " + relativePointer + ", range: [-32,31]");
			
			words.set(branch, (short) (words.get(branch) | (relativePointer & 0x7F)));
			
			if (verbose) System.out.println("Branch at Word " + branch + " points to label \"" + branchInstructions.get(branch) + "\". Relative label pointer set to " + relativePointer);
			
		}
		
		for (Integer jump : jumpInstructions.keySet()) {
			
			int labelPointer = labelPointers.get(jumpInstructions.get(jump));
			
			if (labelPointer < 0 || labelPointer > 8191)
				throw new IllegalArgumentException("The jump pointer is out of range (pointer: " + labelPointer + ", range: [0,8192)");
			
			words.set(jump, (short) ((words.get(jump) | (labelPointer & 0x1FFF))));
			
			if (verbose) System.out.println("Jump at Word " + jump + " points to label \"" + jumpInstructions.get(jump) + "\". Absolute label pointer set to " + labelPointer);
			
		}
		
		System.out.println(words.size() + " instructions written (" + ((double) words.size() * 100 / memorySize) + "% memory used)");
		
		//pack bytes into array
		
		byte[] bytesAsArray;
		
		if (memorySize != -1) {
		
			if (memorySize < words.size()) throw new IllegalArgumentException("There is not enough instruction memory available for this program");
			
			bytesAsArray = new byte[memorySize * 2];
		
		}
		else bytesAsArray = new byte[words.size() * 2];
		
		//fill instructions into byte array
		for (int i = 0; i < words.size(); i++) {
			
			bytesAsArray[i*2] = (byte) (words.get(i) >> 8 & 0xFF);
			bytesAsArray[i*2+1] = (byte) (words.get(i) & 0xFF);
			
		}
		
		return bytesAsArray;
		
	}
	
	private static List<String> getArgs(String line) {
		
		List<String> args = new ArrayList<String>();
		
		String[] tokens = line.split("[ ,()]+");
		
		//we skip index 0 because that is the opcode
		for (int i = 1; i < tokens.length; i++) {
			
			String token = tokens[i];
			
			args.add(decodeArg(token));
			
		}
		
		return args;
		
	}
	
	private static String decodeArg(String token) {
		
		//register
		if (token.matches("\\$([0-7]|zero|z)")) {
			
			int registerAddress;
			
			if (token.matches("\\$(0|zero|z)")) registerAddress = 0;
			else registerAddress = Integer.parseInt("" + token.charAt(1));
			
			return "" + registerAddress;
			
		}
		
		//hex or denary immediate
		if (token.matches("-?((0x[0-9A-Fa-f]+)|([0-9]+))")) {
			
			return "" + Integer.decode(token);
			
		}
		
		//label
		if (token.matches("[a-zA-Z0-9_]+")) {
			
			return token;
			
		}
		
		throw new IllegalArgumentException("Argument \"" + token + "\" is not a valid argument");
		
	}
	
	private static String toBinary16(short x) {

	    StringBuilder sb = new StringBuilder(16);

	    for (int i = 15; i >= 0; i--) {
	        sb.append((x >> i) & 1);
	    }

	    return sb.toString();
	}
	
}
