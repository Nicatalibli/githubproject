package com.test.screenrecord.listener;


import java.util.ArrayList;
import java.util.List;

/**
 * Create by thien on 4/10/2019
 */
public class ObserverUtils<T> implements Subject<ObserverInterface<T>, T> {
    private List<ObserverInterface> observers = new ArrayList<>();
    private static ObserverUtils instance;

    public static ObserverUtils getInstance(){
        if (instance == null)
            instance = new ObserverUtils();
        return instance;
    }

    @Override
    public void registerObserver(ObserverInterface<T> observer) {
        if(!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(ObserverInterface<T> observer) {
        if (observers.contains(observer)){
            observers.remove(observer);
        }
    }

    @Override
    public void notifyObservers(T notify) {
        for (ObserverInterface observer :
            observers) {
            observer.notifyAction(notify);
        }
    }
}

