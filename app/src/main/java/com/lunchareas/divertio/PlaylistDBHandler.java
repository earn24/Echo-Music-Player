package com.lunchareas.divertio;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDBHandler extends SQLiteOpenHelper {

    // database info
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "PlaylistInfoDatabase";

    // database attributes
    private static final String TABLE_PLAYLISTS = "playlists";
    private static final String KEY_NAME = "name";
    private static final String KEY_LIST = "list";

    // numbers correspond to keys
    private static final int KEY_NAME_IDX = 0;
    private static final int KEY_LIST_IDX = 1;

    // needs context for song database handler
    private Context dbContext;

    public PlaylistDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.dbContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SONG_DATABASE = "CREATE TABLE " + TABLE_PLAYLISTS + "(" + KEY_NAME + " TEXT," + KEY_LIST + " TEXT" + ")";
        db.execSQL(CREATE_SONG_DATABASE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldDb, int newDb) {

        // replace old table
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLISTS);
        this.onCreate(db);
    }

    private String playlistToString(PlaylistData playlistData) {

        // get usable data from list
        List<String> songNameList = new ArrayList<>();
        for (SongData songData: playlistData.getSongList()) {
            songNameList.add(songData.getSongName());
        }

        Gson gson = new Gson();
        String songListString = gson.toJson(songNameList);
        System.out.println("String: " + songListString);

        return songListString;
    }

    private List<SongData> stringToSongData(String songListString) {

        // get usable data from string
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        ArrayList<String> finalOutputString = gson.fromJson(songListString, type);

        // search for songs with the name
        SongDBHandler db = new SongDBHandler(dbContext);
        List<SongData> songDataList = new ArrayList<>();
        for (String songName: finalOutputString) {
            songDataList.add(db.getSongData(songName));
        }

        return songDataList;
    }

    public void addPlaylistData(PlaylistData playlistData) {

        // get table data
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // insert new data from song data
        values.put(KEY_NAME, playlistData.getPlaylistName());
        values.put(KEY_LIST, playlistToString(playlistData));

        db.insert(TABLE_PLAYLISTS, null, values);
        db.close();
    }

    public PlaylistData getPlaylistData(String name) {

        // get table data
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_PLAYLISTS, new String[] { KEY_NAME, KEY_LIST }, KEY_NAME + "=?", new String[] { String.valueOf(name) }, null, null, null, null);

        // search through database
        if (cursor != null) {
            cursor.moveToFirst();
            PlaylistData playlistData = new PlaylistData(cursor.getString(KEY_NAME_IDX), stringToSongData(cursor.getString(KEY_LIST_IDX)));
            db.close();
            return playlistData;
        } else {
            System.out.println("Failed to create database cursor.");
        }

        System.out.println("Failed to find song data with that name in database.");
        return null;
    }

    public List<PlaylistData> getPlaylistDataList() {

        // create list and get table data
        List<PlaylistData> playlistDataList = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        String dbQuery = "SELECT * FROM " + TABLE_PLAYLISTS;
        Cursor cursor = db.rawQuery(dbQuery, null);

        // go through database and all to list
        if (cursor.moveToFirst()) {
            do {
                PlaylistData playlistData = new PlaylistData(cursor.getString(KEY_NAME_IDX), stringToSongData(cursor.getString(KEY_LIST_IDX)));
                playlistDataList.add(playlistData);
            } while (cursor.moveToNext());
        }

        db.close();
        return playlistDataList;
    }
}
