package me.Josh123likeme.DarionE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
	
	private static boolean stepExecution = false;
	
	public static void main(String[] args) throws IOException {
		
		if (args.length < 1) throw new IllegalArgumentException("Expected input file");
		
		byte[] programBinary = Files.readAllBytes(Paths.get(args[0]));
		
		short[] instMem = null;
		short[] dataMem = null;
		
		for (int i = 1; i < args.length; i++) {
			
			String arg = args[i];
			
			switch (arg) {
			
			case "-s":
				stepExecution = true;
				break;
			case "-i":
				instMem = new short[Integer.parseInt(args[++i])];
				break;
			case "-d":
				dataMem = new short[Integer.parseInt(args[++i])];
				break;
			}
			
		}
		
		if (instMem == null) throw new IllegalArgumentException("Please provide the instruction memory size (-i 1024)");
		if (dataMem == null) throw new IllegalArgumentException("Please provide the data memory size (-d 1024)");
		
		if (programBinary.length / 2 > instMem.length) throw new IllegalArgumentException("Not enough instruction memory (" + instMem.length + " words allocated, " + (programBinary.length / 2) + " words in program");
		
		for (int i = 0; i < programBinary.length; i += 2) {
			
			instMem[i / 2] = (short) ((programBinary[i] & 0xFF) << 8 | programBinary[i+1] & 0xFF);
			
		}
		
		emulate(instMem, dataMem);
		
	}
	
	private static void emulate(short[] instMem, short[] dataMem) {
		
		int cycle = 0;
		
		short[] regs = new short[8];
		
		int pc = 0;
		
		while (true) {
			
			cycle++;
			
			System.out.println();
			System.out.println("PC: " + pc);
			System.out.println("Cycles elapsed: " + cycle);
			
			boolean useAddressAsPCSource = false;
			
			short instruction = instMem[pc];
			
			int opcode = instruction >>> 13 & 0b111;
			int rs_a = instruction >>> 10 & 0b111;
			int rt_a = instruction >>> 7 & 0b111;
			int rd_a = instruction >>> 4 & 0b111;
			int func = instruction & 0b1111;
			int im = ((instruction & 0b1111111) << 25) >> 25;
			int address = instruction & 0b1111111111111;
			
			if (opcode == 0b000) {
				
				System.out.println("op rs rt rd func");
				System.out.println("---===---===----");
				System.out.println(toBinary16(instruction));
			}
			else if (opcode == 0b011) {
				
				System.out.println("op    address   ");
				System.out.println("---=============");
				System.out.println(toBinary16(instruction));
				
			}
			else {
				
				System.out.println("op rs rt   im   ");
				System.out.println("---===---=======");
				System.out.println(toBinary16(instruction));
				
			}
			
			short rs = regs[rs_a];
			short rt = regs[rt_a];
			
			switch (opcode) {
			
			case 0b000:
				
				switch (func) {
				
				case 0b0000: regs[rd_a] = (short) (rs + rt); break;
				case 0b0001: regs[rd_a] = (short) (rs - rt); break;
				case 0b0100: regs[rd_a] = (short) (rs << rt); break;
				case 0b0110: regs[rd_a] = (short) (rs >>> rt); break;
				case 0b0111: regs[rd_a] = (short) (rs >> rt); break;
				case 0b1000: regs[rd_a] = (short) (rs & rt); break;
				case 0b1001: regs[rd_a] = (short) (rs | rt); break;
				case 0b1010: regs[rd_a] = (short) (rs ^ rt); break;
				case 0b1011: regs[rd_a] = (short) (~rs); break;
				
				}
				
				break;
				
			case 0b001: regs[rt_a] = (short) (rs + im); break;
			case 0b010: regs[rt_a] = (short) (im - rs); break;
			case 0b011: useAddressAsPCSource = true; break;
			case 0b100: if (rs != rt) pc += im; break;
			case 0b101: if (rs > rt) pc += im; break;
			case 0b110: regs[rt_a] = dataMem[rs+im]; break;
			case 0b111: dataMem[rs+im] = rt; break;
			
			}
			
			regs[0] = 0;
			
			//display info
			
			System.out.println("~~~~~~~~~~~~REGS~~~~~~~~~~~~~~");
			System.out.println("  reg | data hex | data denary");
			System.out.println("------------------------------");
			
			for (int i = 0; i < 8; i++) {	
				
				System.out.println("  $" + i + "  |  0x" + String.format("%04X", regs[i]) + "  | " + regs[i]);	
			}
			
			System.out.println("~~~~~~~~~~~MEMORY~~~~~~~~~~~~~");
			System.out.println("addr  | data hex | data denary");
			System.out.println("------------------------------");
			
			for (int i = 0; i < dataMem.length; i++) {
				
				System.out.println("0x" + String.format("%04X", i) + "|  0x" + String.format("%04X", dataMem[i]) + "  | " + dataMem[i]);	
			}
			
			//infinite looping jmp command, so break from emulation
			if (useAddressAsPCSource && address == pc) break;
			
			//update program counter
			pc += 1;
			if (pc == instMem.length) pc = 0;
			
			if (useAddressAsPCSource) {
				
				pc = address; 
			}
			
		}
		
	}
	
	private static String toBinary16(short x) {

	    StringBuilder sb = new StringBuilder(16);

	    for (int i = 15; i >= 0; i--) {
	        sb.append((x >> i) & 1);
	    }

	    return sb.toString();
	}
	
}
