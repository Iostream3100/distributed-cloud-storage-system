package com.example.centralServer.storage;

public interface ServerManager {
    String getNextAvailableServerUrl();

    String[] getAllServerUrl();

    int getNumberOfServers();
}
