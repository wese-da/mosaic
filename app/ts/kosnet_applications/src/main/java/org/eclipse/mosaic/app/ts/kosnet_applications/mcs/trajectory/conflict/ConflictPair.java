package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict;

public class ConflictPair {
    ConflictInstance conflict1;
    ConflictInstance conflict2;
    
    public ConflictPair(ConflictInstance c1, ConflictInstance c2) {
        this.conflict1 = c1;
        this.conflict2 = c2;
    }
}