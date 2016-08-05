package com.lebron.breakpointdownload.bean;

/**记录下载进度信息的java bean,进行数据库保存
 * Created by wuxiangkun on 2016/8/4 19:17.
 * Contacts wuxiangkun@live.com
 */
public class DownloadInfo {
    //对应FileInfo的id
    private int id;
    //对应FileInfo的url
    private String url;
    //下载的起始位置,用于单任务多线程下载
    private long start;
    //下载的结束位置,一般是文件的大小
    private long end;
    //已经完成的大小
    private long finished;
    //专门用来记录进度条进度的属性，范围值在(0--100)之间
    private int progress;

    public DownloadInfo() {
    }

    public DownloadInfo(int id, String url, long start, long end, long finished, int progress) {
        this.id = id;
        this.url = url;
        this.start = start;
        this.end = end;
        this.finished = finished;
        this.progress = progress;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getFinished() {
        return finished;
    }

    public void setFinished(long finished) {
        this.finished = finished;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    @Override
    public String toString() {
        return "DownloadInfo{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", finished=" + finished +
                ", progress=" + progress +
                '}';
    }
}
