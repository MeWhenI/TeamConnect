## Overview

 The TeamConnect project contains definitions for a TeamConnectServer and a TeamConnectClient designed to allow team members to join teams on the server and share their status with eachother.

## Usage

 TeamConnect comes with prebuilt runners to set up and run a server or a client.

### Running a Server

 To run a TeamConnectServer from the project, first compile the runner class by navigating to the TeamConnect/src folder and executing:

```
javac teamConnect/runner/ServerRunner.java
```

 Afterwards you can run the class by executing in the same directory:

```
java teamConnect.runner.ServerRunner port teamNames statuses
```

 Where `port` is the port to run the server on, `teamNames` is a string containing up to 50 team names delimited by slashes, and `statuses` is a string contains up to 50 status names delimited by slashes. For example:

```
java teamConnect.runner.ServerRunner 2000 "The A Team/The B Team/The C Team" "Working Hard/Hardly Working/Asleep"
```

### Running a Client

 To run a TeamConnectClient from the project, first compile the runner class by navigating to the TeamConnect/src folder and executing:

```
javac teamConnect/runner/ClientRunner.java
```

 Afterwards you can run the class by executing in the same directory:

```
java teamConnect.runner.ClientRunner hostName hostPort displayName
```

 Where `hostName` is the hostName of the server the client should connect to, `hostPort` is the server's port, and `displayName` is the desired username for the client. For example:

```
java teamConnect.runner.ClientRunner "127.0.0.1" 2000 "Rusty Arbuckle"
```

## Transmission Protocol

 The transmission protocol used by TeamConnect Clients and Servers is managed by the `teamConnect/transmission` package. Both the client and server rely exclusively on UDP Datagram sockets, so there are no persistent connections. Transmissions always happen in pairs, with a client sending a ClientMessage to the server, and the server immediately handling any actions requested by the client and then responding with a ServerMessage which acknowledges the action taken and may contains information requested by the client. Both ClientMessages and ServerMessages have a message type byte in their headers that specify what type of information the contains or action they relate to. Following are the different message types:

ClientMessage types which may be sent to a server are:
 * Network ID Request: A request for a unique network ID. This message's body contains the displayName the client wishes to use.
 * Status Update: A request for the server database to update the status entry for this user.
 * Team Status Request: A request for the names of all members of a team and their statuses.
 * Server Description Request: A request for the teams and status options supported by a server.

ServerMessage types which may be sent in response to a ClientMessage are:
 * Error: Indicates something went wrong in handling the client's request.
 * ACK New User: Indicates that a new user profile was succesfully created for the client. The body of the message contains the networkID associated with the new profile.
 * Server Description: The body of the message contains a list of all the teams and all the status options the server supports.
 * Team Status: The body of the message contains the usernames and statuses of each member of a team requested by the client.
 * ACK Status Update: Indicates that a client's request to update their status of change teams was handled succesfully.

Further details about the structure of messages is provided in the relevant classes' Javadocs.
