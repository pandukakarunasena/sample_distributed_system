package org.tutorial;

public interface DistributedTxListener {

    void onGlobalCommit();
    void onGlobalAbort();
}
