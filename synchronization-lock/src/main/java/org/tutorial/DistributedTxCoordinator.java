package org.tutorial;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DistributedTxCoordinator extends DistributedTx {

    public DistributedTxCoordinator(DistributedTxListener listener) {

        super(listener);
    }

    @Override
    void onStartTransaction(String transactionId, String participantId) {

        try {
            currentTransaction = "/" + transactionId;
            client.createNode(currentTransaction, true, CreateMode.PERSISTENT, "".getBytes(StandardCharsets.UTF_8));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean perform() throws InterruptedException, KeeperException {

        List<String> childrenNodePaths = client.getChildrenNodePaths(currentTransaction);
        boolean result = true;
        byte[] data;
        System.out.println("Child count : " + childrenNodePaths.size());

        for (String path : childrenNodePaths) {
            path = currentTransaction + "/" + path;
            System.out.println("Checking path : " + path);
            data = client.getData(path, false);
            String dataString = new String(data);
            if (!VOTE_COMMIT.equals(dataString)) {
                System.out.println("Child " + path + " caused the transaction to abort. Sending GLOBAL_ABORT");
                sendGlobalAbort();
                result = false;
            }
        }

        System.out.println("All nodes are okay to commit the transaction. Sending GLOBAL_COMMIT");
        sendGlobalCommit();
        reset();
        return result;
    }

    public void sendGlobalCommit() throws KeeperException, InterruptedException {
        if (currentTransaction != null) {
            System.out.println("Sending global commit for " + currentTransaction);
            client.write(currentTransaction, DistributedTxCoordinator.GLOBAL_COMMIT.getBytes(StandardCharsets.UTF_8));
            listener.onGlobalCommit();
        }
    }

    public void sendGlobalAbort() throws KeeperException,InterruptedException {
        if (currentTransaction != null) {
            System.out.println("Sending global abort for " + currentTransaction);
            client.write(currentTransaction, DistributedTxCoordinator.GLOBAL_ABORT.getBytes(StandardCharsets.UTF_8));
            listener.onGlobalAbort();
        }
    }

    private void reset() throws KeeperException, InterruptedException {
        client.forceDelete(currentTransaction);
        currentTransaction = null;
    }
}
