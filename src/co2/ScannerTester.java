package co2;

import java.io.FileReader;
import java.io.IOException;

// IMPORTANT: You need to put jar files in lib/ in your classpath: at the minimum commons-cli-1.5.0.jar
import org.apache.commons.cli.*;

public class ScannerTester {

    public static void main (String[] args) {
        Options options = new Options();
        options.addRequiredOption("s", "src", true, "Source File");


        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser cmdParser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = cmdParser.parse(options, args);
        } catch (ParseException e) {
            formatter.printHelp("All Options", options);
            System.exit(-1);
        }

        co2.Scanner s = null;
        String sourceFile = cmd.getOptionValue("src");

        try {
            s = new co2.Scanner(new FileReader(sourceFile));
        }
        catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error accessing the code file: \"" + sourceFile + "\"");
            System.exit(-2);
        }

        Token t;
        while (s.hasNext()) {
            t = s.next();
            System.out.print(t.kind);
            switch (t.kind) {
                case INT_VAL:
                case FLOAT_VAL:
                case IDENT:
                    System.out.println("\t" + t.lexeme());
                    break;
                default:
                    System.out.println();
                    break;
            }
        }
    }
}
