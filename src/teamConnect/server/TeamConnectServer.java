package teamConnect.server;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

import teamConnect.transmission.*;

/**
 * <p>
 * An instance of a TeamConnectServer that hosts 1 or more teams of users. Users
 * may join teams by contacting the server over the net. Once a user has joined
 * a team, they may share their status, and request the status of their team
 * members.
 * </p>
 * 
 * <p>
 * An instance can be created using the TeamConnectServer() constructor. Once an
 * instance is created, it can be run with .run(), and will run until the
 * application is closed.
 * </p>
 * 
 * <p>
 * An instance can support up to {@value #MAX_TEAM_COUNT} teams,
 * {@value #MAX_STATUS_COUNT} unique statuses, and
 * {@value teamConnect.transmission.MessageHelper#MAX_SEGMENT_SIZE} unique
 * users. All team names, user names, and statuses may be between 1 and
 * {@value MessageHelper#MAX_IDENTIFIER_SIZE} characters, and consist of
 * letters, numbers, and spaces.
 * </p>
 */
public class TeamConnectServer
{
 /* CLASS CONSTANTS */

 /**
  * All team ID's should be representable in a byte, and 0xff represents no team.
  */
 public static final int MAX_TEAM_COUNT = 20;
 public static final int MAX_TEAM_SIZE = 50;
 /**
  * All statuses should be representable in a byte, and 0xff represents no
  * status.
  */
 public static final int MAX_STATUS_COUNT = 20;

 /**
  * The userbase will start with space for {@value} users, and double in size
  * when needed.
  */
 public static final int INITIAL_USERBASE_SIZE = 16;

 /* INSTANCE VARIABLES */

 private final DatagramSocket socket;
 private final Team[] teams;
 private final ServerMessage serverDescription;
 private int nextNetID;
 private User[] userbase;

 /* CONSTRUCTORS */

 /**
  * Creates an instance of Server with the specified team names and supported
  * statuses that will run on the specified port.
  * 
  * @param port              The port number to host this server on
  * @param teamNames         The names of all teams that will be hosted on this
  *                          server
  * @param supportedStatuses Statuses this clients using this server will be able
  *                          to share to their teams
  * @throws SocketException          If a socket could not be opened on the
  *                                  specified port
  * @throws IllegalArgumentException If teamNames is not an array with 1 <= size
  *                                  <= {@value #MAX_TEAM_SIZE}, or if
  *                                  supportedStatuses is not an array with 1 <=
  *                                  size <= {@value #MAX_STATUS_COUNT}, or if
  *                                  either contains any
  */
 public TeamConnectServer(int port, String[] teamNames, String[] supportedStatuses) throws SocketException
 {
  /* Guard case against null arrays */
  if(teamNames == null || supportedStatuses == null)
   throw new IllegalArgumentException("Null array passed into Server");

  /* Guard case against invalid number of teams */
  if(teamNames.length < 1 || teamNames.length > MAX_TEAM_COUNT)
   throw new IllegalArgumentException(
     "Server supports between 1 and " + MAX_TEAM_COUNT + " teams, " + teamNames.length + " team names were given");

  /* Guard case against invalid number of statuses */
  if(supportedStatuses.length < 1 || supportedStatuses.length > MAX_STATUS_COUNT)
   throw new IllegalArgumentException("Server supports between 1 and " + MAX_STATUS_COUNT + " statuses, "
     + supportedStatuses.length + " team names were given");

  /* Guard case against any invalid team names */
  for(String teamName : teamNames)
   if(!MessageHelper.isValidIdentifier(teamName))
    throw new IllegalArgumentException("Team names should consist of 1 to " + MessageHelper.MAX_IDENTIFIER_SIZE
      + " letters, numbers, and spaces. The name \"" + teamName + "\" is invalid.");

  /* Guard case against any invalid statuses */
  for(String status : supportedStatuses)
   if(!MessageHelper.isValidIdentifier(status))
    throw new IllegalArgumentException("Statuses should consist of 1 to " + MessageHelper.MAX_IDENTIFIER_SIZE
      + " letters, numbers, and spaces. The name \"" + status + "\" is invalid.");

  /* Open a socket on the desired port */
  this.socket = new DatagramSocket(port);

  /* Generate all teams, assigning each a unique sequential ID */
  teams = new Team[teamNames.length];
  for(byte teamID = 0; teamID < teamNames.length; teamID++)
   teams[teamID] = new Team();

  /*
   * Create an empty list of users and initialize the first user ID to 0. This
   * will be incremented by 1 for each new user.
   */
  userbase = new User[INITIAL_USERBASE_SIZE];
  nextNetID = 0;

  /*
   * Because the server description is static (The list of team names and statuses
   * will not change during the server's lifetime), go ahead and generate the
   * messages that will be sent to clients when they request the server
   * description.
   */
  serverDescription = MessageHelper.generateServerDescription(teamNames, supportedStatuses);

  System.out.printf("Successfully set up TeamConnect server on port %d.\n", port);
 }

 /* RUNTIME HELPERS */

 /**
  * Run the server. It will begin to listen for messages from clients, and handle
  * appropriately by updating its team database and responding to clients as
  * needed.
  */
 public void run()
 {
  System.out.println("Running...");

  /*
   * The server enters an infinite loop wherein it receives a packet, handles the
   * packet, and sends a message based on the result of that transmission.
   */
  while (true)
  {
   DatagramPacket packet = new DatagramPacket(new byte[MessageHelper.MAX_SEGMENT_SIZE], MessageHelper.MAX_SEGMENT_SIZE);
   try
   {
    System.out.println("Listening for packet...");
    socket.receive(packet);
    System.out.printf("Received packet from %s:%d.\n", packet.getAddress(), packet.getPort());

    /*
     * The handling of any client package will return a message to send back to the
     * client, either in the form of an error, an ACK, or of information requested
     * by the client
     */
    ServerMessage msg = handleClientPacket(packet);
    transmit(msg, packet.getAddress(), packet.getPort());

   } catch (IOException e)
   {
    e.printStackTrace();
   }
  }
 }

 /**
  * Parse a packet to a ClientMessage object and perform the appropriate action
  * according to its MessageType.
  * 
  * @param packet
  * @return The message to be sent back to the client
  */
 private ServerMessage handleClientPacket(DatagramPacket packet)
 {
  try
  {
   ClientMessage cm = new ClientMessage(packet);

   System.out.println("Extracted client message of type " + ClientMessage.typeString(cm.getMessageType()));

   /*
    * Message type in the Client Message header informs how it should be handled by
    * the server
    * 
    */
   switch (cm.getMessageType())
   {
   case ClientMessage.NET_ID_REQUEST:
    return serviceNetIDRequest(cm);
   case ClientMessage.STATUS_UPDATE:
    return serviceStatusUpdate(cm);
   case ClientMessage.TEAM_STATUS_REQUEST:
    return serviceTeamStatusRequest(cm);
   case ClientMessage.SERVER_DESCRIPTION_REQUEST:
    return serviceServerDescriptionRequest(cm);
   default:
    return new ServerMessage(ServerMessage.ERROR, "Could not process request - Invalid client message type");
   }
  } catch (IllegalArgumentException e)
  {
   /*
    * Fallback for a message that is not compliant to the transmission protocol.
    */
   return new ServerMessage(ServerMessage.ERROR, "Failed to parse malformed message");
  }

 }

 /* REQUEST SERVICERS */

 /**
  * Handle and produce a response to a request for the status vector of a team
  * 
  * @param clientMessage
  * @return A message containing the requested team status vector, or an error
  *         message if the team does not exist
  */
 private ServerMessage serviceTeamStatusRequest(ClientMessage clientMessage)
 {

  /* Guard case for invalid team ID */
  int teamID = clientMessage.getTeamID();
  if(teamID < 0 || teamID >= teams.length)
   return new ServerMessage(ServerMessage.ERROR, "Invalid Team ID: " + teamID);

  Team t = teams[teamID];

  return MessageHelper.generateTeamStatusMessage(t.getUserNames(), t.getStatusVector());
 }

 /**
  * Handle and produce a response to a request for the details of this server
  * 
  * @param clientMessage
  * @return A message with a description of this sever's teams and supported
  *         statuses
  */
 private ServerMessage serviceServerDescriptionRequest(ClientMessage clientMessage)
 {
  return serverDescription;
 }

 /**
  * Attempts to register a new user with a unique network ID. The body of the
  * client message should contain the client's desired user-name. Creates a
  * record of the new user and their associated information, then returns a
  * message to inform them of their user ID.
  * 
  * @param clientMessage
  * @return A message containing the client's unique user ID, or an error message
  *         if something went wrong while creating a new user.
  */
 private ServerMessage serviceNetIDRequest(ClientMessage clientMessage)
 {
  return createNewUser(clientMessage);
 }

 /**
  * Recieve a message detailing a client's change in status, which may also
  * include a change in team. Appropriately modifies the server database to
  * reflect the change, and returns a message acknowledging completion of the
  * task.
  * 
  * @param clientMessage
  * @return An acknowledgement of the status update, or an error message
  *         something went wrong.
  */
 private ServerMessage serviceStatusUpdate(ClientMessage clientMessage)
 {

  /* Guard case against invalid network ID */
  int networkID = clientMessage.getNetworkID();
  User user = getUserByNetID(networkID);
  if(user == null)
   return new ServerMessage(ServerMessage.ERROR, "Cannot resolve invalid user ID: " + networkID);

  /* Guard case against invalid status */
  int messageStatus = clientMessage.getStatus();
  if((messageStatus < 0 || messageStatus >= MAX_STATUS_COUNT) && messageStatus != MessageHelper.INACTIVE_STATUS)
   return new ServerMessage(ServerMessage.ERROR, "Invalid status: " + messageStatus);

  /* Guard case against invalid team ID */
  int newTeamID = clientMessage.getTeamID();
  if((newTeamID < 0 || newTeamID >= teams.length) && newTeamID != MessageHelper.TEAM_WILDCARD)
   return new ServerMessage(ServerMessage.ERROR, "Invalid team ID: " + newTeamID);

  /* Update user status */
  user.setStatus(messageStatus);

  /* Special case for removing user from an old team */
  int oldTeamID = user.getTeamID();
  if(newTeamID != oldTeamID && oldTeamID != MessageHelper.TEAM_WILDCARD)
   teams[oldTeamID].removeUser(user);

  /* Special case for adding user to a new team */
  if(newTeamID != oldTeamID && newTeamID != MessageHelper.TEAM_WILDCARD)
   teams[newTeamID].addUser(user);

  user.setTeamID(newTeamID);

  return new ServerMessage(ServerMessage.ACK_STATUS_UPDATE, MessageHelper.EMPTY_BODY);
 }

 /* CLASS HELPERS */

 /**
  * Returns the user with a given network ID, or null if no such user exists.
  * 
  * @param networkID
  * @return user
  */
 private User getUserByNetID(int networkID)
 {
  /*
   * Since net ID's are assigned sequentially, it is sufficient to check whether
   * the requested ID is outside the range of ID's created so far.
   */
  if(networkID < 0 || networkID >= this.nextNetID)
   return null;

  /* A user's networkID doubles as their index in the userbase */
  return userbase[networkID];
 }

 /**
  * Transmits a message to a client specified by ip and port. This transmission
  * is unreliable, the server does not listen for an acknowledgement that the
  * client succesfully received the message.
  * 
  * @param msg
  * @param ip
  * @param port
  */
 private void transmit(ServerMessage msg, InetAddress ip, int port)
 {
  DatagramPacket dgp = new DatagramPacket(msg.getFullMessage(), msg.getFullMessage().length, ip, port);

  /*
   * For unreliable transmission, simply attempt to send the packet, and if an
   * exception arises, let it be.
   */
  try
  {
   socket.send(dgp);

   System.out.printf("Sent %db packet of type %s to %s:%d.\n", dgp.getLength(),
     ServerMessage.typeString(msg.getMessageType()), ip, port);
  } catch (IOException e)
  {
   e.printStackTrace();
  }
 }

 /**
  * Creates a new user from a client message. Assigns this user a unique network
  * ID. If there is no space left for new users or the given user-name is
  * invalid, returns an error transmission. Otherwise, returns a transmission
  * acknowledging successful creation.
  * 
  * @param clientMessage
  * @return ServerTransmission detailing results
  */
 private ServerMessage createNewUser(ClientMessage clientMessage)
 {
  /*
   * Guard case against exceeding the max network ID. A client cannot be assigned
   * a network ID which it will be unable to communicate in messages.
   */
  if(nextNetID >= MessageHelper.NETWORK_ID_LIMIT)
   return new ServerMessage(ServerMessage.ERROR,
     "Could not create new user, this server has reached its limit of users supported");

  String userName = clientMessage.getUserName();

  /* Guard case against invalid user-name */
  if(!MessageHelper.isValidIdentifier(userName))
   return new ServerMessage(ServerMessage.ERROR, "Usernames must be between 1 and " + MessageHelper.MAX_IDENTIFIER_SIZE
     + " letters, numbers and spaces. \"" + userName + "\" is not a valid username.");

  /* Expand userbase if it is out of space for new users */
  if(nextNetID >= userbase.length)
   userbase = Arrays.copyOf(userbase, userbase.length * 2);

  /* Create and store a new user */
  User user = new User(nextNetID, MessageHelper.TEAM_WILDCARD, MessageHelper.INACTIVE_STATUS, userName);
  userbase[nextNetID] = user;

  /* net ID's are assigned sequentially */
  nextNetID++;

  /* Inform the client their user profile has been created */
  return new ServerMessage(ServerMessage.ACK_NEW_USER, Integer.toString(user.getNetID()).getBytes());

 }

 /* HELPER CLASSES */

 /**
  * A User for use by TeamConnectServer. Stores a networkID, displayName, and
  * modifiable status and team ID.
  * 
  *
  */
 private class User
 {
  /* INSTANCE VARIABLES */

  private final int netID;
  private final String displayName;
  private int status;
  private int teamID;

  /* CONSTRUCTORS */

  public User(int netID, int teamNumber, int status, String displayName)
  {
   this.netID = netID;
   this.displayName = displayName;
   this.teamID = teamNumber;
   this.status = status;
  }

  /* SETTERS */

  public void setTeamID(int team)
  {
   this.teamID = team;
  }

  public void setStatus(int status)
  {
   this.status = status;
  }

  /* GETTERS */

  public int getNetID()
  {
   return this.netID;
  }

  public String getName()
  {
   return this.displayName;
  }

  public int getTeamID()
  {
   return this.teamID;
  }

  public int getStatus()
  {
   return this.status;
  }

 }

 /**
  * A Team for use by TeamServer. A team keeps track of its users and each of
  * their statuses. Upon request, it can provide a description with the names of
  * all its current active users, and a status vector of all its users statuses.
  * 
  *
  */
 private class Team
 {
  /* INSTANCE VARIABLES */

  private User[] users;

  /* CONSTRUCTORS */

  public Team()
  {
   /* A team starts with no users */
   users = new User[TeamConnectServer.MAX_TEAM_SIZE];
  }

  /* SETTERS */

  /**
   * Attempts to add a new user to this team.
   * 
   * @param user
   * @return The index the user was added at, or -1 if the addition failed.
   */
  public int addUser(User user)
  {
   /*
    * Initialize freeIndex to a negative value, if it is still negative after
    * searching for a positive free index, then no free index was found
    */
   int freeIndex = -1;

   for(int i = 0; i < users.length; i++)
   {
    User user2 = users[i];

    /* Detect duplicate user ID, do not add a user if detected */
    if(user2 != null && user2.getNetID() == user.getNetID())
     return freeIndex;

    /* Detect an empty spot to add a new user */
    if(freeIndex < 0 && user2 == null)
     freeIndex = i;
   }

   /* If free index never increased, there are no free spots for a new user */
   if(freeIndex < 0)
    return freeIndex;

   /*
    * If all checks passed, a user can successfully be added to the team
    */
   users[freeIndex] = user;
   return freeIndex;
  }

  /**
   * Attempt to remove a user from this team, with network ID matching the ID of
   * the given user.
   * 
   * @param user
   * @return Whether the user was successfully removed (whether they existed on
   *         the team in the first place).
   */
  public boolean removeUser(User user)
  {
   for(int i = 0; i < users.length; i++)
    if(users[i] != null && users[i].getNetID() == user.getNetID())
    {
     /*
      * If an index is found with a matching net ID, clear it, update description,
      * and return successful
      */
     users[i] = null;
     return true;
    }

   return false;
  }

  /* GETTERS */

  /**
   * Returns a list of all the team members' display names, or an empty String for
   * any slot with an inactive user.
   * 
   * @return
   */
  public String[] getUserNames()
  {
   String[] userNames = new String[users.length];

   /* Null users map to an empty name */
   for(int i = 0; i < users.length; i++)
    userNames[i] = users[i] == null ? "" : users[i].getName();

   return userNames;
  }

  /**
   * Returns a vector of the statuses of all of this team's users, or INACTIVE for
   * any slot with an inactive user.
   * 
   * @return byte[] statusVector
   */
  public byte[] getStatusVector()
  {
   byte[] statusVector = new byte[users.length];

   /* Null users map to an inactive status */
   for(int i = 0; i < users.length; i++)
    statusVector[i] = (byte) (users[i] == null ? MessageHelper.INACTIVE_STATUS : users[i].getStatus());

   return statusVector;
  }

 }
}
