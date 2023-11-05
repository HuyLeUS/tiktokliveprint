package com.dantsu.thermalprinter.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel  extends ViewModel {

    private final MutableLiveData<Boolean> _isOnBottom = new MutableLiveData<>(true);

    public LiveData<Boolean> getIsOnBottom() {
        return _isOnBottom;
    }

    public  void setIsOnBottom(Boolean value){
        _isOnBottom.setValue(value);
    }

    public void onScrollDown(){
        _isOnBottom.postValue(true);
    }
}
