// package com.palande.aum.scanner;
package co2;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import co2.Token.Kind;

public class Scanner implements Iterator<Token> {

    private BufferedReader input;   // buffered reader to read file
    private boolean closed; // flag for whether reader is closed or not

    private int lineNum = 1;    // current line number
    private int charPos;    // character offset on current line

    private String scan;    // current lexeme being scanned in
    private int nextChar;   // contains the next char (-1 == EOF)

    private int previousChar; // contains the previous char read
    private boolean overstepped = false; // determines whether the previous char should be included or not
    private boolean returnedEOFToken = false; // determines whether the EOF has been returned

    // reader will be a FileReader over the source file
    public Scanner (Reader reader) {
        input = new BufferedReader(reader);
        closed = false;
    }

    // signal an error message
    public void Error (String msg, Exception e) {
        System.err.println("Scanner: Line - " + lineNum + ", Char - " + charPos);
        if (e != null) {
            e.printStackTrace();
        }
        System.err.println(msg);
    }

    /*
     * helper function for reading a single char from input
     * can be used to catch and handle any IOExceptions,
     * advance the charPos or lineNum, etc.
     */
    private int readChar () {
        try {
            nextChar = input.read();

            if (((char) nextChar == '\n') || nextChar == 10) {
                lineNum++;
                charPos = 0;
            } else {
                charPos++;
            }
        } catch (IOException e) {
            this.Error("Read next char", e);
        }
        return nextChar;
    }

    /*
     * function to query whether or not more characters can be read
     * depends on closed and nextChar
     */
    @Override
    public boolean hasNext () {
        // File is not closed and next char is not EOF
        return (!returnedEOFToken);
    }

    /*
     *	returns next Token from input
     *
     *  invariants:
     *  1. call assumes that nextChar is already holding an unread character
     *  2. return leaves nextChar containing an untokenized character
     *  3. closes reader when emitting EOF
     */
    @Override
    public Token next () {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        Token currToken;
        if (overstepped) {
            currToken = new Token("" + ((char)previousChar), 0, 0);
        }
        else {
            currToken = new Token("", 0, 0);
        }
        overstepped = false;
        Token previousGoodToken = currToken;
        scan = currToken.lexeme();
        if ((nextChar == -1)) {
            returnedEOFToken = true;
            return (new Token("EOF", lineNum, charPos));
        }
        nextChar = readChar();
        // if ((nextChar == -1)) {
        //     returnedEOFToken = true;
        //     return (new Token("EOF", lineNum, charPos));
        // }

        // read char until hits error
        while ((currToken.kind() != Kind.ERROR)) {
            // System.out.println("Current lex is " + currToken.lexeme() + " and type is " + currToken.kind());
            // System.out.println("About to read in next: " + nextChar);
            // System.out.println("About to read in next: " + (char)nextChar);
            // 9 == horizonal tab, 10 == lf, 13 == cr, 32 == space, 
            if ((nextChar != 9) && (nextChar != 10) && (nextChar != 13) && (nextChar != 32) && (nextChar != -1)) {
                scan += ((char) nextChar);

                // line comments
                if (currToken.kind() == Kind.DIV) {
                    if ((previousChar == nextChar) && ((char)nextChar == '/')) {
                        // read chars until you hit a new line
                        while (nextChar != 10) {
                            previousChar = nextChar;
                            nextChar = readChar();
                            if (nextChar == -1) {
                                return new Token("No */", lineNum, charPos);
                            }
                        }
                        overstepped = false;
                        scan = "";
                        currToken = new Token("", lineNum, charPos);
                        continue;
                    }
                }
                
                // block comments
                if (((char) previousChar == '/') && ((char) nextChar == '*')) {
                    while (((char)previousChar != '*') || ((char)nextChar != '/')) {
                        previousChar = nextChar;
                        nextChar = readChar();
                        if (nextChar == -1) {
                            return new Token("No */", lineNum, charPos);
                        }
                    }
                    overstepped = false;
                    scan = "";
                    
                    previousChar = nextChar;
                    nextChar = readChar();
                    currToken = new Token("", lineNum, charPos);
                    if (nextChar == -1) {
                        returnedEOFToken = true;
                        return new Token("EOF", lineNum, charPos);
                    }
                    continue;
                }
            }
            else if ((nextChar == -1) && (currToken.kind() == Kind.EMPTY)) {
                // System.out.println("EXIT");
                returnedEOFToken = true;
                return new Token("EOF", lineNum, charPos);
            }
            else if (!(currToken.lexeme().equals(""))) {
                // if hits a space or smth, return
                if (((currToken.lexeme().charAt(currToken.lexeme().length()-1)) == '.') && (currToken.kind() != Kind.PERIOD)) {
                    scan = "";
                    overstepped = false;
                    return new Token("Err, float double", lineNum, charPos);
                }
                overstepped = false;
                return currToken;
            }

            previousGoodToken = currToken;
            currToken = new Token(scan, lineNum, charPos);
            if (currToken.kind != Kind.ERROR) {
                previousChar = nextChar;
                nextChar = readChar();    
            }
        }
        
        if (nextChar == -1) {
            returnedEOFToken = true;
            return (new Token("EOF", lineNum, charPos));
        }

        previousChar = nextChar;
        overstepped = true;
        return previousGoodToken;
    }
}
