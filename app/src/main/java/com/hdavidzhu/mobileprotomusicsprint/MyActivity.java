package com.hdavidzhu.mobileprotomusicsprint;


import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.MediaController;
import android.app.AlertDialog;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.hdavidzhu.mobileprotomusicsprint.MusicService.MusicBinder;
import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.authentication.SpotifyAuthentication;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.spotify.sdk.android.playback.PlayerState;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


public class MyActivity extends Activity implements MediaController.MediaPlayerControl, PlayerNotificationCallback, ConnectionStateCallback {
    /**
     * SPOTIFY *
     */
    private static final String CLIENT_ID = "2315f1ec631942d88177dcd8c0422e84"; // TODO: Replace with your client ID
    private static final String REDIRECT_URI = "snaptunes://callback";          // TODO: Replace with your redirect URI

    /**
     * FIREBASE *
     */
    SnapFirebase snapFirebase;
    private Player mPlayer;

    /**
     * LISTVIEW OF SONGS *
     */
    private ArrayList<Song> songList;
    private ListView songView;

    /** ALERT DIALOG **/
    private AlertDialog.Builder builder;

    /**
     * PLAYING MUSIC *
     */
    private MusicController controller;
    private boolean paused = false;
    private boolean playbackPaused = false;
    private boolean musicBound = false;
    private Song receivedSong;

    /** CONTEXT **/
    private Context context;
    private Context activity = this;

    /** USERNAME **/
    private String storedusername;

    /**
     * HANDLING SERVICE *
     */
    private Intent playIntent;
    private MusicService musicSrv;
    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder) service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    private static class ReceivedAlertDialog extends AlertDialog {
        protected ReceivedAlertDialog(Context context) {
            super(context);

            setTitle("Profile");

            Button connect = new Button(getContext());
            setView(connect);
            connect.setText("Don't push me");
            connect.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    // I want the dialog to close at this point
                    dismiss();
                }

//            ReceivedAlertDialog.setButton2

//                public void onClick(DialogInterface dialog, int which) {
//                        // The 'which' argument contains the index position
//                        // of the selected item
//                        switch (which) {
//                            case 0:
//                                Toast.makeText(builder.getContext(), "clicked 1", Toast.LENGTH_SHORT).show();
//                                dismiss();
//                                break;
//                            case 1:
//                                Toast.makeText(builder.getContext(), "clicked 2", Toast.LENGTH_SHORT).show();
//                                break;
//                            case 2:
//                                Toast.makeText(builder.getContext(), "clicked 3", Toast.LENGTH_SHORT).show();
//                                break;
//                            case 3:
//                                Toast.makeText(builder.getContext(), "clicked 4", Toast.LENGTH_SHORT).show();
//                                break;
//                        }
//                }
            });
        }
    }

    //thing that allows us to play songs
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my); //layout xml file

        //TODO - Fix Spotify Authentication
//        SpotifyAuthentication.openAuthWindow(CLIENT_ID, "token", REDIRECT_URI,
//                new String[]{"user-read-private", "streaming"}, null, this); //spotify authentication

        // Connecting to Firebase
        Firebase.setAndroidContext(this);
        snapFirebase = new SnapFirebase();

        // Setting the Playback Controller
        setController();

        // Setting context
        this.context = getApplicationContext();

        //Setting up the ListView and getting the songs
        songView = (ListView) findViewById(R.id.song_list);
        getSongList();

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        AlertDialog.Builder usernameDialogBuilder = new AlertDialog.Builder(activity);
        usernameDialogBuilder.setTitle("Set username here:");

        final EditText username = new EditText(activity);
        usernameDialogBuilder.setView(username);

        usernameDialogBuilder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                storedusername = username.getText().toString();
                snapFirebase.postUser(storedusername);

                Query postsQuery;
                postsQuery = snapFirebase.snapRef.child(storedusername).limit(10);
                postsQuery.addChildEventListener(new ChildEventListener() {
                    // TODO - WHEN THE QUERY CHECKS FOR UPDATES, I ONLY WANT TO NOTIFY MYSELF WHEN THE SNAPTUNES IS SENT TO ME.
                    // TODO - RIGHT NOW, I AM QUERYING FOR ALL UPDATES, SO OTHER PEOPLE'S SNAPTUNES ALSO COME TO ME.

                    @Override
                    public void onChildAdded(DataSnapshot snapshot, String previousChild) {
                    }

                    @Override
                    public void onChildChanged(DataSnapshot snapshot, String previousChild) {
                        HashMap<String,String> postInfo = (HashMap) snapshot.getValue();

                        receivedSong = new Song(Long.parseLong(postInfo.get("id"),10),postInfo.get("title"),
                                postInfo.get("artist"),postInfo.get("uri"),postInfo.get("formula"));

//                Song receivedSong = new Song(Long.parseLong(postInfo.get("id"), 10),postInfo.get("title"),
//                        postInfo.get("artist"),postInfo.get("uri"),postInfo.get("formula"));

                        Log.d("Information received", receivedSong.getTitle());
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
                        alertDialogBuilder.setTitle("Song Received!");
                        alertDialogBuilder.setMessage(receivedSong.getTitle());

                        final EditText input = new EditText(activity);
                        alertDialogBuilder.setView(input);

                        alertDialogBuilder.setPositiveButton("Play", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                musicSrv.playSong(receivedSong);
                            }
                        });

                        alertDialogBuilder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // here you can add functions
                            }
                        });

                        // alertDialog.setIcon(R.drawable.icon);
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();

//                songAdt.addSong(new ChatModel(postInfo.get("name"),postInfo.get("message"),postInfo.get("timestamp")));
//                chatAdapter.notifyDataSetChanged();
                    }

                    public void onChildRemoved(DataSnapshot snapshot){
                    }
                    public void onChildMoved(DataSnapshot dataSnapshot, java.lang.String s){
                    }
                    public void onCancelled(FirebaseError firebaseError){
                    }
                });
            }
        });

        // alertDialog.setIcon(R.drawable.icon);
        AlertDialog alertDialog = usernameDialogBuilder.create();
        alertDialog.show();





    }

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }

//        AlertDialog receivedAlertDialog = new ReceivedAlertDialog(this);
//        receivedAlertDialog.show();

//        builder = new AlertDialog.Builder(this.context);
//        builder.setTitle("Title");
//        builder.setItems(new CharSequence[]
//                        {"button 1", "button 2", "button 3", "button 4"},
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        // The 'which' argument contains the index position
//                        // of the selected item
//                        switch (which) {
//                            case 0:
//                                Toast.makeText(builder.getContext(), "clicked 1", Toast.LENGTH_SHORT).show();
//                                dismiss();
//                                break;
//                            case 1:
//                                Toast.makeText(builder.getContext(), "clicked 2", Toast.LENGTH_SHORT).show();
//                                break;
//                            case 2:
//                                Toast.makeText(builder.getContext(), "clicked 3", Toast.LENGTH_SHORT).show();
//                                break;
//                            case 3:
//                                Toast.makeText(builder.getContext(), "clicked 4", Toast.LENGTH_SHORT).show();
//                                break;
//                        }
//                    }
//                });
//        builder.create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            setController();
            paused = false;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if (uri != null) {
            AuthenticationResponse response = SpotifyAuthentication.parseOauthResponse(uri);
            Spotify spotify = new Spotify(response.getAccessToken());
            //TODO - TEST WITH AUTHENTICATED  PREMIUM ACCOUNT
//            mPlayer = spotify.getPlayer(this, "My Company Name", this, new Player.InitializationObserver() {
//                @Override
//                public void onInitialized() {
//                    mPlayer.addConnectionStateCallback(MyActivity.this);
//                    mPlayer.addPlayerNotificationCallback(MyActivity.this);
//                    mPlayer.play("spotify:track:2TpxZ7JUBn3uw46aR7qd6V");
//                }
//
//                @Override
//                public void onError(Throwable throwable) {
//                    Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
//                }
//            });
        }
    }

    /**
     * ACTIONBAR FUNCTIONALITY *
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                musicSrv.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv = null;
                System.exit(0);
                break;
        }

        return true;
    }

    public void getSongList() {
        songList = new ArrayList<Song>();

        // Retrieves song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            // Get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            // Add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist, "This is a test URI", ""));
            }
            while (musicCursor.moveToNext());
        }
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
    }

    public void songPicked(View view) {
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        Log.d("Playing song", view.getTag().toString());

        musicSrv.playSong();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    String song_formula;
    String[] effects;
    public void shareSong(View view) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Pick your user and  song effects");
//        alertDialogBuilder.setMessage("Are you sure?");
        final EditText sendUser = new EditText(activity);
        alertDialogBuilder.setView(sendUser);

        effects = getResources().getStringArray(R.array.effects);
        alertDialogBuilder.setMultiChoiceItems(effects, null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which,
                                        boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            song_formula = effects[0];
                        } else if (song_formula == effects[0]) {
                            // Else, if the item is already in the array, remove it
                            song_formula = "";
                        }
                    }
        });
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Song currentSong = musicSrv.getCurrentSong();
                String recipientUsername = sendUser.getText().toString();

                //post song to Firebase
                snapFirebase.postSnap(currentSong, song_formula, recipientUsername);
                Log.d("posted song", "IDIDIT");
            }
            });

            alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                                       // here you can add functions
            }
            });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
    }


    private void setController() {
        //set the controller up
        controller = new MusicController(this);

        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    //play next
    private void playNext() {
        musicSrv.playNext();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    //play previous
    private void playPrev() {
        musicSrv.playPrev();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    /**
     * ACTIVITY CALLBACKS
     */

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }


    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv = null;
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override

    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onNewCredentials(String s) {
        Log.d("MainActivity", "User credentials blob received");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(PlayerNotificationCallback.EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
        switch (eventType) {
            // TODO: Handle event type as necessary
            default:
                break;
        }
    }

    public void getSong() {
        String uri = "spotify:track:0wrWRmDKwfPrWzWZwBYsTM";
//        Song currentSong = new Song(thisId, thisTitle, thisArtist, "", "");
    }

    @Override
    public void start() {
        musicSrv.go();
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicSrv.pausePlayer();
    }

    @Override
    public int getDuration() {
        if (musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);

    }

    @Override
    public boolean isPlaying() {
        if (musicSrv != null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}
