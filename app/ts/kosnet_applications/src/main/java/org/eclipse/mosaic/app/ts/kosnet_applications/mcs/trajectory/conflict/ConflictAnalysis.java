package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict;

import java.util.ArrayList;
import java.util.List;

public class ConflictAnalysis {
    private List<ConflictInstance> criticalConflicts = new ArrayList<>();
    private List<ConflictInstance> warningConflicts = new ArrayList<>();
    private List<ConflictInstance> noticeConflicts = new ArrayList<>();
    
    private ConflictInstance mostCriticalConflict;
    private List<ConflictPair> overlappingConflicts = new ArrayList<>();
    private List<List<ConflictInstance>> clusteredConflicts = new ArrayList<>();
    
    private Urgency urgency = Urgency.NONE;
    
    public int getTotalConflictCount() {
        return criticalConflicts.size() + warningConflicts.size() + 
               noticeConflicts.size();
    }
    
    public ConflictInstance getMostCriticalConflict() {
    	return mostCriticalConflict;
    }
    
    public void setMostCriticalConflict(ConflictInstance c) {
    	this.mostCriticalConflict = c;
    }
    
    public List<ConflictPair> getOverlappingConflicts() {
    	return overlappingConflicts;
    }
    
    public void addOverlappingConflict(final ConflictPair p) {
    	this.overlappingConflicts.add(p);
    }
    
    public List<List<ConflictInstance>> getClusteredConflicts() {
    	return clusteredConflicts;
    }
    
    public void addClusteredConflict(final List<ConflictInstance> c) {
    	this.clusteredConflicts.add(c);
    }
    
    public List<ConflictInstance> getAllConflicts() {
        List<ConflictInstance> all = new ArrayList<>();
        all.addAll(criticalConflicts);
        all.addAll(warningConflicts);
        all.addAll(noticeConflicts);
        return all;
    }
    
    public void addCriticalConflict(ConflictInstance c) {
    	this.criticalConflicts.add(c);
    }
    
    public List<ConflictInstance> getCriticalConflicts() {
    	return new ArrayList<ConflictInstance>(criticalConflicts);
    }
    
    public void addWarningConflict(ConflictInstance c) {
    	this.warningConflicts.add(c);
    }
    
    public List<ConflictInstance> getWarningConflicts() {
    	return new ArrayList<ConflictInstance>(warningConflicts);
    }
    
    public void addNoticeConflict(ConflictInstance c) {
    	this.noticeConflicts.add(c);
    }
    
    public List<ConflictInstance> getNoticeConflicts() {
    	return new ArrayList<ConflictInstance>(noticeConflicts);
    }
    
    public Urgency getUrgency() {
    	return urgency;
    }
    
    public void setUrgency(Urgency u) {
    	this.urgency = u;
    }
}