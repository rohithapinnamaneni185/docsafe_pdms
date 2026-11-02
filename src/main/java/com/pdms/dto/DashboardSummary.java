package com.pdms.dto;

import java.util.List;
import java.util.Map;

/**
 * Aggregated statistics for the dashboard view.
 */
public class DashboardSummary {

    private long totalDocuments;
    private long validDocuments;
    private long expiringSoonDocuments;
    private long expiredDocuments;
    private long noExpiryDocuments;
    private long trashDocuments;
    private Map<String, Long> categoryCounts;
    private List<DocumentResponse> recentDocuments;
    private List<DocumentResponse> expiringSoonList;

    public DashboardSummary() {
    }

    public DashboardSummary(long totalDocuments, long validDocuments, long expiringSoonDocuments, 
                            long expiredDocuments, long noExpiryDocuments, long trashDocuments, 
                            Map<String, Long> categoryCounts, 
                            List<DocumentResponse> recentDocuments, List<DocumentResponse> expiringSoonList) {
        this.totalDocuments = totalDocuments;
        this.validDocuments = validDocuments;
        this.expiringSoonDocuments = expiringSoonDocuments;
        this.expiredDocuments = expiredDocuments;
        this.noExpiryDocuments = noExpiryDocuments;
        this.trashDocuments = trashDocuments;
        this.categoryCounts = categoryCounts;
        this.recentDocuments = recentDocuments;
        this.expiringSoonList = expiringSoonList;
    }

    public long getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(long totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public long getValidDocuments() {
        return validDocuments;
    }

    public void setValidDocuments(long validDocuments) {
        this.validDocuments = validDocuments;
    }

    public long getExpiringSoonDocuments() {
        return expiringSoonDocuments;
    }

    public void setExpiringSoonDocuments(long expiringSoonDocuments) {
        this.expiringSoonDocuments = expiringSoonDocuments;
    }

    public long getExpiredDocuments() {
        return expiredDocuments;
    }

    public void setExpiredDocuments(long expiredDocuments) {
        this.expiredDocuments = expiredDocuments;
    }

    public long getNoExpiryDocuments() {
        return noExpiryDocuments;
    }

    public void setNoExpiryDocuments(long noExpiryDocuments) {
        this.noExpiryDocuments = noExpiryDocuments;
    }

    public long getTrashDocuments() {
        return trashDocuments;
    }

    public void setTrashDocuments(long trashDocuments) {
        this.trashDocuments = trashDocuments;
    }

    public Map<String, Long> getCategoryCounts() {
        return categoryCounts;
    }

    public void setCategoryCounts(Map<String, Long> categoryCounts) {
        this.categoryCounts = categoryCounts;
    }

    public List<DocumentResponse> getRecentDocuments() {
        return recentDocuments;
    }

    public void setRecentDocuments(List<DocumentResponse> recentDocuments) {
        this.recentDocuments = recentDocuments;
    }

    public List<DocumentResponse> getExpiringSoonList() {
        return expiringSoonList;
    }

    public void setExpiringSoonList(List<DocumentResponse> expiringSoonList) {
        this.expiringSoonList = expiringSoonList;
    }
}
