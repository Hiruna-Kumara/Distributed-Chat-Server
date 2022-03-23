package Server;

public class ServerInfo {
    private String serverID;
    private String address;
    private Integer serverPort;
    private Integer clientPort;

    public ServerInfo(String serverID, String address, Integer serverPort, Integer clientPort) {
        this.serverID = serverID;
        this.address = address;
        this.serverPort = serverPort;
        this.clientPort = clientPort;
    }

    public String getServerID() {
        return serverID;
    }

    public void setServerID(String serverID) {
        this.serverID = serverID;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public Integer getClientPort() {
        return clientPort;
    }

    public void setClientPort(Integer clientPort) {
        this.clientPort = clientPort;
    }

    public Integer getServerIdInt() {
        Integer i=Integer.parseInt(serverID);
        return i;
    }
}
