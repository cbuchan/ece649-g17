package simulator.framework;

import java.text.ParseException;

public interface Parser
{
  /**
   * Callback method used by FileTokenizer to notify the parser that a new line 
   * has been found.  
   * 
   * sourceFT is primarily passed so that the lineMessage() method can be used
   * to create accurate messages.  Because FileTokenizer processes #INCLUDE directives, the
   * sourceFT may differ from the original FileTokenizer used to initiate parsing.
   * 
   * @param args  The current line from the file being parsed with comments removed
   * and split into tokens by whitespace.
   * @param sourceFT  The FileTokenizer that created the line.
   * @throws ParseException  Thrown if the syntax of the line is not valid in the context
   * of the parser.
   */
    public void parse(String[] args, FileTokenizer sourceFT) throws ParseException;
}
