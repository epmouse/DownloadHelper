package com.example.liu_xingxing.downloadhelp;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.Unique;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by liu_xingxing on 2017/11/27.
 */
@Entity
public class DownloadEntity {
    @Id
    private Long id;
    @Unique
    private String url;
    private long totalLength;
    private long currentProgress;
    private String fileName;
    private boolean isComplete;

    public DownloadEntity() {
        this.isComplete = currentProgress!=0&&totalLength!=0&&currentProgress==totalLength;
    }
    @Keep
    public DownloadEntity(Long id, String url, long totalLength, long currentProgress,
            String fileName, boolean isComplete) {
        this.id = id;
        this.url = url;
        this.totalLength = totalLength;
        this.currentProgress = currentProgress;
        this.fileName = fileName;
        this.isComplete = isComplete;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(long totalLength) {
        this.totalLength = totalLength;
    }

    public long getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(long currentProgress) {
        this.currentProgress = currentProgress;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public boolean getIsComplete() {
        return this.isComplete;
    }

    public void setIsComplete(boolean isComplete) {
        this.isComplete = isComplete;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DownloadEntity)) return false;

        DownloadEntity that = (DownloadEntity) o;

        return getUrl().equals(that.getUrl());
    }

    @Override
    public int hashCode() {
        return getUrl().hashCode();
    }
}
