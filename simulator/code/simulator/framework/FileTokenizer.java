package simulator.framework;

import java.io.*;
import java.util.*;
import java.text.ParseException;

/**
 * This is the class responsible for processing files.  Objects that want to use
 * FileTokenizer should implement the Parser interface and pass themselves as a
 * constructor parameter.
 *
 * The FileTokenizer discards anything after the first ; on the line as a comment.
 * The FileTokenizer forwards all non-blank, non-comment lines through the
 * parse() callback method of Parser.
 *
 * The only exception is the #INCLUDE directive, which results in the creation
 * of a new FileTokenizer object to process the specified include file inline.
 * #INCLUDE directives are handled transparently and the parser is not notified,
 * except that the FileTokenizer object passed through the sourceFT argument in
 * the parse() callback will differ.
 *
 */

public class FileTokenizer
{

  boolean verbose;
  String filename;
  LineNumberReader theReader;
  Parser parser;
  String name;
  private final static String includeTag = "#INCLUDE";
  FileTokenizer parent = null;

  /**
   * This contructor is called internally when parsing #INCLUDE directives.
   * It is useful for the FileTokenizer to know about its "parent" tokenizer so
   * that we can avoid problems caused by a cycle in include files.
   * 
   * @param filename
   * @param verbose
   * @param parser
   * @param parent
   */
  private FileTokenizer(String filename, boolean verbose, Parser parser, FileTokenizer parent) {
      this(filename, verbose, parser);
      this.parent = parent;
  }

  /**
   * Create a tokenizer with the specified filename.  
   * @param filename
   * @param verbose  if true, print out parsing information.
   * @param parser  Lines read from filename will be passed back to this object.
   */
  public FileTokenizer(String filename, boolean verbose, Parser parser) {

      this.filename = filename;
      this.verbose = verbose;
      this.parser = parser;
      name = "FileTokenizer("+filename+")";
  }

  public void parseFile()
  {
      try {
          theReader = new LineNumberReader(new FileReader(filename));

          log("opened "+filename+", reading...");

          for( String line = theReader.readLine(); null!=line; line = theReader.readLine() ){

              // delete comments, which is anything after and including the first
              // ';'
              line = line.replaceFirst("\\s*;.*", "").trim();

              if( line.length()==0 ){
                  log(lineMessage("skipping blank line"));
                  continue;
              }

              // strip whitespace from both ends, then split into space-separated
              // words
              String[] words = line.split("\\s+");
              log(lineMessage(String.format(
                    "%d words: %s", words.length, Arrays.toString(words))));
              
              //see if the line is an include command
              if (words.length > 0 && words[0].equals(includeTag)) {
                  if (words.length != 2) {
                      throw new RuntimeException("Invalid syntax for #INCLUDE on line " + theReader.getLineNumber() + " of " + filename + ": expected exactly one argument, the include filename.");
                  }
                  if (!verifyFilename(words[1])) {
                      throw new RuntimeException("Error in the #INCLUDE syntax on line " + theReader.getLineNumber() + " of " + filename + ": a file named \"" + words[1] + "\" is already being parsed.  This error most likely results from a file that includes itself, or a set of files that form a cycle with #INCLUDE directives.");
                  }
                  try {
                      //create a new file tokenizer for the include file and parse it now
                      log("Including file \"" + words[1]);
                      FileTokenizer includeFT = new FileTokenizer(words[1],verbose,parser,this);
                      includeFT.parseFile();
                  } catch (Exception ex) {
                      throw new RuntimeException("The following exception occured while processing the #INCLUDE statement on line " + theReader.getLineNumber() + " of " + filename + ":  \n" + ex.getMessage());
                  }
                  //skip the rest of this line because we don't want to actually pass it to the parser.
                  continue;
              }



              try {
                  parser.parse(words, this);
              } catch(ParseException e) {
                  //System.err.println(lineMessage(e.toString()));
                  throw new RuntimeException(lineMessage(e.getMessage()));
              }
          }

          theReader.close();
      } catch (FileNotFoundException e) {
          throw new IllegalArgumentException(e);
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
  }
  
  private void log(String s)
  {
      if(verbose) Harness.log(name,s);
  }

  public String lineMessage(String s) {

      return String.format("%s:%d: %s",
              filename, theReader.getLineNumber(), s);
  }

  public int getLineNumber() {
      return theReader.getLineNumber();
  }

  public String getFilename() {
      return filename;
  }

  public String getLineInfo() {
      return getFilename() + ":" + getLineNumber();
  }

  /**
   * Check this file and parents (recursively) to see if any of them match the
   * current filename.
   * @param checkname
   * @return false if the file is already being parsed.
   */
  private boolean verifyFilename(String checkname) {
      //call up the chain of parents
      if (parent != null) {
          if (!parent.verifyFilename(checkname)) return false;
      }
      if (filename.equalsIgnoreCase(checkname)) {
          return false;
      }
      return true;
  }

}
