package ca.ubc.cs317.dict.net;

import ca.ubc.cs317.dict.exception.DictConnectionException;
import ca.ubc.cs317.dict.model.Database;
import ca.ubc.cs317.dict.model.Definition;
import ca.ubc.cs317.dict.model.MatchingStrategy;

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
            socket = new Socket(host, port);
            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
    public synchronized void close() {
        System.out.println("Terminating connection to dict server");
        this.output.println("QUIT");
        try{
            this.socket.close();
            this.input.close();
            this.output.close();
        } catch(Exception e){
            //TODO what should I do with this exception?
            System.out.println("There was a problem disconnecting O.o");
        }
        System.out.println("Connection terminated");
        // TODO Add your code here
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

        // TODO Add your code here

        return set;
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

        // TODO Add your code here

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
        	String in;
        	in = this.input.readLine();
        	if(!in.startsWith("110")) {
        		throw new Exception("There was a problem with the server");
        	}
        	while((in = this.input.readLine()) != null) {
        		if(in.equals(".")){
        			return databaseMap.values();
        		}
        		String dbName = in.substring(0, in.indexOf(" "));
        		String dbDescription = in.substring(in.indexOf(" ") + 1, in.length());
        		databaseMap.put(dbName, new Database(dbName, dbDescription));
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
    	System.out.println("in getStrategyList()");
        Set<MatchingStrategy> set = new LinkedHashSet<>();
        this.output.println("SHOW STRAT");
        try{
        	String in;
        	//check 250 success response
        	in = this.input.readLine();
        	if(!in.startsWith("250")) {
        		throw new Exception("There was a problem with the server");
        	}
        	//check 111 response indicating # of responses found
        	in = this.input.readLine();
        	if(!in.startsWith("111")) {
        		throw new Exception("there are currently no matching strategies available");
        	}
        	while((in = this.input.readLine()) != null) {
        		if(in.equals(".")){
        			return set;
        		}
        		String strategyName = in.substring(0, in.indexOf(" "));
        		String strategyDescription = in.substring(in.indexOf(" ") + 1, in.length());
        		set.add(new MatchingStrategy(strategyName, strategyDescription));
        	}
        } catch (Exception e){
        	throw new DictConnectionException("Encountered an error in obtaining the list of strategies: " + e.getMessage());
        }
        return set;
    }

}
