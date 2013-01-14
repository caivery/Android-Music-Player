package com.howfun.music;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.TabActivity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.commonsware.cwac.tlv.TouchListView;

import com.howfun.music.MusicService.State;
import com.howfun.music.control.IMusicService;

public class MusicPlayerTabWidget extends TabActivity {
    protected static final String TAG = "MusicPlayerTabWidget";
	MusicListAdapter playListAdapter;
    PlayList playList = PlayList.instance;
    
    // TODO what to do if mService is null when we try to use it?
    IMusicService mService = null;

	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    playList.frontEnd = this;
	    
	    // ---- Set up playing controls ----
	    
	    View.OnClickListener playPauseListener = new View.OnClickListener() {
	    	public void onClick(View v) {
	    		if (mService != null) {
	    			try {
						mService.processPlayPauseRequest();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
	    		}
	    	}
	    };
	    
	    findViewById(R.id.playingIcon).setOnClickListener(playPauseListener);
	    findViewById(R.id.playingInfo).setOnClickListener(playPauseListener);
	    findViewById(R.id.playPauseIcon).setOnClickListener(playPauseListener);
	    
	    SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
	    	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	    		if (fromUser && mService != null) {
	    			try {
						mService.setPosition(progress);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
	    		}
	    	}
	    	
	    	public void onStartTrackingTouch (SeekBar seekBar) {}
	    	public void onStopTrackingTouch (SeekBar seekBar) {}
	    };
	    
	    ((SeekBar)findViewById(R.id.playingSeekBar)).setOnSeekBarChangeListener(seekListener);
	    
	    // ---- Set up playlist ----
	    
	    playListAdapter = new MusicListAdapter(this, R.layout.playlist_item, R.id.playlist_item_text, playList.files, R.id.playlist_icon, R.id.playlist_item_text);
	    
	    TouchListView playListView = (TouchListView) findViewById(R.id.playListList);
	    playListView.setAdapter(playListAdapter);
	    
	    playListView.setDropListener(new TouchListView.DropListener() {
	    	public void drop(int from, int to) {
	    		AudioFile file = playList.files.remove(from);
	    		playList.files.add(to, file);
	    		playListAdapter.notifyDataSetChanged();
	    	}
	    });
	    
	    playListView.setRemoveListener(new TouchListView.RemoveListener() {
	    	public void remove(int which) {
	    		playList.files.remove(which);
	    		playListAdapter.notifyDataSetChanged();
	    	}
	    });
	    
	    playListView.setOnItemLongClickListener(new OnItemLongClickListener() {
	    	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
	    		AudioFile file = playList.files.remove(position);
	    		playList.files.add(0, file);
				playListAdapter.notifyDataSetChanged();
				
				Toast.makeText(getApplicationContext(),
						"Playing " + file.getTitle(),
						Toast.LENGTH_SHORT).show();
				
				if (mService != null) {
	    			try {
						mService.processPlayNowRequest();
					} catch (RemoteException e) {
						
						e.printStackTrace();
					}
	    		}
				return true;
			}
    	});
	    
	    // ---- Set up song list ----
	    
	    final List<AudioFile> fileList = scanFiles();
	    final MusicListAdapter fileListAdapter = new MusicListAdapter(this, R.layout.list_item, R.id.list_item_text, fileList, R.id.icon, R.id.list_item_text);
        
	    ListView fileListView = (ListView) findViewById(R.id.fileListView);
	    fileListView.setTextFilterEnabled(true);
	    fileListView.setAdapter(fileListAdapter);
	    
	    fileListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				AudioFile file = fileListAdapter.getItem(position);
				
				// When clicked, show a toast with the TextView text
				Toast.makeText(getApplicationContext(),
						"Queued " + file.getTitle(),
						Toast.LENGTH_SHORT).show();
				
				playList.files.add(file);
				playListAdapter.notifyDataSetChanged();
			}
		});
	    
	    fileListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				AudioFile file = fileListAdapter.getItem(position);
				
				// When clicked, show a toast with the TextView text
				Toast.makeText(getApplicationContext(),
						"Playing " + file.getTitle(),
						Toast.LENGTH_SHORT).show();
				
				playList.files.add(0, file);
				playListAdapter.notifyDataSetChanged();
				if (mService != null) {
	    			try {
						mService.processPlayNowRequest();
					} catch (RemoteException e) {
						
						e.printStackTrace();
					}
	    		}
				return true;
			}
		});
	    
	    // ---- Set up tabs ----

	    Resources res = getResources(); 	// Resource object to get Drawables
	    TabHost tabHost = getTabHost();  	// The activity TabHost
	    
	    tabHost.addTab(tabHost.newTabSpec("playlist")
	    				      .setIndicator("Playlist", res.getDrawable(R.drawable.ic_tab_artists))
	    				      .setContent(R.id.playListView));
	    tabHost.addTab(tabHost.newTabSpec("songs")
	    					  .setIndicator("Songs", res.getDrawable(R.drawable.ic_tab_artists))	// ic_tab_songs
	    					  .setContent(R.id.fileListView));
	    tabHost.setCurrentTab(0);
	}
	
	
	@Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, MusicService.class);

        this.startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        
        
        // Start the updater
        updateHandler.postDelayed(new Updater(), 100);
    }
	
	@Override
	protected void onStop() {
		super.onStop();
		
		if (mConnection != null)
			this.unbindService(mConnection);
	}
	
	/** Get a Servie from AIDL Binder */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
        	//Get Binder
        	mService = IMusicService.Stub.asInterface(service);
        	updatePlayingInfo();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        	Utils.log(TAG, "onService disconnected.");
        	mService = null;
        }
    };
	
	
	private List<AudioFile> scanFiles() {
		List<AudioFile> audioFiles = new ArrayList<AudioFile>();
		
		ContentResolver resolver = getBaseContext().getContentResolver();
		
		String[] columns = new String [] {
				MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.ALBUM,
				MediaStore.Audio.Media.ALBUM_ID,
				MediaStore.Audio.Media.TRACK,
				MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.DURATION,
		};
		
		String where = "IS_MUSIC";
		String[] whereArgs = null;
		
		String orderBy = "Artist, Album, Track, Title";
 		
		Cursor cursor = resolver.query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				columns,
				where,
				whereArgs,
				orderBy);
		
		// TODO handle null => no external storage
		if (cursor != null) {
			while (cursor.moveToNext()) {
				audioFiles.add(
						new AudioFile(
								cursor.getLong(0),
								cursor.getString(1),
								cursor.getString(2),
								cursor.getLong(3),
								cursor.getInt(4),
								cursor.getString(5),
								cursor.getLong(6)
						)
				);
	        }
		}
		
		return audioFiles;
	}

	/**
	 * Update playing info by querying the music service.
	 */
	protected void updatePlayingInfo() {
		if (mService != null) {
			try {
				String display = mService.getCurDisplayStr();
				Uri albumUri = mService.getAlbumUri();
				updateMusicInfo(display, albumUri);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mymenu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.exit:
			exitMusicPlayer();
	
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void exitMusicPlayer() {

		Intent intent = new Intent(this, MusicService.class);
        this.stopService(intent);
        
        this.finish();
	}

	// ----------------------------------------------------------------
	
	private Handler updateHandler = new Handler();
	int mMusicState = State.Paused.ordinal();
	
    private class Updater implements Runnable {
    	public void run() {
    	    TextView playTime = (TextView) findViewById(R.id.playingTime);
    	    SeekBar bar = (SeekBar) findViewById(R.id.playingSeekBar);
    		ImageView play = (ImageView) findViewById(R.id.playPauseIcon);
    	    
    		if (playList.getCurrent() != null) {
    			int position;
				try {
					position = mService.getPosition();
				} catch (RemoteException e) {
					e.printStackTrace();
					return;
				}
    			int duration = (int) playList.getCurrent().getDuration();
    		
	    	    playTime.setText(AudioFile.formatDuration(position) + " / " + AudioFile.formatDuration(duration));
	    	    bar.setMax(duration);
	    	    bar.setProgress(position);
	    	    
	    	    try {
					mMusicState = mService.getState();
				} catch (RemoteException e) {
					e.printStackTrace();
					return;
				}
	    	    if (mMusicState == State.Stopped.ordinal()
	    	    		|| mMusicState == State.Paused.ordinal()) {
	    	    	play.setImageResource(R.drawable.play);
	    	    } else if (mMusicState == State.Preparing.ordinal() ||
	    	    		mMusicState == State.Playing.ordinal()) {
	    	    	play.setImageResource(R.drawable.pause);
	    	    }
//	    	    switch (mService.mState) {
//	    	    	case mState.Stopped:
//	    	    	case Paused:
//	    	    		play.setImageResource(R.drawable.play);
//	    	    		break;
//	    	    	case Preparing:
//	    	    	case Playing:
//	    	    		play.setImageResource(R.drawable.pause);
//	    	    		break;
//	    	    }
    		}
    		else {
    			playTime.setText("");
    			bar.setMax(0);
    			bar.setProgress(0);
    			play.setImageResource(R.drawable.play);
    		}    		
    		
    		updateHandler.postDelayed(this, 100);
    	}
    }
    
    public void changeFile(AudioFile file) {
    	if (file != null) {
    		updateMusicInfo(file.toString(), file.getImageUri());
    	}
    }
    
    private void updateMusicInfo(String fileInfoStr, Uri albumUri) {
    	
    	ImageView art = (ImageView) findViewById(R.id.playingIcon);
		TextView info = (TextView) findViewById(R.id.playingInfo);
		ImageView play = (ImageView) findViewById(R.id.playPauseIcon);
		
		if (fileInfoStr != null) {
			try {
	    		InputStream in = getContentResolver().openInputStream(albumUri);
	    		Bitmap bitmap = BitmapFactory.decodeStream(in);
	    		art.setImageBitmap(bitmap);
	    	}
	    	catch (FileNotFoundException e) {
	    		art.setImageResource(R.drawable.ic_tab_artists_white);
	    	}
			
			info.setText(fileInfoStr);
			play.setImageResource(R.drawable.play);
		}
		else {
			art.setImageBitmap(null);
			info.setText("");
			play.setImageBitmap(null);
		}
    	
		playListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	changeFile(PlayList.instance.getCurrent());
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Utils.log(TAG, "OnDestroy(), unbind Music service.");

    }
}
