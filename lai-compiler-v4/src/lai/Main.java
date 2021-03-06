package lai;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import lai.ast.LaiFile;

public class Main {

	private static String getCarrotPointer(int offset) {
		String s = "";
		for (int i = 0; i < offset; ++i) {
			s += " ";
		}
		s += "^";
		return s;
	}

	public static void error(String filename, int lineNumber, int charNumber, String message, String offendingLine) {
		System.out.println("Error in file '" + filename + "(" + (lineNumber + 1) + ")" + "':");
		System.out.println("\t" + offendingLine.replace("\t", ""));
		System.out.println("\t" + getCarrotPointer(charNumber));
		System.out.println(message);
		System.out.println("");

		Main.compilerErrors++;
	}

	public static void error(String filename, int lineNumber, int charNumber, String message) {
		// Find file
		int fileIndex = -1;
		for (int i = 0; i < filenames.size(); ++i) {
			String s = filenames.get(i);
			if (s.equals(filename)) {
				fileIndex = i;
				break;
			}
		}
		if (fileIndex == -1) {
			// This should pretty much never happen for any reason, but you never know.
			System.out.println("Error in unloaded file: " + filename + "(line=" + (lineNumber + 1) + ", char="
					+ charNumber + "):\n" + message);
		}
		String line = filesContent.get(fileIndex).get(lineNumber);
		error(filename, lineNumber, charNumber, message, line);
	}

	static ArrayList<String> flags = new ArrayList<String>();
	static ArrayList<String> filenames = new ArrayList<String>();

	static ArrayList<ArrayList<String>> filesContent = new ArrayList<>();

	static int compilerErrors = 0;

	public static void main(String[] args) {

		/********************************/
		/* Parse Command Line Arguments */
		/********************************/

		// Verify that a file has been provided as a command line arg.
		// Other args are a WIP, for now we will just focus on compiling.

		if (args.length == 0) {
			System.out.println("You must specify a file or argument.");
			System.exit(0);
		}

		// Sort the arguments into flags and files.

		// Flags are anything that begin with a '-'.
		for (String s : args) {
			if (s.charAt(0) == '-') {
				flags.add(s);
			} else {
				filenames.add(s);
			}
		}

		/*************************/
		/* Load All Source Files */
		/*************************/
		for (String filename : filenames) {
			ArrayList<String> list = new ArrayList<String>();
			System.out.println("Loading file: " + filename + "...");
			try {
				Scanner s = new Scanner(new File(filename));
				while (s.hasNext()) {
					list.add(s.nextLine());
				}
				s.close();

			} catch (Exception ex) {
				ex.printStackTrace();
			}

			filesContent.add(list);
		}

		/************/
		/* Tokenize */
		/************/

		System.out.println("Tokenizing...");

		Lexer lexer = new Lexer();

		// Pass each file individually
		for (int i = 0; i < filenames.size(); ++i) {
			String filename = filenames.get(i);
			ArrayList<String> fileContents = filesContent.get(i);
			lexer.parseFile(filename, fileContents);
		}

		System.out.println("Tokenized.");
		// Print the tokens for debugging

		if (flags.contains("-token")) {
			System.out.println("\nTokens: ");

			for (int i = 0; i < filenames.size(); ++i) {
				System.out.println("\n" + filenames.get(i) + ": ");
				ArrayList<String> fileContents = filesContent.get(i);
				ArrayList<LaiLexer.Token> tokens = lexer.getFileTokens(filenames.get(i));
				int lineNum = 0;
				int tokenIndex = 0;
				int futureindent = 0;
				int currentindent = 0;
				while (lineNum < fileContents.size()) {
					String line = "";
					while (tokens.get(tokenIndex).lineNumber == lineNum) {

						line += "[" + LaiLexer.getDebugString(tokens.get(tokenIndex)) + "]";

						if (LaiLexer.getConciseDebugString(tokens.get(tokenIndex)).equals("{")) {
							futureindent++;
						}
						if (LaiLexer.getConciseDebugString(tokens.get(tokenIndex)).equals("}")) {
							currentindent--;
							futureindent--;
						}
						tokenIndex++;
						if (tokenIndex >= tokens.size()) {
							break;
						}
					}
					System.out.print("\n" + (lineNum + 1) + ":" + getIndents(currentindent) + line);
					currentindent = futureindent;

					lineNum++;
				}
				System.out.print("\n\n");
			}
		}

		if (compilerErrors > 0) {
			cancelWithErrors(compilerErrors);
		}

		/*********************************/
		/* Assemble Abstract Syntax Tree */
		/*********************************/

		System.out.println("Assembling AST...");
		ASTAssembler ast = new ASTAssembler();
		for (int i = 0; i < filenames.size(); ++i) {
			ast.assembleFile(filenames.get(i), lexer.getFileTokens(filenames.get(i)));
		}
		System.out.println("AST assembled.");

		if (flags.contains("-ast")) {
			System.out.println("\nAST:\n");
			for (int i = 0; i < filenames.size(); ++i) {
				LaiFile file = ast.files.get(i);
				System.out.println(file.getDebugString(0));
			}

		}

		if (compilerErrors > 0) {
			cancelWithErrors(compilerErrors);
		}

		Backend code_generator = new BackendC(ast.files);
		System.out.println("\n\nCompiled Code:");

		String cCode = code_generator.compile();
		System.out.println(code_generator.compile());

		writeGeneratedCodeToFile(cCode);

		if (flags.contains("-visualstudio")) {
			compileCVisualStudio();
		}
		if (flags.contains("-gcc")) {
			compileCGCC();
		}
	}

	private static void writeGeneratedCodeToFile(String s) {
		PrintWriter out = null;
		try {
			out = new PrintWriter("output.c");
			out.write(s);
		} catch (FileNotFoundException e) {
			e.printStackTrace();

		} finally {
			if (out != null)
				out.close();
		}
	}

	private static void compileCGCC() {

		try {
			Process p = Runtime.getRuntime().exec("gcc output.c");

			BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while (true) {

				line = r.readLine();

				if (line == null) {
					break;
				}
				System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String compileVS = "findVS.bat";

	private static void compileCVisualStudio() {

		ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", compileVS);
		builder.redirectErrorStream(true);
		Process p;
		try {
			p = builder.start();

			// TODO Auto-generated catch block

			BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while (true) {

				line = r.readLine();

				if (line == null) {
					break;
				}
				System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void cancelWithErrors(int numOfErrors) {
		System.out.println("\n\nCompilation failed with " + numOfErrors + " errors.");
		System.exit(0);
	}

	public static String getIndents(int n) {
		String out = "";
		for (int i = 0; i < n; i++) {
			out += "\t";
		}
		return out;
	}
}