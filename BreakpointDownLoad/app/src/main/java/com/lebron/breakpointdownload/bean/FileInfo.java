package com.lebron.breakpointdownload.bean;

/**包含了待下载的文件的信息
 * Created by wuxiangkun on 2016/8/4 13:53.
 * Contacts wuxiangkun@live.com
 */
public class FileInfo {
    //文件下载时任务队列的id
    private  int id;
    //根据文件url唯一标识文件信息
    private String url;
    //文件名
    private String fileName;
    //文件的字节长度
    private long length;
    //文件下载完成进度，范围从0-100.用于设置ProgressBar,在单线程断点下载中没有用到该属性。多线程中用到了
    private long progress;

    public FileInfo(int id, String url, String fileName, long length, long progress) {
        this.id = id;
        this.url = url;
        this.fileName = fileName;
        this.length = length;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }
}
