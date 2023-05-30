package org.tutorial;

import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class DummyProcess {

    public static final String ZOOKEEPER_URL = "127.0.0.1:2181";
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public static void main(String[] args)  {

        DistributedLock.setZooKeeperURL(ZOOKEEPER_URL);
        if (args.length < 1) {
            System.out.println("Usage org.tutorial.DummyProcess <Lock Name to Acquire");
            System.exit(1);
        }

        String lockName = args[0];
        System.out.println("Contesting to acquire lock " + lockName);

        DistributedLock lock = null;
        try {
            lock = new DistributedLock(lockName, "");
            lock.acquireLock();
            System.out.println("I got the lock at " + getCurrentTimeStamp());
            accessSharedResources();
            lock.releaseLock();
            System.out.println("Releasing the lock " + getCurrentTimeStamp());
        } catch (InterruptedException | KeeperException | IOException e) {
            System.out.println("Error while creating Distributed Lock myLock : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void accessSharedResources() {

        Random r = new Random();
        long sleepDuration = Math.abs(r.nextInt() % 20);
        System.out.println("Accessing critical section. Time remaining : " + sleepDuration + " seconds.");

        try {
            Thread.sleep(sleepDuration * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private static String getCurrentTimeStamp() {
        return timeFormat.format(new Date(System.currentTimeMillis()));
    }
}
