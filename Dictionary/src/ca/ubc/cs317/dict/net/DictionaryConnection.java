package ca.ubc.cs317.dict.net;

import ca.ubc.cs317.dict.exception.DictConnectionException;
import ca.ubc.cs317.dict.model.Database;
import ca.ubc.cs317.dict.model.Definition;
import ca.ubc.cs317.dict.model.MatchingStrategy;
import ca.ubc.cs317.dict.util.DictStringParser;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Created by Jonatan on 2017-09-09.
 */
public class DictionaryConnection {

    private static final int DEFAULT_PORT = 2628;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private DictStringParser dictStringParser;

    private Map<String, Database> databaseMap = new LinkedHashMap<String, Database>();

    /** Establishes a new connection with a DICT server using an explicit host and port number, and handles initial
     * welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @param port Port number used by the DICT server
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host, int port) throws DictConnectionException {
        try{
            this.dictStringParser = new DictStringParser();
            this.socket = new Socket(host, port);
            this.output = new PrintWriter(socket.getOutputStream(), true);
            this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String in = input.readLine();
            if(!in.startsWith("220")){
                throw new DictConnectionException("Could not Connect.  Please try again.");        
            } else{
                System.out.println("Sucessfully connected to " + host + " on port: " + port);
            }
        } catch(Exception e) {
            throw new DictConnectionException("Something went wrong with the connetion: " + e.getMessage(), e);
        }
    }

    /** Establishes a new connection with a DICT server using an explicit host, with the default DICT port number, and
     * handles initial welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host) throws DictConnectionException {
        this(host, DEFAULT_PORT);
    }

    /** Sends the final QUIT message and closes the connection with the server. This function ignores any exception that
     * may happen while sending the message, receiving its reply, or closing the connection.
     *
     */
    public synchronized void close(){
        System.out.println("Terminating connection to dict server.");
        this.output.println("QUIT");
        try {
            this.input.close();
            this.output.close();
            this.socket.close();
            System.out.println("Connection terminated");
            System.exit(0);
        } catch(Exception e) {
            System.out.println("Error while terminating presentation.");
        }
    }

    /** Requests and retrieves all definitions for a specific word.
     *
     * @param word The word whose definition is to be retrieved.
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 definitions in the first database that has a definition for the word should be used
     *                 (database '!').
     * @return A collection of Definition objects containing all definitions returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Collection<Definition> getDefinitions(String word, Database database) throws DictConnectionException {
        Collection<Definition> set = new ArrayList<>();
        getDatabaseList(); // Ensure the list of databases has been populated
        this.output.println("DEFINE " + database.getName() + " " + "\""+ word + "\"");
        try{
            String in = this.input.readLine();
            String statusCode;
            String[] inputSplitIntoDictAtoms;
            readInput:
            while(in != null) {
                inputSplitIntoDictAtoms = dictStringParser.splitAtoms(in);
                // splitAtoms returns an empty array if input line is "." or null
                statusCode = inputSplitIntoDictAtoms.length > 0 ? inputSplitIntoDictAtoms[0] : in;
                switch(statusCode){
                    case "550": // Invalid database
                        throw new Exception("Invalid Database provided with name: " + database.getName());
                    case "552": // No matches found
                        System.out.println("No matches found");
                    case "250":
                        break readInput; //Breaks out of the while loop
                    case "150": // Got definitions
                        int numberOfDefinitions = Integer.parseInt(inputSplitIntoDictAtoms[1]);
                        set = parseDefinitions(numberOfDefinitions);
                        break;
                    case ".":
                        break;
                    case "501":
                        throw new Exception("Invalid syntax. Illegal parameters");
                    default:
                        throw new Exception("Encountered an unexpected error");
                }
                in = this.input.readLine();
            }
        } catch (Exception e){
            throw new DictConnectionException("Encountered an error while finding definitions: " + e.getMessage());
        }
        return set;
    }


    private ArrayList<Definition> parseDefinitions(int numberOfDefinitions) throws DictConnectionException {
                        // This is followed by a long statement like:

                        // 150 3 definitions retrieved
                        //151 "Obligatory" gcide "The Collaborative International Dictionary of English v.0.48"
                        //Obligatory \Ob"li*ga*to*ry\, a. [L. obligatorius: cf. F.
                        //   obligatoire.]
                        //   Binding in law or conscience; imposing duty or obligation;
                        //   requiring performance or forbearance of some act; -- often
                        //   followed by on or upon; as, obedience is obligatory on a
                        //   soldier.
                        //   [1913 Webster]
                        //
                        //         As long as the law is obligatory, so long our obedience
                        //         is due.                                  --Jer. Taylor.
                        //   [1913 Webster]
                        //.
                        //151 "obligatory" wn "WordNet (r) 3.0 (2006)"
                        //obligatory
                        //    adj 1: morally or legally constraining or binding; "attendance
                        //           is obligatory"; "an obligatory contribution" [ant:
                        //           {optional}]
                        //    2: required by obligation or compulsion or convention; "he made
                        //       all the obligatory apologies"
                        //.
                        //151 "obligatory" moby-thesaurus "Moby Thesaurus II by Grady Ward, 1.0"
                        //38 Moby Thesaurus words for "obligatory":
                        //   absolute, binding, choiceless, compulsory, conclusive, de rigueur,
                        //   decisive, decretory, demanded, dictated, entailed, essential,
                        //   exigent, final, hard-and-fast, imperative, imperious, importunate,
                        //   imposed, incumbent, indispensable, inevitable, involuntary,
                        //   irrevocable, mandated, mandatory, must, necessary, necessitous,
                        //   peremptory, prescript, prescriptive, required, requisite, ultimate,
                        //   urgent, without appeal, without choice
                        //
                        //
                        //.
                        //250 ok [d/m/c = 3/0/123; 0.000r 0.000u 0.000s]

        try{
            ArrayList<Definition> set = new ArrayList<Definition>();
            String in;
            String[] inputSplitIntoDictAtoms;
            for(int i = 0; i < numberOfDefinitions; i++){
                in = this.input.readLine(); // 151 "obligatory" moby-thesaurus "Moby Thesaurus II by Grady Ward, 1.0"
                inputSplitIntoDictAtoms = dictStringParser.splitAtoms(in);
                String definitionDatabaseName = inputSplitIntoDictAtoms[2];
                String definitionWord = inputSplitIntoDictAtoms[1];
                Database definitionDatabase = databaseMap.get(definitionDatabaseName);
                Definition definition = new Definition(definitionWord, definitionDatabase);
                while(true){
                    in = this.input.readLine();
                    if(in.equals(".")){ //Definiton ends with "."
                         break;
                    }
                    definition.appendDefinition(in);
                }
                set.add(definition);
            }
            return set;
        } catch (Exception e) {
            throw new DictConnectionException("There was an error while parsing the definitions");
        }
    }

    /** Requests and retrieves a list of matches for a specific word pattern.
     *
     * @param word     The word whose definition is to be retrieved.
     * @param strategy The strategy to be used to retrieve the list of matches (e.g., prefix, exact).
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 matches in the first database that has a match for the word should be used (database '!').
     * @return A set of word matches returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<String> getMatchList(String word, MatchingStrategy strategy, Database database) throws DictConnectionException {
        Set<String> set = new LinkedHashSet<>();
        this.output.println("MATCH " + database.getName() + " " + strategy.getName() + " " + "\"" + word + "\"");
        try{
            String in = this.input.readLine();
            String matchingWord, statusCode;
            String[] inputSplitIntoDictAtoms;
            readInput:
                while(in != null) {
                    inputSplitIntoDictAtoms = dictStringParser.splitAtoms(in);
                    // splitAtoms returns an empty array if input line is "." or null
                    statusCode = inputSplitIntoDictAtoms.length > 0 ? inputSplitIntoDictAtoms[0] : in;
                    switch(statusCode){
                        case "550": // Invalid database
                            throw new Exception("Invalid database used with name: " + database.getName());
                        case "551": // Invalid strategy
                            throw new Exception("Invalid Strategy used with name: " + strategy.getName());
                        case "552": // No matches found
                        case "250":
                            break readInput; //Breaks out of the while loop
                        case "152": // 152 4 matches found
                            int numberOfWords = Integer.parseInt(inputSplitIntoDictAtoms[1]);
                            for(int i = 0; i < numberOfWords; i++){
                                in = this.input.readLine();
                                inputSplitIntoDictAtoms = dictStringParser.splitAtoms(in);
                                matchingWord = inputSplitIntoDictAtoms[1];
                                set.add(matchingWord);
                            }
                            break;
                        case ".":
                            break;
                        case "501":
                            throw new Exception("Invalid syntax. Illegal parameters");
                        default:
                            throw new Exception("Encountered an unexpected error");
                    }
                    in = this.input.readLine();
                }
        } catch (Exception e){
            throw new DictConnectionException(e.getMessage());
        }
        return set;
    }

    /** Requests and retrieves a list of all valid databases used in the server. In addition to returning the list, this
     * method also updates the local databaseMap field, which contains a mapping from database name to Database object,
     * to be used by other methods (e.g., getDefinitionMap) to return a Database object based on the name.
     *
     * @return A collection of Database objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Collection<Database> getDatabaseList() throws DictConnectionException {
        if (!databaseMap.isEmpty()) return databaseMap.values();
        this.output.println("SHOW DB");
        try{
        	String in, statusCode, dbName, dbDescription;
            String[] splitAtoms;
            in = this.input.readLine();
        	readInput: while(in != null ) { // This while loop has label readInput
                splitAtoms = dictStringParser.splitAtoms(in);
                // splitAtoms returns an empty array if input line is "." or null
                statusCode = splitAtoms.length > 0 ? splitAtoms[0] : in;
                switch(statusCode){
                    case("110"): // databases found no error. Staement: 110 72 databases present
                        int numberOfDatabases = Integer.parseInt(splitAtoms[1]);
                        for(int i = 0; i < numberOfDatabases; i++){
                            in = this.input.readLine();
                            splitAtoms = dictStringParser.splitAtoms(in);
                            dbName = splitAtoms[0];
                            dbDescription = splitAtoms[1];
                            databaseMap.put(dbName, new Database(dbName, dbDescription));
                        }
                        break;
                    case("."):
                        break;
                    case("250"):
                        break readInput; // This leaves the whole for loop otherwise the this.input.readLine() waits forever
                    case("554"):
                        break readInput;
                }
                in = this.input.readLine();
            }
        } catch (Exception e){
        	throw new DictConnectionException("Encountered an error in obtaining the list of databases: " + e.getMessage());
        }
        return databaseMap.values();
    }

    /** Requests and retrieves a list of all valid matching strategies supported by the server.
     *
     * @return A set of MatchingStrategy objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<MatchingStrategy> getStrategyList() throws DictConnectionException {
        Set<MatchingStrategy> strategySet = new LinkedHashSet<>();
        this.output.println("SHOW STRAT");
        try{
            String in, statusCode, strategyName, strategyDescription;
            String[] splitAtoms;
            in = this.input.readLine();
            readInput: while(in != null ) { // This while loop has label readInput
                splitAtoms = dictStringParser.splitAtoms(in);
                // splitAtoms returns an empty array if input line is "." or null
                statusCode = splitAtoms.length > 0 ? splitAtoms[0] : in;
                switch(statusCode){
                    case("111"): // databases found no error. Staement: 111 12 strategies present
                        int numberOfStrategies = Integer.parseInt(splitAtoms[1]);
                        for(int i = 0; i < numberOfStrategies; i++){
                            in = this.input.readLine();
                            splitAtoms = dictStringParser.splitAtoms(in);
                            strategyName = splitAtoms[0];
                            strategyDescription = splitAtoms[1];
                            strategySet.add(new MatchingStrategy(strategyName, strategyDescription));
                        }
                        break;
                    case("."):
                        break;
                    case("250"):
                        break readInput; // This leaves the while loop. Otherwise the this.input.readLine() outside the switch waits forever for more input
                    case("555"): //No strategies available
                        break readInput;
                }
                in = this.input.readLine();
            }
        } catch (Exception e){
        	throw new DictConnectionException("Encountered an error in obtaining the list of strategies: " + e.getMessage());
        }
        return strategySet;
    }

}
