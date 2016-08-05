package com.lebron.breakpointdownload.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lebron.breakpointdownload.R;
import com.lebron.breakpointdownload.bean.FileInfo;
import com.lebron.breakpointdownload.service.DownloadService;

import java.util.List;

/**
 * Created by wuxiangkun on 2016/8/4 23:14.
 * Contacts wuxiangkun@live.com
 */
public class DownloadListAdpter extends BaseAdapterHelper<FileInfo>{
    private Context mContext;
    private List<FileInfo> mFileInfoList;
    public DownloadListAdpter(List<FileInfo> list, Context context) {
        super(list, context);
        mContext = context;
        mFileInfoList = list;
    }

    @Override
    public View getItemView(int position, View convertView, ViewGroup parent, List<FileInfo> list, LayoutInflater inflater) {
        final FileInfo info = list.get(position);
        final ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_download, null);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.proText = (TextView) convertView.findViewById(R.id.pro_text);
            holder.start = (Button) convertView.findViewById(R.id.start);
            holder.pasue = (Button) convertView.findViewById(R.id.pause);
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        int progress = (int) info.getProgress();
        holder.name.setText(info.getFileName());
        holder.progressBar.setProgress(progress);
        holder.proText.setText(new StringBuffer().append(progress).append("%"));

        holder.start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.proText.setVisibility(View.VISIBLE);
                Intent intent = new Intent(mContext, DownloadService.class);
                intent.setAction(DownloadService.ACTION_START);
                intent.putExtra("mFileInfo", info);
                mContext.startService(intent);
                holder.start.setEnabled(false);
                holder.pasue.setEnabled(true);
            }
        });
        holder.pasue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, DownloadService.class);
                intent.setAction(DownloadService.ACTION_PAUSE);
                intent.putExtra("mFileInfo", info);
                mContext.startService(intent);
                holder.start.setEnabled(true);
                holder.pasue.setEnabled(false);
            }
        });
        return convertView;
    }

    /**
     * 更新列表中 ProgressBar的进度
     * @param id 根据广播接收器得到的文件编号
     * @param progress 根据广播接收器得到的下载进度
     */
    public void updateProgressBar(int id, int progress){
        FileInfo fileInfo = mFileInfoList.get(id);
        fileInfo.setProgress(progress);
        notifyDataSetChanged();
    }

    class ViewHolder {
        TextView name;
        TextView proText;
        Button start;
        Button pasue;
        ProgressBar progressBar;
    }
}
