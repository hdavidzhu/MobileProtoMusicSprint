package com.hdavidzhu.mobileprotomusicsprint;

import com.firebase.client.Firebase;

import java.util.HashMap;
import java.util.Map;

public class SnapFirebase {
    Firebase snapRef;

    public SnapFirebase() {
        this.snapRef = new Firebase("https://snaptunes.firebaseio.com/");
    }

    public void postSnap(Song song) {
        Map<String, String> snapMap = new HashMap<String, String>();
        snapMap.put("id", String.valueOf(song.getID()));
        snapMap.put("title", song.getTitle());
        snapMap.put("artist", song.getArtist());

        snapMap.put("uri", song.getURI());
        snapMap.put("formula", song.getFormula());

        snapRef.child("David").setValue(snapMap);
//        snapRef.push().setValue(snapMap);

    }
}