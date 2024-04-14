import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

// Compiler for Jack programs
// Input a directory name

public class JackCompiler {

    String lineSeparator = System.getProperty("line.separator");

    public static void main(String[] args) {
	if (args.length > 0) {
	    String dirName = args[0];
	    System.out.println("Attempting to open folder at: " + dirName);
	    ArrayList<String> filenames = getFileNames(dirName);
	    
	    for (String f:filenames) {
		//Create tokenizer from file
		Tokenizer t = new Tokenizer(f);
		//Create CompileParser with Tokenizer
		CompileParser CP = new CompileParser(t, new SymbolTable());
		
		//put CompilerParser to parse recursively
		CP.compileClass();
		//Get resulting file and write to disk
		String res = CP.getFileStr();
		String outputPath = f.replace(".jack", ".vm"); //Replace .xml with .vm for final version
		try (FileWriter writer = new FileWriter(outputPath)) {
		    writer.write(res);
		} catch (IOException e) {
		    e.printStackTrace();
		}
		//Print Tokenizer input
		/*
		t.resetIndex();
		StringBuilder b = new StringBuilder();
		while (t.hasMoreTokens()){
		    b.append(t.getToken()+", type: "+t.getType()+"\n");
		    t.advance();
		}
		String res2 = b.toString();
		String outputPath2 = f.replace(".jack", "Tokens.txt"); 	
		try (FileWriter writer = new FileWriter(outputPath2)) {
		    writer.write(res2);
		} catch (IOException e) {
		    e.printStackTrace();
		}
		*/
	    }
	}
    }

    public static ArrayList<String> getFileNames (String directory) {
	ArrayList<String> res = new ArrayList<>();
	
	Path dirPath = Paths.get(directory);
	try (Stream<Path> paths = Files.walk(dirPath)) {
	    res = paths
		.filter(Files::isRegularFile)
		.filter(path -> path.toString().endsWith(".jack"))
		.map(Path::toString) // Convert Path to String
		.collect(Collectors.toCollection(ArrayList::new));
	} catch (IOException e) {
	    e.printStackTrace();
	}
	for (String f2:res) {
	    System.out.println("Filenames found in dir: " + f2); 
	}
	return res;
    }

}

class Tokenizer {
    HashMap<Integer, String[]> tokens = new HashMap<>();
    int tokenIndex = 0;

    String symbols = "{}()[].,;+-*/&|<>=~";

    public Tokenizer (String filename) {
	try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
	    StringBuilder fileContent = new StringBuilder();
	    String line;
	    boolean isMultiLineComment = false;

	    while ((line = reader.readLine()) != null) {

		line.trim();
		if (line.isEmpty()) {
		    continue;
		}
		if (isMultiLineComment){
		    if (line.contains("*/")) {
			isMultiLineComment = false;
		    }
		    continue;
		} else {
		    if (line.contains("/*")) {
			isMultiLineComment = true;
			if (line.contains("*/")) {
			    isMultiLineComment = false;
			}
			continue;
		    }
		    if (line.charAt(0) == '/') {
			continue;
		    }  
		    String[] temp = line.split("//");
		    String tempWrite = temp[0];
		    fileContent.append(tempWrite);
		}
	    }
	    String strF = fileContent.toString();

	    String currentToken = "";
	    boolean processingStringC = false;
	    
	    //Do stuff to file by chars
	    for (int i = 0; i < strF.length(); i++) {
		char c = strF.charAt(i);
		//String Constant related
		if (processingStringC) {
		    if (c == '"') {
			//End token
			currentToken += c;
			tokens.put(tokenIndex, new String[]{currentToken, "StringConstant"});
                        tokenIndex++;
                        currentToken = "";
			processingStringC = false;
		    } else {
			currentToken += c;
		    }
		    continue;
		}
		if (c == '"') {
		    currentToken += c;
		    processingStringC = true;
		}

		//Rest of the chars:
		else if (symbols.indexOf(c) >= 0) { 
                    if (!currentToken.isEmpty()) {
                        tokens.put(tokenIndex, new String[]{currentToken, determineTokenType(currentToken)});
                        tokenIndex++;
                        currentToken = "";
                    }
                    tokens.put(tokenIndex, new String[]{Character.toString(c), "symbol"});
                    tokenIndex++;
                } else if (c == ' ') {
                    if (!currentToken.isEmpty()) {
                        tokens.put(tokenIndex, new String[]{currentToken, determineTokenType(currentToken)});
                        tokenIndex++;
                        currentToken = "";
                    }
                } else if (c=='\n' || c=='\t') {
		    continue;
		} else {
                    currentToken += c; 
                }
	    }

	} catch (IOException e) {
	    System.err.println("an error reading file: "+e.getMessage());
	}
	tokenIndex = 0;
    }
    
    String determineTokenType(String token) {
	if (token.charAt(0) == '"' && token.charAt(token.length()-1) == '"') {
	    return "StringConstant";
	} 
	else try {
	    Integer.parseInt(token);
	    return "integerConstant";
	} catch (Exception e) {
	    //System.out.println("");
	}
	if (token.equals("class") ||
	    token.equals("constructor") ||
	    token.equals("function") ||
	    token.equals("method") ||
	    token.equals("field") ||
	    token.equals("static") ||
	    token.equals("var") ||
	    token.equals("int") ||
	    token.equals("char") ||
	    token.equals("boolean") ||
	    token.equals("void") ||
	    token.equals("true") ||
	    token.equals("false") ||
	    token.equals("null") ||
	    token.equals("this") ||
	    token.equals("let") ||
	    token.equals("do") ||
	    token.equals("if") ||
	    token.equals("else") ||
	    token.equals("while") ||
	    token.equals("return")) {
	    
	    return "keyword";
	} else {
	    return "identifier";
	}
    }

    public String getToken() {
	return tokens.get(tokenIndex)[0];
    }

    public String getType() {
	return tokens.get(tokenIndex)[1];
    }

    public void advance() {
	tokenIndex++;
    }
    
    public boolean hasMoreTokens() {
	if (tokenIndex < tokens.size()) {
	    return true;
	} else return false;
    }
    public String checkNext() {
	if (hasMoreTokens()) {
	    return tokens.get(tokenIndex+1)[0];
	} else return "nullNext";
    }
    public void resetIndex() {
	tokenIndex = 0;
    } 
}

class CompileParser {
    Tokenizer t;
    StringBuilder b;
    SymbolTable st;
    
    String className;
    int runningLoopIndex;
    int currentArgs;
    HashMap<Character, String> charCodes;

    public CompileParser (Tokenizer _t, SymbolTable _st) {
	t = _t;
	b = new StringBuilder();
	st = _st;
	runningLoopIndex = 0;
	currentArgs = 0;

	charCodes = new HashMap<>();
        initializeCharCodes();
    }
    
    private void initializeCharCodes() {
        // Adding ASCII codes for space and special characters
        charCodes.put(' ', "32");
        charCodes.put('!', "33");
        charCodes.put('"', "34");
        charCodes.put('#', "35");
        charCodes.put('$', "36");
        charCodes.put('%', "37");
        charCodes.put('&', "38");
        charCodes.put('\'', "39");
        charCodes.put('(', "40");
        charCodes.put(')', "41");
        charCodes.put('*', "42");
        charCodes.put('+', "43");
        charCodes.put(',', "44");
        charCodes.put('-', "45");
        charCodes.put('.', "46");
        charCodes.put('/', "47");

        // Adding ASCII codes for numbers
        for (int i = 0; i < 10; i++) {
            charCodes.put((char) ('0' + i), String.valueOf(48 + i));
        }

        // Adding ASCII codes for special characters
        charCodes.put(':', "58");
        charCodes.put(';', "59");
        charCodes.put('<', "60");
        charCodes.put('=', "61");
        charCodes.put('>', "62");
        charCodes.put('?', "63");
        charCodes.put('@', "64");

        // Adding ASCII codes for uppercase letters
        for (int i = 0; i < 26; i++) {
            charCodes.put((char) ('A' + i), String.valueOf(65 + i));
        }

        // Adding ASCII codes for lowercase letters
        for (int i = 0; i < 26; i++) {
            charCodes.put((char) ('a' + i), String.valueOf(97 + i));
        }

        // Adding ASCII codes for the rest of the special characters
        charCodes.put('[', "91");
        charCodes.put('\\', "92");
        charCodes.put(']', "93");
        charCodes.put('^', "94");
        charCodes.put('_', "95");
        charCodes.put('`', "96");
        charCodes.put('{', "123");
        charCodes.put('|', "124");
        charCodes.put('}', "125");
        charCodes.put('~', "126");
    }

    public String getFileStr() {
	return b.toString();
    }

    public void compileClass() {
	st.resetClass();

	String cToken = t.getToken();
	if (!cToken.equals("class")) {
	    System.err.println("Class defined unproperly: "+cToken);
	    return;
	} else {
	    t.advance();
	    className = t.getToken();
	    cToken = t.getToken();
	    t.advance();
	    cToken = t.getToken();
	    t.advance();
	    //class variables
	    while (t.getToken().equals("field") || t.getToken().equals("static")){
		compileClassVarDec();
	    }
	    while (t.getToken().equals("constructor") || t.getToken().equals("function") || t.getToken().equals("method")) {
		compileSubroutine();
	    }
	    cToken = t.getToken();
	}
    } 

    void compileClassVarDec() {
	String dest = t.getToken();
	if (dest.equals("field")) { dest = "this";}

	t.advance();
	String type = t.getToken();

	t.advance();
	String name = t.getToken();
	//Insert to classtable
	st.insertClassEntry(name, type, dest);
	//Check for multiple var declaration
	t.advance();
	while (!t.getToken().equals(";")) {
	    t.advance();
	    name = t.getToken();
	    st.insertClassEntry(name, type, dest);
	    t.advance();
	}
	t.advance();
    }

    void compileSubroutine() {
	st.resetSubroutine();
	String subType = t.getToken();
	t.advance();

	if (subType.equals("method")){
	    st.insertSubroutineEntry("this", className, "argument");
	}
	// 'void' or type
	String returnType = t.getToken();
	if (returnType.equals("void") || returnType.equals("int") || returnType.equals("char") || returnType.equals("boolean")) {
	    //b.append("<keyword>"+cToken+"</keyword>\n");
	} else {
	    //b.append("<identifier>"+cToken+"</identifier>\n");
	}
	t.advance();

	// subroutineName
	String subName = t.getToken();
	t.advance();

	// '(' parameterList ')'
	t.advance();
	compileParameterList();
	t.advance();
	
	t.advance();
	// varDec*
	while (t.getToken().equals("var")) {
	    compileVarDec();
	}
	b.append("function "+className+"."+subName+" "+Integer.toString(st.getLocalCount())+"\n");
	if (subType.equals("constructor")) {
	    b.append("push constant "+Integer.toString(st.getFieldCount())+"\n");
	    b.append("call Memory.alloc 1\n");
	    b.append("pop pointer 0\n");
	}
	if (subType.equals("method")) {
	    b.append("push argument 0\n");
	    b.append("pop pointer 0\n");
	}
	// statements
	compileStatements();

	t.advance();
    }

    void compileParameterList() {
	// parameterList: ((type varName) (',' type varName)*)?
	String cToken = t.getToken();
	String type;
	String name;
	while (!cToken.equals(")")) { // stop at closing parenthesis
	    if (cToken.equals(",")) {
		// ',' in list
	    } else {
		// type varName
		type = t.getToken();
		t.advance();
		// varName
		name = t.getToken();
		st.insertSubroutineEntry(name, type, "argument");
	    }
	    t.advance();
	    cToken = t.getToken();
	}
    }

    void compileSubroutineBody() {
	t.advance();
	// varDec*
	while (t.getToken().equals("var")) {
	    compileVarDec();
	}

	
    }

    void compileVarDec() {
	t.advance();

	// type
	String type = t.getToken();
	t.advance();

	// varName
	String name = t.getToken();
	t.advance();
	
	st.insertSubroutineEntry(name, type, "local");
	// (',' varName)*
	while (t.getToken().equals(",")) {
	    t.advance();
	    name = t.getToken();
	    st.insertSubroutineEntry(name, type, "local");
	    t.advance();
	}
	t.advance();
    }

    void compileStatements() {
	// statement*
	String cToken = t.getToken();
	while (!t.getToken().equals("}")) {
	    compileStatement();
	}
    }

    void compileStatement() {
	// Determines the type of statement and calls the appropriate method
	String cToken = t.getToken();
	switch (cToken) {
	    case "let":
		compileLet();
		break;
	    case "if":
		compileIf();
		break;
	    case "while":
		compileWhile();
		break;
	    case "do":
		compileDo();
		break;
	    case "return":
		compileReturn();
		break;
	    default:
		throw new IllegalStateException("Unexpected statement: " + cToken);
	}
    }

    void compileLet() {
	//let
	t.advance();
	String name = t.getToken();	
	boolean isArrayAcc = false;

	t.advance();
	//Arrays
	if (t.getToken().equals("[")) {
	    isArrayAcc = true;
	    t.advance();
	    compileExpression();
	    t.advance();
	}
	//EQ-symbol
	t.advance();
	//Compile set value
	compileExpression();
	if (isArrayAcc) {
	    b.append("pop temp 0\n");
	    b.append("push "+st.checkForElement(name)+"\n");
	    b.append("add\n");
	    b.append("pop pointer 1\n");
	    b.append("push temp 0\n");
	    b.append("pop that 0\n");
	    
	} else {
	    b.append("pop "+st.checkForElement(name)+"\n");
	}
	t.advance();
    }

    void compileIf() {
	int i = runningLoopIndex;
	runningLoopIndex++;

	//if
	t.advance();
	
	//(expression)
	t.advance();
	compileExpression();
	t.advance();
	
	b.append("not\n");
	b.append("if-goto "+className+Integer.toString(i)+"L1\n");
	t.advance();
	compileStatements();

	t.advance();
	b.append("goto "+className+Integer.toString(i)+"L2\n");
	b.append("label "+className+Integer.toString(i)+"L1\n");

	if (t.getToken().equals("else")) {
	    //else {statements}
	    t.advance();
	    t.advance();
	    compileStatements();
	    t.advance();
	}
	b.append("label "+className+Integer.toString(i)+"L2\n");
    }
    void compileWhile() {
	int i = runningLoopIndex;
	runningLoopIndex++;
	b.append("label "+className+Integer.toString(i)+"L1\n");
	t.advance();
	
	t.advance();
	compileExpression();
	b.append("not\n");
	b.append("if-goto "+className+Integer.toString(i)+"L2\n");
	t.advance();
	t.advance();
	compileStatements();
	t.advance();
	b.append("goto "+className+Integer.toString(i)+"L1\n");
	b.append("label "+className+Integer.toString(i)+"L2\n");

    }

    void compileDo() {
	t.advance();
	
	compileExpression();
	t.advance();
	//We append this to get rid of the return value; do doesn't care about it!
	b.append("pop temp 0\n");
    }

    void compileReturn() {
	t.advance();
	
	if (!t.getToken().equals(";")) {
	    compileExpression();
	} else {
	    b.append("push constant 0\n");
	}
	
	t.advance();

	b.append("return\n");
    }
    void compileExpression() {
	compileTerm();
	
	while (t.getToken().matches("\\+|-|\\*|/|&|\\||<|>|=")) {
	    String op = t.getToken();
	    t.advance();
	    compileTerm();
	    switch (op) {
		case "+":
		    b.append("add\n");
		    break;
		case "-":
		    b.append("sub\n");
		    break;
		case "*":
		    b.append("call Math.multiply 2\n");
		    break;
		case "/":
		    b.append("call Math.divide 2\n");
		    break;
		case "&":
		    b.append("and\n");
		    break;
		case "|":
		    b.append("or\n");
		    break;
		case "<":
		    b.append("lt\n");
		    break;
		case ">":
		    b.append("gt\n");
		    break;
		case "=":
		    b.append("eq\n");
		    break;
		
	    }
	}
	
    }

    void compileTerm() {
	String cToken = t.getToken();
	String cType = t.getType();
	if (cType.equals("integerConstant"))  {
	    b.append("push constant "+cToken+"\n");
	    t.advance();
	}
	else if (cType.equals("StringConstant")) {
	    //Create new String object and return the addr
	    String strLen = Integer.toString(cToken.length());
	    b.append("push constant "+strLen+"\n");
	    b.append("call String.new 1\n");
	    for (char c:cToken.toCharArray()) {
		String charcode = charCodes.get(c);
		b.append("push constant "+ charcode+"\n");
		b.append("call String.appendChar 2\n");
	    }
	    t.advance();
	} else if (cType.equals("keyword")) {
	    switch (cToken) {
		case "true":
		    b.append("push constant 1\n");
		    b.append("neg\n");
		    break;
		case "false":
		    b.append("push constant 0\n");
		    break;
		case "null":
		    b.append("push constant 0\n");
		    break;
		case "this":
		    b.append("push pointer 0\n");
		    break;
		default:
		    break;
	    }
	    t.advance();

	} else if (cToken.equals("(")) {
	    t.advance();
	    compileExpression();
	    t.advance();
	} else if (cToken.matches("-|~")) {
	    t.advance();
	    compileTerm();
	    if (cToken.equals("-")){
		b.append("neg\n");
	    } else {
		b.append("not\n");
	    }
	} else {
	    String varname = t.getToken();
	    t.advance();
	    switch (t.getToken()) {
		case "[":
		    //Array Accessing
		    t.advance();
		    b.append("push "+st.checkForElement(varname)+"\n");
		    compileExpression();
		    b.append("add\n");
		    b.append("pop pointer 1\n");
		    b.append("push that 0\n");
		    t.advance();
		    break;
		case ".":
		    t.advance();
		    String subroutineName = t.getToken();
		    String test = null;
		    try {
			test = st.checkForElement(varname);
			//we have a method call
			b.append("push "+test+"\n");
			t.advance();
			t.advance();
			compileExpressionList();
			t.advance();
			b.append("call "+st.checkTypeOf(varname)+"."+subroutineName+" "+Integer.toString(currentArgs+1)+"\n");
		    } catch (Exception e) {
			//functiono/constructor call
			t.advance();
			t.advance();
			compileExpressionList();
			t.advance();
			b.append("call "+varname+"."+subroutineName+" "+Integer.toString(currentArgs)+"\n");
		    }
		    break;
		case "(":
		    //This is a method call inside the object itself
		    b.append("push pointer 0\n");
		    t.advance();
		    compileExpressionList();
		    t.advance();
		    b.append("call " +className+"."+varname+" "+Integer.toString(currentArgs+1)+"\n");
		    break;
		default:
		    b.append("push "+st.checkForElement(varname)+"\n");
		    break;
	    }
	}
    }

    void compileExpressionList() {
	//CurrentArgs WILL Break if someone puts a subroutine call inside a subroutine call's expression list
	//I'm not fixing this
	currentArgs = 0;
	if (t.getToken().equals(")")){
	    return;
	}
	compileExpression();
	currentArgs ++;
	while (t.getToken().equals(",")) {
	    t.advance();
	    compileExpression();
	    currentArgs++;
	}
	
    }
}

class SymbolTableEntry {
    private String name;
    private String type;
    private String kind;
    private int index;

    public SymbolTableEntry(String n, String t, String k, int i) {
	name = n;
	type = t;
	kind = k;
	index = i;
    }
    public String getName(){
	return name;
    }
    public String getType(){
	return type;
    }
    public String getKind(){
	return kind;
    }
    public int getIndex(){
	return index;
    }
}

class SymbolTable {
    private List<SymbolTableEntry> classTable;
    private List<SymbolTableEntry> subroutineTable;

    private int staticIndex;
    private int thisIndex;
    private int localIndex;
    private int argumentIndex;

    public SymbolTable () {
	classTable = new ArrayList<>();
	subroutineTable = new ArrayList<>();

	staticIndex = 0;
	thisIndex = 0;
	localIndex =0;
	argumentIndex = 0;
    }

    public void insertClassEntry(String n, String t, String k) {
	if (k.equals("this")) {
	    classTable.add(new SymbolTableEntry(n, t, k, thisIndex));
	    thisIndex++;
	} else if (k.equals("static")) {
	    classTable.add(new SymbolTableEntry(n, t, k, staticIndex));	    
	    staticIndex++;
	} else {
	    throw new IllegalArgumentException("Trying to insert sometihng wrong to symboltable");
	}
    }
    public void insertSubroutineEntry(String n, String t, String k) {
	if (k.equals("local")) {
	    subroutineTable.add(new SymbolTableEntry(n, t, k, localIndex));
	    localIndex++;
	} else if (k.equals("argument")) {
	    subroutineTable.add(new SymbolTableEntry(n, t, k, argumentIndex));
	    argumentIndex++;
	} else {
	    throw new IllegalArgumentException("Trying to insert sometihng wrong to symboltable");
	}
    }

    public void resetClass(){
	classTable = new ArrayList<>();
	subroutineTable = new ArrayList<>();
	staticIndex = 0;
	thisIndex = 0;
	localIndex = 0;
	argumentIndex = 0;
    }
    
    public void resetSubroutine() {
	subroutineTable = new ArrayList<>();
	localIndex = 0;
	argumentIndex = 0;
    }

    public String checkTypeOf (String identifier) {
    	for (SymbolTableEntry ste:subroutineTable) {
	    if (ste.getName().equals(identifier)) {
		String res = ste.getType();
		return res;
	    }
	}
	for (SymbolTableEntry ste:classTable) {
	    if (ste.getName().equals(identifier)) {
		String res = ste.getType();
		return res;
	    }
	}
	//RAISE ERROR HERE; STOP PROGRAM!!!
        throw new IllegalArgumentException("Identifier not found: " + identifier);

    }
    public String checkForElement (String identifier) {
	// First check in subroutineTable
	// Then in ClassTable
	// If not found, raise error
	for (SymbolTableEntry ste:subroutineTable) {
	    if (ste.getName().equals(identifier)) {
		String res = ste.getKind() + " " + Integer.toString(ste.getIndex());
		return res;
	    }
	}
	for (SymbolTableEntry ste:classTable) {
	    if (ste.getName().equals(identifier)) {
		String res = ste.getKind() + " " + Integer.toString(ste.getIndex());
		return res;
	    }
	}
	//RAISE ERROR HERE; STOP PROGRAM!!!
        throw new IllegalArgumentException("Identifier not found: " + identifier);
    }
    public int getArgCount() {
	int count = 0;
	for (SymbolTableEntry e:subroutineTable) {
	    if (e.getKind().equals("argument")) { count++;}
	}
	return count;
    }
    public int getFieldCount() {
	int count = 0;
	for (SymbolTableEntry e:classTable) {
	    if (e.getKind().equals("this")) { count++;}
	}
	return count;
    }
    public int getLocalCount() {
	int count =0;
	for (SymbolTableEntry e:subroutineTable){
	    if (e.getKind().equals("local")) { count++; }
	}
	return count;
    }

}
