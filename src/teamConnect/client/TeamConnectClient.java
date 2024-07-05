package teamConnect.client;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

import teamConnect.transmission.*;

/**
 * <p>
 * An instance of TeamConnectClient allows a user to connect to a
 * TeamConnectServer and interact with it using terminal commands.
 * </p>
 * 
 * <p>
 * In order to communicate over the internet, a TeamConnectClient will open a
 * socket on a free port on the machine. Once the client has been created, it
 * can be run using the .run() method
 * </p>
 */
public class TeamConnectClient
{
 /* CLASS CONSTANTS */

 public final static int MESSAGE_TIMEOUT = 1_000; // ms
 /**
  * Delay between screen refreshes for aesthetic purposes
  */
 private final static int SCREEN_DELAY = 300; // ms

 /* INSTANCE VARIABLES */

 private final InetAddress serverIP;
 private final int serverPort;
 private DatagramSocket socket;
 private String displayName;
 private int networkID;
 private String[] teams;
 private String[] statuses;
 private int team;
 private int status;
 private Scanner scanner;

 /* CONSTRUCTORS */

 /**
  * Constructs a new TeamConnectClient to interact with the server specified by
  * IP and port, with the given display name. Binds the client to an open port on
  * the machine. To run the client, call the .run() method.
  * 
  * @param serverIP
  * @param serverPort
  * @param displayName
  * @throws SocketException
  */
 public TeamConnectClient(InetAddress serverIP, int serverPort, String displayName) throws SocketException
 {
  this(serverIP, serverPort, displayName, MessageHelper.NETWORK_ID_LIMIT);
 }

 /**
  * <p>
  * Constructs a new TeamConnectClient to interact with the server specified by
  * IP and port, with the given display name. Binds the client to an open port on
  * the machine. To run the client, call the .run() method.
  * </p>
  * 
  * <p>
  * Constructing with an explicitly specified networkID will tie this
  * TeamConnectClient to that networkID on the server rather than assigning it a
  * new one. This may be used to recover networkID after becoming disconnected
  * from the server. However, this may lead to errors if the specified networkID
  * does not exist on the server.
  * </p>
  * 
  * @param serverIP
  * @param serverPort
  * @param displayName
  * @param networkID
  * @throws SocketException
  */
 public TeamConnectClient(InetAddress serverIP, int serverPort, String displayName, int networkID)
   throws SocketException
 {
  /* Validate display name */
  if(!MessageHelper.isValidIdentifier(displayName))
   throw new IllegalArgumentException("User names should consist of 1 to " + MessageHelper.MAX_IDENTIFIER_SIZE
     + " letters, numbers, and spaces. The name \"" + displayName + "\" is invalid.");

  this.displayName = displayName;

  this.serverIP = serverIP;
  this.serverPort = serverPort;

  /* Open a socket on a free port on the machine */
  this.socket = new DatagramSocket();
  socket.setSoTimeout(MESSAGE_TIMEOUT);

  this.networkID = networkID;

  /* Set team and status to default values */
  this.team = MessageHelper.TEAM_WILDCARD;
  this.status = MessageHelper.INACTIVE_STATUS;
 }

 /**
  * Start up this instance of TeamConnectClient
  */
 public void run()
 {
  System.out.println("Attempting to connect to server at " + serverIP + ":" + serverPort);

  setup();

  System.out.println("Now running TeamConnect client with netID " + networkID);

  mainLoop();
 }

 /**
  * Sets up information that is nessecary for interfacing with a server.
  * Specifically, ensures this client has a networkID and requests all of the
  * teams and status options managed by the server.
  */
 private void setup()
 {
  /* If networkID is unset, request a unique networkID */
  if(networkID == MessageHelper.NETWORK_ID_LIMIT)
   this.networkID = transmitUntilResponse(ClientMessage.NET_ID_REQUEST, displayName.getBytes(),
     ServerMessage.ACK_NEW_USER).getNetID();

  /* Populate data about the teams and status options specified by the server */

  ServerMessage serverDescription = transmitUntilResponse(ClientMessage.SERVER_DESCRIPTION_REQUEST,
    MessageHelper.EMPTY_BODY, ServerMessage.SERVER_DESCRIPTION);

  teams = serverDescription.getTeamNames();
  statuses = serverDescription.getStatuses();

  /* Prepare scanner for use in main loop */

  scanner = new Scanner(System.in);

 }

 /**
  * Repeatedely take input from the command line and interface with the server
  * accordingly.
  */
 private void mainLoop()
 {
  while (true)
  {
   displayMenu();

   switch (scanner.nextInt())
   {
   case 1: // Update status
    updateStatus();
    break;
   case 2:
    changeTeam();
    break;
   case 3:
    displayTeamStatusUpdate();
    break;
   case 4:
    quit();
    break;
   default:
    System.out.println("Invalid input.");
   }

   /*
    * Adding a small delay before the next screen seems to work well aesthetically
    */
   try
   {
    Thread.sleep(SCREEN_DELAY);
   } catch (InterruptedException e)
   {
   }
  }
 }

 /* COMMAND-LINE INTERFACE HELPERS */

 /**
  * Helper method to display main Command-Line interface menu
  */
 private void displayMenu()
 {
  System.out.println();
  System.out.println("===========================================================");
  System.out.printf("SERVER: %-20s |\n", serverIP + ":" + serverPort);
  System.out.printf("USER:   %-20s | NET_ID: %-20d\n", displayName, networkID);
  System.out.printf("TEAM:   %-20s | STATUS: %-20s\n\n", team == MessageHelper.TEAM_WILDCARD ? "[none]" : teams[team],
    status == MessageHelper.INACTIVE_STATUS ? "[none]" : statuses[status]);

  System.out.println();
  System.out.println("Type a number to choose an option:");
  System.out.println("  [1] Update my status");
  System.out.println("  [2] Change my team");
  System.out.println("  [3] Get team status update");
  System.out.println("  [4] Quit program");
  System.out.println();
 }

 /**
  * Helper method to prompt the user for a status update.
  */
 private void updateStatus()
 {
  /* Display all status options and prompt the user for a choice */
  System.out.println("Type a number to choose a status");
  for(int i = 0; i < statuses.length; i++)
   System.out.printf(" %4s %s\n", "[" + (i + 1) + "]", statuses[i]);
  int newStatus = scanner.nextInt() - 1;

  /* Guard case for invalid choice */
  if(newStatus < 0 || newStatus >= statuses.length)
  {
   System.out.println("Invalid selection: " + (newStatus + 1));
   return;
  }

  /* Update status and inform server of the change */
  status = newStatus;
  transmitUntilResponse(ClientMessage.STATUS_UPDATE, MessageHelper.EMPTY_BODY, ServerMessage.ACK_STATUS_UPDATE);

 }

 /**
  * Helper method to prompt the user for a team change.
  */
 private void changeTeam()
 {
  /* Display all team options and prompt the user for a choice */
  System.out.println("Type a number to choose a team");
  for(int i = 0; i < teams.length; i++)
   System.out.printf(" %4s %s\n", "[" + (i + 1) + "]", teams[i]);
  int newTeam = scanner.nextInt() - 1;

  /* Guard case for invalid choice */
  if(newTeam < 0 || newTeam >= teams.length)
  {
   System.out.println("Invalid selection: " + (newTeam + 1));
   return;
  }

  /* Update team and inform server of the change */
  team = newTeam;
  transmitUntilResponse(ClientMessage.STATUS_UPDATE, MessageHelper.EMPTY_BODY, ServerMessage.ACK_STATUS_UPDATE);
 }

 /**
  * Helper method to get a status update from the server and display it.
  */
 private void displayTeamStatusUpdate()
 {
  /* Guard case for not yet on a team */
  if(team == MessageHelper.TEAM_WILDCARD)
  {
   System.out.println("You are not on a team. To get team status updates, join a team.");
   return;
  }

  /* Get all users on the team and their statuses */
  ServerMessage sm = transmitUntilResponse(ClientMessage.TEAM_STATUS_REQUEST, MessageHelper.EMPTY_BODY,
    ServerMessage.TEAM_STATUS);
  String[] teammates = sm.getUsernames();
  int[] statusVector = sm.getStatusVector();

  /* Display team status data */
  for(int i = 0; i < teammates.length; i++)
   if(!teammates[i].isEmpty())
    System.out.printf("  %-16s - %s\n", teammates[i],
      statusVector[i] >= statuses.length ? "[none]" : statuses[statusVector[i]]);

 }

 /**
  * Helper method to close the application
  */
 private void quit()
 {
  System.out.println("Shutting down TeamConnect client...");

  /* Inform the server that the client is disconnecting */
  this.team = MessageHelper.TEAM_WILDCARD;
  this.status = MessageHelper.INACTIVE_STATUS;
  transmitUntilResponse(ClientMessage.STATUS_UPDATE, MessageHelper.EMPTY_BODY, ServerMessage.ACK_STATUS_UPDATE);

  /* Prevent resource leakage */
  scanner.close();
  socket.close();

  System.out.println("Shutdown complete. Goodbye.");
  System.exit(0);

 }

 /* TRANSMITTERS AND RECEIVERS */

 /**
  * Transmits a message unreliably. Attempt to send it once, does not attempt to
  * determine whether transmission was succesful
  * 
  * @param messageType
  * @param messageBody
  */
 private void transmit(int messageType, byte[] messageBody)
 {
  /*
   * The ClientMessage object properly joins header data and the body into a
   * readable format
   */
  ClientMessage msg = new ClientMessage(messageType, networkID, team, status, messageBody);

  /* Create and unreliably send a packet to the server */
  DatagramPacket packet = new DatagramPacket(msg.getFullMessage(), msg.getFullMessage().length, serverIP, serverPort);
  try
  {
   socket.send(packet);
  } catch (IOException e)
  {
  }
 }

 /**
  * Transmits a message and then listens for a response from the server of a
  * specified type for {@value #MESSAGE_TIMEOUT}ms. If an error message is
  * received, prints its contents.
  * 
  * @param messageType
  * @param body
  * @param expectedMessageType
  * @return The received response if exists and is the expected type, otherwise
  *         null
  */
 private ServerMessage transmitAndReceiveResponse(int messageType, byte[] body, int expectedMessageType)
 {
  transmit(messageType, body);

  DatagramPacket p = new DatagramPacket(new byte[MessageHelper.MAX_SEGMENT_SIZE], MessageHelper.MAX_SEGMENT_SIZE);
  try
  {
   socket.receive(p);
   ServerMessage msg = new ServerMessage(p);

   /* Only return if the message exists and is the expected type */
   if(msg.getMessageType() == expectedMessageType)
    return msg;

   /* Dump error message contents */
   if(msg.getMessageType() == ServerMessage.ERROR)
    System.out.println(msg.getBodyText());

  } catch (IOException e)
  {
  }

  return null;
 }

 /**
  * Repeatedly transmits a message to the server until it recieves a response of
  * the expected type. Will loop infinitely if the server is unreachable.
  * 
  * @param messageType
  * @param body
  * @param expectedMessageType
  * @return Server response
  */
 private ServerMessage transmitUntilResponse(int messageType, byte[] body, int expectedMessageType)
 {
  ServerMessage msg;

  do
   msg = transmitAndReceiveResponse(messageType, body, expectedMessageType);
  while (msg == null);

  return msg;
 }

}
