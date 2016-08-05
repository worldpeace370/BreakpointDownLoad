package com.lebron.breakpointdownload.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**包含了待下载的文件的信息,为了Intent传输，实现了Parcelable接口序列化
 * Created by wuxiangkun on 2016/8/4 13:53.
 * Contacts wuxiangkun@live.com
 */
public class FileInfo implements Parcelable{
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
    //是否已经获得了文件长度
    private boolean isGettedLength;

    public FileInfo(int id, String url, String fileName, long length, long progress, boolean isGettedLength) {
        this.id = id;
        this.url = url;
        this.fileName = fileName;
        this.length = length;
        this.progress = progress;
        this.isGettedLength = isGettedLength;
    }

    //实现Parcelable后自动生成的，也可以根据需要自己改写
    protected FileInfo(Parcel in) {
        id = in.readInt();
        url = in.readString();
        fileName = in.readString();
        length = in.readLong();
        progress = in.readLong();
        isGettedLength = in.readByte() != 0;
    }
    //通过createFromParcel将Parcel对象映射成你的对象
    //也可以将Parcel看成是一个流，通过writeToParcel把对象写到流里面，在通过createFromParcel从流里读取对象，
    //只不过这个过程需要你来实现，因此写的顺序和读的顺序必须一致。
    public static final Creator<FileInfo> CREATOR = new Creator<FileInfo>() {
        @Override
        public FileInfo createFromParcel(Parcel in) {
            return new FileInfo(in);
        }

        @Override
        public FileInfo[] newArray(int size) {
            return new FileInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
    //通过writeToParcel将你的对象映射序列化成Parcel对象
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(url);
        dest.writeString(fileName);
        dest.writeLong(length);
        dest.writeLong(progress);
        dest.writeByte((byte) (isGettedLength ? 1 : 0));
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

    public boolean isGettedLength() {
        return isGettedLength;
    }

    public void setGettedLength(boolean gettedLength) {
        isGettedLength = gettedLength;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", fileName='" + fileName + '\'' +
                ", length=" + length +
                ", progress=" + progress +
                ", isGettedLength=" + isGettedLength +
                '}';
    }

}
