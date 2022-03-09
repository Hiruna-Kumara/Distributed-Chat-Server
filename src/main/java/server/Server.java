package server;

public class Server {
    private int id;
    private int coordinationPort;
    private int clientsPort;
    private String serverAddress;

    public Server(int serverID, int coordinationPort, int clientsPort, String serverAddress) {
        this.id = serverID;
        this.coordinationPort = coordinationPort;
        this.clientsPort = clientsPort;
        this.serverAddress = serverAddress;
    }

    public int getServerID() {
        return id;
    }

    public int getCoordinationPort() {
        return coordinationPort;
    }

    public int getClientsPort() {
        return clientsPort;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerID(int serverID) {
        this.id = serverID;
    }

    public void setCoordinationPort(int coordinationPort) {
        this.coordinationPort = coordinationPort;
    }

    public void setClientsPort(int clientsPort) {
        this.clientsPort = clientsPort;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }
}
