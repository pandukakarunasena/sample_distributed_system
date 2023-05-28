package org.tutorial;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.bank.service.BalanceServiceGrpc;
import org.bank.service.CheckBalanceRequest;
import org.bank.service.CheckBalanceResponse;

import java.io.IOException;
import java.util.Scanner;

public class CheckBalanceServiceClient {

    public static final String NAME_SERVICE_ADDRESS = "http://localhost:2379";
    private ManagedChannel channel = null;
    BalanceServiceGrpc.BalanceServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;

    public static void main(String[] args) throws InterruptedException {
        String host = null;
        int port = -1;
        if (args.length != 2) {
            System.out.println("Usage CheckBalanceServiceClient <host> <port>");
            System.exit(1);
        }
        host = args[0];
        port = Integer.parseInt(args[1].trim());
        CheckBalanceServiceClient client = new CheckBalanceServiceClient(host, port);
        client.initializeConnection();
        client.processUserRequests();
        client.closeConnection();
    }
    public CheckBalanceServiceClient (String host, int port) {
        this.host = host;
        this.port = port;

        // Use ETCD for dynamically get the service. This removes the hard-coding of the service host and port.
        fetchServerDetails();
    }

    private void fetchServerDetails() {

        NameServiceClient client = null;
        try {
            client = new NameServiceClient(NAME_SERVICE_ADDRESS);
            NameServiceClient.ServiceDetails serviceDetails = client.findService("CheckBalanceService");
            host = serviceDetails.getIPAddress();
            port = serviceDetails.getPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeConnection () {
        System.out.println("Initializing Connecting to server at " + host + ":" + port);
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        clientStub = BalanceServiceGrpc.newBlockingStub(channel);

        // Use ETCD name server to find the service
        channel.getState(true);
    }
    private void closeConnection() {
        channel.shutdown();
    }

    private void processUserRequests() throws InterruptedException {

        while (true) {
            Scanner userInput = new Scanner(System.in);
            System.out.println("\nEnter Account ID to check the balance :");
            String accountId = userInput.nextLine().trim();
            System.out.println("Requesting server to check the account balance for " + accountId.toString());
            CheckBalanceRequest request = CheckBalanceRequest
                    .newBuilder()
                    .setAccountId(accountId)
                    .build();

            //Use ETCH name server to find the service
            ConnectivityState state = channel.getState(true);
            while (state != ConnectivityState.READY) {

                System.out.println("Service unavailable, looking for a service provider..");
                fetchServerDetails();
                initializeConnection();
                Thread.sleep(5000);
                state = channel.getState(true);
            }

            CheckBalanceResponse response = clientStub.checkBalance(request);
            System.out.printf("My balance is " + response.getBalance() + " LKR");
            Thread.sleep(1000);
        }
    }
}
