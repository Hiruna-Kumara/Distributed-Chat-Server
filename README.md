# Distributed-Chat-Server

This project is about a distributed chat system. The system consists of two main components, chat servers and chat clients which can run on different hosts. This repository contains the implementation of the chat servers. 

Chat clients can connect to any available chat servers. After connecting, chat clients can  send requests to create, delete, join and quit a chat room. Also, clients can get the list of available chat rooms in the system and the list of client identities currently connected to a given chat room. Finally, they can be used to send chat messages to other chat clients connected to the same chat room

## Executable Files

The executable files for client and sever can be found as jar files in the ```src\main\resource\``` folder.

A client can be executed as
```
java -jar client.jar -h server_address [-p server_port] -i identity [-d]
```
> Example
```
java -jar client.jar -h 127.0.0.1 -p 4444 -i Jhon
```
A server can be executed as
```
java -jar server.jar [server_name] "[path to server configuration file]"
```
> Example
```
java -jar server.jar s1 C:\code\Distributed-Chat-Server\src\main\config\serverConfig.txt
```

## Building the executable server file

Run the below commands
```
mvn clean install
mvn clean compile assembly:single
```
The executable jar file for server will be created inside the ```target``` folder. 
