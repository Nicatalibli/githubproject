package com.test.screenrecord.common;


import androidx.collection.LruCache;

import com.test.screenrecord.model.Photo;
import com.test.screenrecord.model.Video;

import java.util.ArrayList;

public class Cache {

    private static Cache instance;
    private LruCache<Object, Object> lru;
    private ArrayList<Photo> arrPhotos;
    private ArrayList<Video> arrVideos;
    private ArrayList<Video> arrVideosEdited;

    private Cache() {
        lru = new LruCache<>(10 * 1024 * 1024);
        arrPhotos = new ArrayList<>();
        arrVideos = new ArrayList<>();
        arrVideosEdited = new ArrayList<>();
    }

    public static Cache getInstance() {
        if (instance == null) {
            instance = new Cache();
        }
        return instance;
    }

    public LruCache<Object, Object> getLru() {
        return lru;
    }

    public ArrayList<Photo> getArrPhotos() {
        return arrPhotos;
    }

    public void setArrPhotos(ArrayList<Photo> arr) {
        arrPhotos.clear();
        arrPhotos.addAll(arr);
    }

    public ArrayList<Video> getArrVideos() {
        return arrVideos;
    }

    public ArrayList<Video> getArrVideosEdited() {
        return arrVideosEdited;
    }

    public void setArrVideos(ArrayList<Video> arrVideos) {
        this.arrVideos.clear();
        this.arrVideos.addAll(arrVideos);
    }

    public void setArrVideosEdited(ArrayList<Video> arrVideosEdited) {
        this.arrVideosEdited.clear();
        this.arrVideosEdited.addAll(arrVideosEdited);
    }
}
