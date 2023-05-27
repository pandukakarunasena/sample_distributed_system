import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class ZooKeeperClient {

    private ZooKeeper zooKeeper;

    public ZooKeeperClient(String zooKeeperUrl, int sessionTimeout, Watcher watcher) throws IOException {

        zooKeeper = new ZooKeeper(zooKeeperUrl, sessionTimeout, watcher);
    }

    public String createNode(String path, boolean shouldWatch, CreateMode mode)
            throws UnsupportedEncodingException, InterruptedException, KeeperException {

        String createdPath = zooKeeper.create(path, "".getBytes("UTF-8"), ZooDefs.Ids.OPEN_ACL_UNSAFE, mode);
        return createdPath;
    }

    public boolean checkExists(String path) throws InterruptedException, KeeperException {

        Stat nodeStat = zooKeeper.exists(path, false);
        return nodeStat != null;
    }

    public void delete(String path) throws
            KeeperException, InterruptedException {
        zooKeeper.delete(path, -1);
    }

    public List<String> getChildrenNodePaths(String root) throws InterruptedException, KeeperException {

        zooKeeper.getState().toString();
        return zooKeeper.getChildren(root, false);
    }

    public void addWatch(String path) throws InterruptedException, KeeperException {

        zooKeeper.exists(path, true);
    }
}
