/*
 * see license.txt
 */
package lita.vm;

import java.io.File;
import java.nio.file.Files;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

/**
 * The main entry point to the LitaVM
 * 
 * @author Tony
 *
 */
public class LitaVM {
    public static final String VERSION = "v0.1-alpha";
    
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("h", "help", false, "Displays the help contents");
        options.addOption("v", "version", false, "Displays the version");
        options.addOption("d", "debug", false, "Displays debug information");
        options.addOption("f", "file", true, "The assembly file to run");
        
        options.addOption("sx", "stack", true, "Specifies the stack size (in bytes) of the VM, defaults to 1024 bytes");
        options.addOption("rx", "ram", true, "Specifies the amount of RAM size (in bytes) of the VM, defaults to 1 MiB");
        
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        
        if(cmd.hasOption("h") || cmd.getOptions().length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "litavm", options );
        }
        
        if(cmd.hasOption("v")) {
            System.out.println(VERSION);
        }
        
        boolean debugMode = cmd.hasOption("d");        
        
        int stackSize = Integer.parseInt(cmd.getOptionValue("sx", "1024"));
        int ramSize   = Integer.parseInt(cmd.getOptionValue("rx", "1048576"));
        
        if(cmd.hasOption("file")) {
            String inputFilePath = cmd.getOptionValue("file");
            File inputFile = new File(inputFilePath);
            
            if(!inputFile.exists()) {
                inputFile = new File(System.getProperty("user.dir"), inputFilePath);
                if(!inputFile.exists()) {
                    System.out.println("The input file '" + inputFilePath + "' does not exist.");
                    System.exit(1);
                }
            }
            
            LitaVM vm = new LitaVM(ramSize, stackSize, debugMode);
            
            Assembler asm = new Assembler(vm);
            try {
                Bytecode bytecode = asm.compile(new String(Files.readAllBytes(inputFile.toPath()), "UTF8"));
                vm.execute(bytecode);
            }
            catch(ParserException e) {
                System.err.println("Parsing Error >> " + e.getMessage());
                if(debugMode) {
                    throw e;
                }
            }
            catch(EvalException e) {
                System.err.println("Evaluation Error >> " + e.getMessage());
                if(debugMode) {
                    throw e;
                }
            }
        }
    }
    
    
    private final RAM ram;
    private final CPU32 cpu;
    private final boolean debugMode;
    
    /**
     * @param ram
     * @param stackSize
     * @param debugMode 
     */
    public LitaVM(int ramSize, int stackSize, boolean debugMode) {        
        if(stackSize > ramSize) {
            throw new IllegalArgumentException("Stack size is bigger than RAM amount");
        }
    
        this.ram = new RAM(ramSize);
        this.cpu = new CPU32(this.ram, stackSize);
        
        this.debugMode = debugMode;
    }
    
    /**
     * @return the cpu
     */
    public CPU32 getCpu() {
        return cpu;
    }
    
    /**
     * @return the ram
     */
    public RAM getRam() {
        return ram;
    }

    /**
     * Execute the supplied {@link Bytecode}
     * 
     * @param code
     */
    public void execute(Bytecode code) {
        if(this.debugMode) {
            printInstructions(code);
        }
        
        this.cpu.execute(code);
    }
    
    private void printInstructions(Bytecode code) {
        StringBuilder sb = new StringBuilder();
        int pc = code.pc;
        while(pc < code.length) {
            Instruction.print(cpu, sb, code.instr[pc++]);
        }
        
        System.out.println(sb);
    }
    
}
