package teamConnect.runner;

import java.net.*;

import teamConnect.client.TeamConnectClient;

/**
 * Test class for running an instance of TeamConnectClient
 */
public class ClientRunner
{
 public static void main(String[] args) throws SocketException, UnknownHostException
 {
  if(args.length != 3)
  {
   System.out.println(
     "Required arguments: hostName hostPort displayName\nExample: java teamConnect.runner.ServerRunner \"127.0.0.1\" 2000 \"JohnnyCash\"");
   return;
  }
  InetAddress hostIP = InetAddress.getByName(args[0]);
  int hostPort = Integer.parseInt(args[1]);
  String displayName = args[2];

  TeamConnectClient client = new TeamConnectClient(hostIP, hostPort, displayName);

  client.run();
 }
}
