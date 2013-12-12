package com.tightdb;

import java.util.ArrayList;
import java.util.List;

class Context {
    
    // Each group of related TightDB objects will have a Context object in the root.
    // The root can be a table, a group, or a shared group.
    // The Context object is used to store a list of native pointers 
    // whose disposal need to be handed over from the garbage 
    // collection thread to the users thread.

    private List<Long> abandonedTables = new ArrayList<Long>();
    private List<Long> abandonedTableViews = new ArrayList<Long>();
    private List<Long> abandonedQueries = new ArrayList<Long>();
    
    private boolean isFinalized = false;

    public void executeDelayedDisposal() {
        synchronized (this) {
            for (long nativePointer: abandonedTables) {
                    Table.nativeClose(nativePointer);
            }
            abandonedTables.clear();

            for (long nativePointer: abandonedTableViews) {
                TableView.nativeClose(nativePointer);
            }
            abandonedTableViews.clear();
            
            for (long nativePointer: abandonedQueries) {
                TableQuery.nativeClose(nativePointer);
            }
            abandonedQueries.clear();
        }
    }

    public void asyncDisposeTable(long nativePointer, boolean isRoot) {
        synchronized (this) {
            if (isRoot || isFinalized) {
                Table.nativeClose(nativePointer);
            }
            else {
                abandonedTables.add(nativePointer);
            }
        }
    }

    public void asyncDisposeTableView(long nativePointer) {
        synchronized (this) {
            if (isFinalized) {
                TableView.nativeClose(nativePointer);
            }
            else {
                abandonedTableViews.add(nativePointer);
            }
        }
    }

    public void asyncDisposeQuery(long nativePointer) {
        synchronized (this) {
            if (isFinalized) {
                TableQuery.nativeClose(nativePointer);
            }
            else {
                abandonedQueries.add(nativePointer);
            }
        }
    }

    public void asyncDisposeGroup(long nativePointer) {
        synchronized (this) {
            Group.nativeClose(nativePointer);
        }
    }
    
    public void asyncDisposeSharedGroup(long nativePointer) {
        synchronized (this) {
            SharedGroup.nativeClose(nativePointer);
        }
    }

    protected void finalize() {
        synchronized (this) {
            isFinalized = true;
        }
        executeDelayedDisposal();
    }
}
