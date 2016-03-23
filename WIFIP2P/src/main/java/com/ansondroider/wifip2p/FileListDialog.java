package com.ansondroider.wifip2p;

import java.io.File;

import com.anson.acode.FileUtils;
import com.ansondroider.wifip2p.utils.WPGlobal;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FileListDialog extends Dialog implements OnItemClickListener{

	TextView tv_current_path;
	ListView lv_file;
	FileAdapter adapter;
	protected FileListDialog(Context context, boolean cancelable,
			OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
		// TODO Auto-generated constructor stub
	}
	
	protected FileListDialog(Context context, int theme) {
		super(context, theme);
		// TODO Auto-generated constructor stub
	}
	
	protected FileListDialog(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	protected FileListDialog(Context context, OnFileSelectedListener lis){
		super(context);
		fsLis = lis;
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_filelist);
		tv_current_path = (TextView)findViewById(R.id.tv_current_path);
		lv_file = (ListView)findViewById(R.id.lv_file);
		goPath(Environment.getExternalStorageDirectory());
	}
	
	File[] files;
	File currentFile;
	void goPath(File f){
		currentFile = f;
		files = FileUtils.getFileList(f.getAbsolutePath(), true);
		FileUtils.sortFile(files);
		
		if(adapter == null){
			adapter = new FileAdapter(getContext(), files);
			lv_file.setAdapter(adapter);
			lv_file.setOnItemClickListener(this);
		}else{
			adapter.updateFiles(files);
			adapter.notifyDataSetChanged();
		}
		tv_current_path.setText(currentFile.getAbsolutePath());
	}
	
	public static class FileAdapter extends BaseAdapter{
		File[] files = null;

		Context cxt;
		LayoutInflater lInflaer;
		
		public FileAdapter(Context context, File[] files){
			cxt = context;
			this.files = files;
			lInflaer = LayoutInflater.from(cxt);
		}
		
		public void updateFiles(File[] fs){
			this.files = fs;
		}
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return files == null ? 0 : files.length;
		}

		@Override
		public Object getItem(int pos) {
			// TODO Auto-generated method stub
			return files == null ? null : files[pos];
		}

		@Override
		public long getItemId(int pos) {
			// TODO Auto-generated method stub
			return pos;
		}

		@Override
		public View getView(int pos, View currentView, ViewGroup parent) {
			// TODO Auto-generated method stub
			FileItem item;
			if(currentView != null && currentView.getTag() != null){
				item = (FileItem)currentView.getTag();
			}else{
				View v = lInflaer.inflate(R.layout.list_item_peer, null);
				TextView tv = (TextView)v.findViewById(R.id.tv_name);
				ImageView iv = (ImageView)v.findViewById(R.id.iv_header);
				item = new FileItem();
				item.icon = iv;
				item.name = tv;
				v.setTag(item);
				currentView = v;
			}
			item.name.setText(FileUtils.getSimpleName(files[pos]));
			item.icon.setImageResource(files[pos].isDirectory() ? R.drawable.format_folder : R.drawable.format_unkown);
			return currentView;
		}
		
		class FileItem {
			ImageView icon;
			TextView name;
		}
		
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
		// TODO Auto-generated method stub
		if(files[pos].isDirectory()){
			goPath(files[pos]);
		}else{
			if(fsLis != null){
				fsLis.onFileSelected(files[pos].getAbsolutePath());
			}
			dismiss();
		}
	};
	
	
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if(currentFile.getAbsolutePath().equals("/")){
			super.onBackPressed();
		}else{
			File parent = currentFile.getParentFile();
			goPath(parent);
		}
		
		
	}
	
	OnFileSelectedListener fsLis;
	public static interface OnFileSelectedListener{
		public void onFileSelected(String file);
	}
	

}
