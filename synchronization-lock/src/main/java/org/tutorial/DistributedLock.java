package org.tutorial;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DistributedLock implements Watcher {

    private String childPath;
    private ZooKeeperClient client;
    private String lockPath;
    private boolean isAcquired = false;
    private String watchedNode;
    CountDownLatch startFlag = new CountDownLatch(1);
    CountDownLatch eventReceivedFlag;
    public static String zooKeeperURL;
    private static String lockProcessPath = "/lp_";

    public static void setZooKeeperURL(String url) {
        zooKeeperURL = url;
    }

    public DistributedLock(String lockName) throws InterruptedException, IOException, KeeperException {

        this.lockPath = "/" + lockName;
        client = new ZooKeeperClient(zooKeeperURL, 5000, this);
        startFlag.await();
        if (client.checkExists(lockPath) == false) {
            createRootNode();
        }
        createChildNode();
    }

    private void createRootNode() throws UnsupportedEncodingException, InterruptedException, KeeperException {

        lockPath = client.createNode(lockPath, false, CreateMode.PERSISTENT);
        System.out.println("Root node created at " + lockPath);
    }

    private void createChildNode() throws UnsupportedEncodingException, InterruptedException, KeeperException {

        childPath = client.createNode(lockPath + lockProcessPath, false, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Chile node created at " + childPath);
    }

    public void acquireLock() throws InterruptedException, KeeperException {

        String smallestNode = findSmallestNodePath();
        if (smallestNode.equals(childPath)) {
            isAcquired = true;
        } else {
            do {
                System.out.println("Lock is currently acquired by node " + smallestNode + " ..hence waiting..");
                eventReceivedFlag = new CountDownLatch(1);
                watchedNode = smallestNode;
                client.addWatch(smallestNode);
                eventReceivedFlag.await();
                smallestNode = findSmallestNodePath();
            } while (!smallestNode.equals(childPath)); {
                isAcquired = true;
            }
        }
    }

    private String findSmallestNodePath() throws InterruptedException, KeeperException {

        List<String> childrenNodePaths = null;
        childrenNodePaths = client.getChildrenNodePaths(lockPath);
        Collections.sort(childrenNodePaths);
        String smallestPath = childrenNodePaths.get(0);
        smallestPath = lockPath + "/" + smallestPath;
        return smallestPath;
    }

    public void releaseLock() throws InterruptedException, KeeperException {
        if (!isAcquired) {
            throw new IllegalStateException("Lock needs to be acquired first to release");
        }
        client.delete(childPath);
        isAcquired = false;
    }

    @Override
    public void process(WatchedEvent event) {

        Event.KeeperState state = event.getState();
        Event.EventType type = event.getType();

        if (Event.KeeperState.SyncConnected == state) {
            if (Event.EventType.None == type) {
                // Identify successful connection
                System.out.println("Successful connected to the server");
                startFlag.countDown();
            }
        }

        if (Event.EventType.NodeDeleted.equals(type)) {
            if (watchedNode != null && eventReceivedFlag != null && event.getPath().equals(watchedNode)) {
                System.out.println("NodeDelete event received. Trying to get the lock...");
                eventReceivedFlag.countDown();
            }
        }
    }

}
