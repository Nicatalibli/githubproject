package com.test.screenrecord.listener;

public interface Subject<T, K> {
    void registerObserver(T observer);
    void removeObserver(T observer);
    void notifyObservers(K notify);
}
