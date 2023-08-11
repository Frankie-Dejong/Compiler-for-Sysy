package arg;

import java.io.*;

public class Arg {
    public final String srcFileName;
    public final FileInputStream srcStream;
    public final String outPath;
    public boolean opt;

    private Arg(String src, String outPath, boolean opt) throws FileNotFoundException {
        this.srcFileName = src;
        this.srcStream = new FileInputStream(srcFileName);
        this.outPath = outPath;
        this.opt = opt;
    }

    public static Arg parse(String[] args) {
        String src = "", out = "";
        boolean opt = false;
        for (int i = 0;i < args.length;i ++) {
            if (args[i].equals("-o")) {
                out = args[i + 1];
                src = args[i + 2];
            }

            if (args[i].equals("-O1")) {
                opt = true;
            }
        }

        try {
            Arg arg = new Arg(src, out, opt);
            return arg;
        } catch (FileNotFoundException e) {
            printHelp();
            throw new RuntimeException(e);
        }

    }

    public static void printHelp() {
        System.err.println("Usage: compiler {(-S|-emit-llvm) -o filename} filename -On [options...]");
        System.err.println("optimize level: 0, 1 (default), 2");
    }
}
