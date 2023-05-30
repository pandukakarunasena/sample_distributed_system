package org.tutorial;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class BankServer {

    public static final String NAME_SERVICE_ADDRESS = "http://localhost:2379";

    public static void main (String[] args) throws Exception{

        // Manually up the server in the port 11436 and the add service
        int serverPort = 11436;
        Server server = ServerBuilder
                .forPort(serverPort)
                .addService(new BalanceServiceImpl())
                .build();
        server.start();
        System.out.println("BankServer Started and ready to accept requests on port" + serverPort);
        server.awaitTermination();

        // Register the service in etcd to dynamically search by client
        NameServiceClient client = new NameServiceClient(NAME_SERVICE_ADDRESS);
        client.registerService("CheckBalanceService", "127.0.0.1", serverPort, "tcp");
    }
}
