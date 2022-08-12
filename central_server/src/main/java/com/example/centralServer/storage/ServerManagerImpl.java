package com.example.centralServer.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServerManagerImpl implements ServerManager {

    @Value("${nodeServerUrls}")
    private String[] nodeServerUrls;
    private int availableServerIndex = 0;

    private static boolean TEST_MODE = true;

    @Autowired
    public ServerManagerImpl() {

    }


    @Override
    public String getNextAvailableServerUrl() {
        if (TEST_MODE) {
            return nodeServerUrls[0];
        }
        availableServerIndex %= nodeServerUrls.length;
        return nodeServerUrls[availableServerIndex++];
    }

    @Override
    public String[] getAllServerUrl() {
        return nodeServerUrls;
    }

    @Override
    public int getNumberOfServers() {
        return nodeServerUrls.length;
    }
}
