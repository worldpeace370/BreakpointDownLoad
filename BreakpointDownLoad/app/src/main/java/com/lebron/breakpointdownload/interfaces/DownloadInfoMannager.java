package com.lebron.breakpointdownload.interfaces;

import com.lebron.breakpointdownload.bean.DownloadInfo;

import java.util.List;

/**下载信息->DownloadInfo的管理接口
 * 把DownloadInfo的所有操作定义成接口的好处，
 * 是方便扩展，实现类既可以用数据库也可以用网络存储
 * Created by wuxiangkun on 2016/8/4 19:37.
 * Contacts wuxiangkun@live.com
 */
public interface DownloadInfoMannager {
    /**
     * 往数据库中插入某文件的下载信息，包含了下载进度等等
     * @param downloadInfo java bean
     */
    void insertDownloadInfo(DownloadInfo downloadInfo);

    /**
     * 删除数据库中的某文件的下载信息，当下载任务完成后执行此操作
     * @param url 文件的url
     * @param id  文件的编号
     */
    void deleteDownloadInfo(String url, int id);

    /**
     * 更新数据库中的文件的下载信息,完成的字节数和进度
     * @param url 文件的url
     * @param id  文件的编号
     * @param finished 已经下载完成的字节数
     * @param progress 完成的进度(0--100之间取值)
     */
    void updateDownloadInfo(String url, int id, long finished, int progress);

    /**
     * 这个主要是针对单任务多线程下载来使用的函数
     * @param url 文件的url
     * @return 返回 数据库中存储的将单任务拆分成几个线程进行下载的DownloadInfo的List集合
     */
    List<DownloadInfo> getDownloadInfoList(String url);

    /**
     * 判断当前DownloadInfo是否在数据库中存在
     * @param url      * @param url 文件的url
     * @param id       文件的编号
     * @return  存在返回true,否则false
     */
    boolean isExists(String url, int id);
}
