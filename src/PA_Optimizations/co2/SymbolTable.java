package co2;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {
    HashMap<Symbol, String> tableTest;

    public SymbolTable () {
        tableTest = new HashMap<>();

        tableTest.put(new Symbol(new Token("readInt", 0, 0)), ":int");
        tableTest.put(new Symbol(new Token("readFloat", 0, 0)), ":float");
        tableTest.put(new Symbol(new Token("readBool", 0, 0)), ":bool");
        tableTest.put(new Symbol(new Token("printInt", 0, 0)), "int:void");
        tableTest.put(new Symbol(new Token("printFloat", 0, 0)), "float:void");
        tableTest.put(new Symbol(new Token("printBool", 0, 0)), "bool:void");
        tableTest.put(new Symbol(new Token("println", 0, 0)), ":void");
        tableTest.put(new Symbol(new Token("arrcpy", 0, 0)), "[],[],int:void");
    }

    // lookup name in SymbolTable
    public String lookup (Symbol name) throws SymbolNotFoundError {
        for (Symbol s : tableTest.keySet()) {
            if (s.token().lexeme().equals(name.token().lexeme())) {
                return tableTest.get(s);
            }
        }

        throw new SymbolNotFoundError(name.token().lexeme());
    }

    public String lookup (Symbol name, String argumentList) throws SymbolNotFoundError {
        boolean foundFunction = false;
        // System.out.println("Looking up: " + name.token().lexeme() + ", with args list=" + argumentList);

        for (Symbol s : tableTest.keySet()) {
            // System.out.println("checking against " + s.token().lexeme());
            if (s.token().lexeme().equals(name.token().lexeme())) {
                String registeredArgs = (tableTest.get(s).split(":")[0]+":").toLowerCase();
                String varsArgumentList;
                
                if ((argumentList.length() == 1)) {
                    varsArgumentList = ":";
                }
                else {
                    varsArgumentList = argumentList.split(":")[0]+":";
                }
                
                // System.out.println("Found matching name: " + name.token().lexeme() + " now checking=" + registeredArgs + ", vs=" + varsArgumentList);
                foundFunction = true;
                if (registeredArgs.equals(varsArgumentList)) {
                    return tableTest.get(s);
                }
            }
        }
        if (foundFunction) {
            throw new SymbolNotFoundError(name.token().lexeme(), true);    
        }
        throw new SymbolNotFoundError(name.token().lexeme());
    }


    // insert name in SymbolTable
    public Symbol insert (Symbol name) throws RedeclarationError {
        // check for redeclaration
        for (Symbol s : tableTest.keySet()) {
            if (s.token().lexeme().equals(name.token().lexeme())) {
                throw new RedeclarationError(name.token().lexeme());
            }
        }

        tableTest.put(name, "void");
        return name;
    }

    public Symbol insert (Symbol name, String returnType) throws RedeclarationError {
        // check for redeclaration
        for (Symbol s : tableTest.keySet()) {
            if (s.token().lexeme().equals(name.token().lexeme())) {
                if (tableTest.get(s).equals(returnType)) {
                    if (tableTest.get(s).contains(":")) {
                        throw new RedeclarationError(name.token().lexeme());
                    }
                }
            }
        }

        // System.out.println("Inserted function on symbol: " + name.token().lexeme() + "," + returnType);
        tableTest.put(name, returnType);
        return name;
    }

    public HashMap<Symbol, String> getDefinedSymbols() {
        return tableTest;
    }
}

// class SymbolNotFoundError extends Error {

//     private static final long serialVersionUID = 1L;
//     private final String name;

//     public SymbolNotFoundError (String name) {
//         super("Symbol " + name + " not found.");
//         this.name = name;
//     }

//     public String name () {
//         return name;
//     }
// }

// class RedeclarationError extends Error {

//     private static final long serialVersionUID = 1L;
//     private final String name;

//     public RedeclarationError (String name) {
//         super("Symbol " + name + " being redeclared.");
//         this.name = name;
//     }

//     public String name () {
//         return name;
//     }
// }
